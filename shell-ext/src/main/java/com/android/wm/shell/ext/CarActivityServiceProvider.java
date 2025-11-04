package com.android.wm.shell.ext;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;

public class CarActivityServiceProvider {
    private static final String TAG = "CarActivityServiceProvider";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            STATE_DISCONNECTED,
            STATE_CONNECTING,
            STATE_CONNECTED,
    })
    @Target({ElementType.TYPE_USE})
    public @interface StateTypeEnum {}

    private final Context mContext;

    /** Handler for generic event dispatching. */
    private final Handler mEventHandler;
    private final Handler mMainThreadEventHandler;

    private final HashSet<ServiceConnectedListener> mStatusCallbacks = new HashSet<>();

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private boolean mServiceBound = false;
    @GuardedBy("mLock")
    @StateTypeEnum
    private int mConnectionState = STATE_DISCONNECTED;
    @GuardedBy("mLock")
    private int mConnectionRetryCount = 0;

    // The car service binder object we got.
    @GuardedBy("mLock")
    private ICarActivityService mCarServiceBinder;

    private final Runnable mConnectionRetryRunnable = this::startCarService;

    private final Runnable mConnectionRetryFailedRunnable = new Runnable() {
        @Override
        public void run() {
            mServiceConnectionListener.onServiceDisconnected(new ComponentName("", ""));
        }
    };

    /**
     * Callback to notify the Lifecycle of car service.
     */
    public interface ServiceConnectedListener {
        /**
         * Car service has been connected.
         *
         * <p>This is always called in the main thread context.</p>
         * @param manager when ready is true, this is valid for api calls
         */
        void onConnected(CarActivityManager manager);

        /**
         * Car service is disconnected
         */
        default void onDisconnected() {}
    }

    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.w(TAG, "Car service disconnected, probably crashed");
            synchronized (mLock) {
                if (mConnectionState == STATE_DISCONNECTED) {
                    Log.i(TAG, "State is already disconnected, ignore");
                    // can happen when client calls disconnect before onServiceDisconnected call.
                    return;
                }
                mCarServiceBinder = null;
                handleCarDisconnectLocked();
            }
            dispatchToMainThread(isMainThread(), () -> notifyCarDisconnected());
        }
    };

    private final ServiceConnection mServiceConnectionListener = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (mLock) {
                ICarActivityService newService = ICarActivityService.Stub.asInterface(service);
                if (newService == null) {
                    Log.e(TAG, "Null binder service");
                    return;  // should not happen.
                }
                if (mCarServiceBinder != null && mCarServiceBinder.asBinder().equals(newService.asBinder())) {
                    Log.d(TAG, "Already bound service");
                    return;
                }
                mConnectionState = STATE_CONNECTED;
                mCarServiceBinder = newService;
                try {
                    service.linkToDeath(mDeathRecipient, /* flags= */ 0);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to call linkToDeath on car service binder, will not receive "
                            + "callback if car service crashes", e);
                }
            }
            Log.i(TAG, "car_service ready on main thread");
            dispatchToMainThread(isMainThread(), () -> notifyCarConnected());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Car service can pick up feature changes after restart.
            Log.w(TAG, "Car service disconnected, probably crashed");
            synchronized (mLock) {
                if (mConnectionState == STATE_DISCONNECTED) {
                    // can happen when client calls disconnect before onServiceDisconnected call.
                    return;
                }
                mCarServiceBinder = null;
                handleCarDisconnectLocked();
            }
            dispatchToMainThread(isMainThread(), () -> notifyCarDisconnected());
        }
    };

    public CarActivityServiceProvider(Context context, Handler handler) {
        mContext = context;
        mEventHandler = determineEventHandler(handler);
        mMainThreadEventHandler = determineMainThreadEventHandler(mEventHandler);
        connectService();
        Log.i(TAG, "Car service bound " + mServiceBound);
    }

    private static Handler determineMainThreadEventHandler(Handler eventHandler) {
        Looper mainLooper = Looper.getMainLooper();
        return (eventHandler.getLooper() == mainLooper) ? eventHandler : new Handler(mainLooper);
    }

    private static Handler determineEventHandler(@Nullable Handler eventHandler) {
        Handler handler = eventHandler;

        if (handler == null) {
            Looper looper = Looper.getMainLooper();
            handler = new Handler(looper);
        }
        return handler;
    }

    public void addListener(ServiceConnectedListener listener) {
        mStatusCallbacks.add(listener);
        if (mConnectionState == STATE_CONNECTED) {
            listener.onConnected(CarActivityManager.get(mContext));
        } else if (mConnectionState == STATE_DISCONNECTED) {
            listener.onDisconnected();
        }
    }

    public void removeListener(ServiceConnectedListener listener) {
        mStatusCallbacks.remove(listener);
    }

    public void release() {
        disconnectService();
    }

    private void connectService() {
        synchronized (mLock) {
            if (mConnectionState != STATE_DISCONNECTED) {
                throw new IllegalStateException("already connected or connecting");
            }
            mConnectionState = STATE_CONNECTING;
            startCarService();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        disconnectService();
        super.finalize();
    }

    private void disconnectService() {
        synchronized (mLock) {
            handleCarDisconnectLocked();
            if (mServiceBound) {
                mContext.unbindService(mServiceConnectionListener);
                mServiceBound = false;
            }
        }
    }

    private void startCarService() {
        Intent intent = new Intent("im.github.ole.taskview.action.CREATE_TASK_VIEW");
        intent.setPackage("im.github.ole.taskview");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        boolean bound = mContext.bindService(intent, mServiceConnectionListener,
                Context.BIND_AUTO_CREATE);
        synchronized (mLock) {
            if (!bound) {
                mConnectionRetryCount++;
                if (mConnectionRetryCount > 3) {
                    Log.w(TAG, "cannot bind to car service after max retry");
                    mMainThreadEventHandler.post(mConnectionRetryFailedRunnable);
                } else {
                    mEventHandler.postDelayed(mConnectionRetryRunnable, 2000);
                }
            } else {
                mEventHandler.removeCallbacks(mConnectionRetryRunnable);
                mMainThreadEventHandler.removeCallbacks(mConnectionRetryFailedRunnable);
                mConnectionRetryCount = 0;
                mServiceBound = true;
            }
        }
    }

    private void notifyCarConnected() {
        Log.i(TAG, "notify car service connected");
        CarActivityManager manager = CarActivityManager.get(mContext);
        manager.onCarConnected(mCarServiceBinder);
        for (ServiceConnectedListener callback : mStatusCallbacks) {
            callback.onConnected(manager);
        }
    }

    @GuardedBy("mLock")
    private void handleCarDisconnectLocked() {
        if (mConnectionState == STATE_DISCONNECTED) {
            // can happen when client calls disconnect with onServiceDisconnected already
            // called.
            return;
        }
        mEventHandler.removeCallbacks(mConnectionRetryRunnable);
        mMainThreadEventHandler.removeCallbacks(mConnectionRetryFailedRunnable);
        mConnectionRetryCount = 0;
        mConnectionState = STATE_DISCONNECTED;
    }

    private void notifyCarDisconnected() {
        Log.i(TAG, "notify car service disconnected");
        if (mStatusCallbacks.isEmpty()) {
            // This client does not handle car service restart.
            Log.w(TAG, "Car service crashed, client not handling it");
        } else {
            for (ServiceConnectedListener callback : mStatusCallbacks) {
                callback.onDisconnected();
            }
        }
        CarActivityManager.get(mContext).onCarDisconnected();
    }

    private static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private void dispatchToMainThread(boolean isMainThread, Runnable runnable) {
        if (isMainThread) {
            runnable.run();
        } else {
            // should dispatch to main thread.
            mMainThreadEventHandler.post(runnable);
        }
    }
}
