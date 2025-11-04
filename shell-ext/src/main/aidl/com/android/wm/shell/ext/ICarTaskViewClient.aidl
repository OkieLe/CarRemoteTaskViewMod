package com.android.wm.shell.ext;

import android.app.ActivityManager.RunningTaskInfo;
import android.graphics.Rect;
import android.view.SurfaceControl;

/**
 * Binder API to be implemented by the client side of RemoteCarTaskView. This will be used by the
 * host to call into the client.
 * See {@link CarTaskViewClient} for details.
 */
interface ICarTaskViewClient {
    Rect getCurrentBoundsOnScreen();
    void setResizeBackgroundColor(in SurfaceControl.Transaction transaction, int color);
    void onTaskAppeared(in RunningTaskInfo taskInfo, in SurfaceControl leash);
    void onTaskVanished(in RunningTaskInfo taskInfo);
    void onTaskInfoChanged(in RunningTaskInfo taskInfo);
}
