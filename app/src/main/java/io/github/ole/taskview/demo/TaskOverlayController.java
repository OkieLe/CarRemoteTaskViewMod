package io.github.ole.taskview.demo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import io.github.ole.taskview.ITaskOverlay;
import io.github.ole.taskview.ITaskOverlayCallback;

public class TaskOverlayController {
    private static final String TAG = "TaskOverlayController";
    private final Context mContext;
    private final AtomicInteger mConnectionCount = new AtomicInteger(0);
    private Supplier<Boolean> mBackHandler;

    private boolean mBound;
    private ITaskOverlay mTaskOverlay;

    private final ITaskOverlayCallback mOverlayCallback = new ITaskOverlayCallback.Stub() {
        @Override
        public void onOverlayBackPressed() {
            if (mBackHandler != null) {
                Log.i(TAG, "Handled back: " + mBackHandler.get());
            }
        }
    };

    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            mTaskOverlay = null;
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTaskOverlay = ITaskOverlay.Stub.asInterface(service);
            try {
                service.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to link to death", e);
                return;
            }
            try {
                mTaskOverlay.registerOverlayCallback(mOverlayCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to register overlay", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            try {
                mTaskOverlay.unregisterOverlayCallback(mOverlayCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to unregister overlay", e);
            }
            mTaskOverlay.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mTaskOverlay = null;
        }
    };

    public synchronized static TaskOverlayController get(Context context) {
        return new TaskOverlayController(context);
    }

    private TaskOverlayController(Context context) {
        mContext = context.getApplicationContext();
    }

    public void start() {
        mConnectionCount.incrementAndGet();
        if (mTaskOverlay != null) {
            return;
        }
        if (mBound) {
            unbindTaskOverlay();
        }
        bindTaskOverlay();
    }

    private void bindTaskOverlay() {
        Intent intent = new Intent("com.android.launcher3.wm.action.GET_OVERLAY");
        intent.setPackage("com.android.launcher3");
        mBound = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindTaskOverlay() {
        mContext.unbindService(mConnection);
        mBound = false;
    }

    public void setBackHandler(Supplier<Boolean> handler) {
        mBackHandler = handler;
    }

    public void setBackInterceptable(boolean enabled) {
        try {
            mTaskOverlay.setBackInterceptable(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set back interceptable", e);
        }
    }

    public void stop() {
        int count = mConnectionCount.decrementAndGet();
        if (mTaskOverlay == null || count > 0) {
            return;
        }
        unbindTaskOverlay();
    }
}
