package com.android.wm.shell.ext;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceControl;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiContext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * API to manage {@link android.app.Activity} in Car.
 */
public final class CarActivityManager {
    private static final String TAG = CarActivityManager.class.getSimpleName();

    private final Context mContext;

    private ICarActivityService mService;
    private IBinder mTaskMonitorToken;
    private CarTaskViewControllerSupervisor mCarTaskViewControllerSupervisor;

    @SuppressLint("StaticFieldLeak")
    private static volatile CarActivityManager sManager;
    static synchronized CarActivityManager get(@NonNull Context context) {
        if (sManager == null) {
            synchronized (CarActivityManager.class) {
                if (sManager == null) {
                    sManager = new CarActivityManager(context.getApplicationContext());
                }
            }
        }
        return sManager;
    }

    private CarActivityManager(@NonNull Context context) {
        mContext = context;
    }

    void onCarConnected(ICarActivityService service) {
        mService = service;
    }

    void onCarDisconnected() {
        mService = null;
        mTaskMonitorToken = null;
    }

    /*
     * Registers the caller as TaskMonitor, which can provide Task lifecycle events to CarService.
     * The caller should provide a binder token, which is used to check if the given TaskMonitor is
     * live and the reported events are from the legitimate TaskMonitor.
     */
    public boolean registerTaskMonitor() {
        if(mTaskMonitorToken != null) {
            throw new IllegalStateException("Can't register the multiple TaskMonitors");
        }
        IBinder token = new Binder();
        try {
            mService.registerTaskMonitor(token);
            mTaskMonitorToken = token;
            return true;
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
        return false;
    }

    /**
     * Reports that a Task is created.
     */
    public void onTaskAppeared(ActivityManager.RunningTaskInfo taskInfo,
                               @Nullable SurfaceControl leash) {
        onTaskAppearedInternal(taskInfo, leash);
    }

    private void onTaskAppearedInternal(
            ActivityManager.RunningTaskInfo taskInfo, SurfaceControl leash) {
        if (noValidToken()) return;
        try {
            mService.onTaskAppeared(mTaskMonitorToken, taskInfo, leash);
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
    }

    /**
     * Reports that a Task is vanished.
     */
    public void onTaskVanished(ActivityManager.RunningTaskInfo taskInfo) {
        if (noValidToken()) return;
        try {
            mService.onTaskVanished(mTaskMonitorToken, taskInfo);
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
    }

    /**
     * Reports that some Task's states are changed.
     */
    public void onTaskInfoChanged(ActivityManager.RunningTaskInfo taskInfo) {
        if (noValidToken()) return;
        try {
            mService.onTaskInfoChanged(mTaskMonitorToken, taskInfo);
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
    }

    /**
     * Unregisters the caller from TaskMonitor.
     */
    public void unregisterTaskMonitor() {
        if (noValidToken()) return;
        try {
            mService.unregisterTaskMonitor(mTaskMonitorToken);
            mTaskMonitorToken = null;
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
    }

    /**
     * Returns all the visible tasks in the all displays. The order is not guaranteed.
     */
    @NonNull
    public List<ActivityManager.RunningTaskInfo> getVisibleTasks() {
        try {
            return mService.getVisibleTasks(Display.INVALID_DISPLAY);
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
        return Collections.emptyList();
    }

    /**
     * Returns all the visible tasks in the given display. The order is not guaranteed.
     *
     * @param displayId the id of {@link Display} to retrieve the tasks,
     *         {Display.INVALID_DISPLAY} to retrieve the tasks in the all displays.
     */
    @NonNull
    public List<ActivityManager.RunningTaskInfo> getVisibleTasks(int displayId) {
        try {
            return mService.getVisibleTasks(displayId);
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
        return Collections.emptyList();
    }

    /**
     * Registers a system ui proxy which will be used by the client apps to interact with the
     * system-ui for things like creating task views, getting notified about immersive mode
     * request, etc.
     *
     * <p>This is meant to be called only by the SystemUI.
     *
     * @param carSystemUIProxy the implementation of the {@link CarSystemUIProxy}.
     * @throws UnsupportedOperationException when called more than once for the same SystemUi
     *         process.
     */
    public void registerCarSystemUIProxy(@NonNull CarSystemUIProxy carSystemUIProxy) {
        try {
            mService.registerCarSystemUIProxy(new CarSystemUIProxyAidlWrapper(carSystemUIProxy));
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
    }

    /**
     * Returns true if the {@link CarSystemUIProxy} is registered, false otherwise.
     */
    public boolean isCarSystemUIProxyRegistered() {
        try {
            return mService.isCarSystemUIProxyRegistered();
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
            return false;
        }
    }

    /**
     * Gets the {@link CarTaskViewController} using the {@code carTaskViewControllerCallback}.
     *
     * This method is expected to be called from the {Activity#onCreate(Bundle)}. It will
     * take care of freeing up the held resources when activity is destroyed. If an activity is
     * recreated, it should be called again in the next {Activity#onCreate(Bundle)}.
     *
     * @param carTaskViewControllerCallback the callback which the client can use to monitor the
     *                                      lifecycle of the {@link CarTaskViewController}.
     * @param hostActivity the activity that will host the taskviews.
     */
    @MainThread
    public void getCarTaskViewController(
            @NonNull Activity hostActivity,
            @NonNull Executor callbackExecutor,
            @NonNull CarTaskViewControllerCallback carTaskViewControllerCallback) {
        getCarTaskViewController(
                hostActivity,
                CarTaskViewControllerHostLifecycleFactory.forActivity(hostActivity),
                callbackExecutor,
                carTaskViewControllerCallback);
    }

    /**
     * Gets the {@link CarTaskViewController} using the {@code carTaskViewControllerCallback}.
     *
     * This method is expected to be called when the container (host) is created. It will
     * take care of freeing up the held resources when container is destroyed. If the container is
     * recreated, this method should be called again after it gets created again.
     *
     * @param carTaskViewControllerCallback the callback which the client can use to monitor the
     *                                      lifecycle of the {@link CarTaskViewController}.
     * @param hostContext the visual hostContext which the container (host) is associated with.
     * @param callbackExecutor the executor which the {@code carTaskViewControllerCallback} will be
     *                         executed on.
     * @param carTaskViewControllerHostLifecycle the lifecycle of the container (host).
     */
    @MainThread
    public void getCarTaskViewController(
            @UiContext @NonNull Context hostContext,
            @NonNull CarTaskViewControllerHostLifecycle carTaskViewControllerHostLifecycle,
            @NonNull Executor callbackExecutor,
            @NonNull CarTaskViewControllerCallback carTaskViewControllerCallback) {
        try {
            if (mCarTaskViewControllerSupervisor == null) {
                // Same supervisor is used for multiple activities.
                mCarTaskViewControllerSupervisor = new CarTaskViewControllerSupervisor(mService,
                        mContext.getMainExecutor());
            }
            mCarTaskViewControllerSupervisor.createCarTaskViewController(
                    hostContext,
                    carTaskViewControllerHostLifecycle,
                    callbackExecutor,
                    carTaskViewControllerCallback);
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
    }

    private boolean noValidToken() {
        boolean valid = mTaskMonitorToken != null;
        if (!valid) {
            Log.w(TAG, "Has invalid token, skip the operation: "
                    + new Throwable().getStackTrace()[1].getMethodName());
        }
        return !valid;
    }

    private void handleRemoteExceptionFromCarService(RemoteException e) {
        if (e instanceof TransactionTooLargeException) {
            Log.w(TAG, "Car service threw TransactionTooLargeException", e);
            throw new IllegalStateException("Car service threw TransactionTooLargeException");
        } else {
            Log.w(TAG, "Car service has crashed", e);
        }
    }
}
