package com.android.wm.shell.ext;

import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.graphics.Rect;
import android.view.SurfaceControl;
import com.android.wm.shell.ext.ICarSystemUIProxy;
import com.android.wm.shell.ext.ICarSystemUIProxyCallback;
import java.util.List;

interface ICarActivityService {
    /**
     * Registers the caller as TaskMonitor, which can provide Task lifecycle events to CarService.
     * The caller should provide a binder token, which is used to check if the given TaskMonitor is
     * live and the reported events are from the legitimate TaskMonitor.
     */
    void registerTaskMonitor(in IBinder token) = 1;

    /**
     * Reports that a Task is created.
     */
    void onTaskAppeared(in IBinder token, in RunningTaskInfo taskInfo, in SurfaceControl leash) = 8;

    /**
     * Reports that a Task is vanished.
     */
    void onTaskVanished(in IBinder token, in RunningTaskInfo taskInfo) = 3;

    /**
     * Reports that some Task's states are changed.
     */
    void onTaskInfoChanged(in IBinder token, in RunningTaskInfo taskInfo) = 4;

    /**
     * Unregisters the caller from TaskMonitor.
     */
    void unregisterTaskMonitor(in IBinder token) = 5;

    /** See {@link CarActivityManager#getVisibleTasks(int)} */
    List<RunningTaskInfo> getVisibleTasks(int displayId) = 6;

    /**
     * Registers a System UI proxy which is meant to host all the system ui interaction that is
     * required by other apps.
     */
    void registerCarSystemUIProxy(in ICarSystemUIProxy carSystemUIProxy) = 12;

    /**
     * Adds a callback to monitor the lifecycle of System UI proxy. Calling this for an already
     * registered callback will result in a no-op.
     */
    void addCarSystemUIProxyCallback(in ICarSystemUIProxyCallback callback) = 13;

    /**
     * Removes the callback to monitor the lifecycle of System UI proxy.
     * Calling this for an already unregistered callback will result in a no-op
     */
    void removeCarSystemUIProxyCallback(in ICarSystemUIProxyCallback callback) = 14;

    /**
     * Returns true if the {@link CarSystemUIProxy} is registered, false otherwise.
     */
    boolean isCarSystemUIProxyRegistered() = 16;
}
