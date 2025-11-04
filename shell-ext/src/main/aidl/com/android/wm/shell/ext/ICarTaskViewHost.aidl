package com.android.wm.shell.ext;

import android.app.ActivityManager.RunningTaskInfo;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.SurfaceControl;

/**
 * Binder API for server side of RemoteCarTaskView i.e. CarTaskViewServerImpl.
 * See {@link CarTaskViewHost} for details.
 */
oneway interface ICarTaskViewHost {
    void release();
    void startActivity(in PendingIntent pendingIntent, in Intent intent, in Bundle options, in Rect launchBounds);
    void notifySurfaceCreated(in SurfaceControl control);
    void setWindowBounds(in Rect bounds);
    void notifySurfaceDestroyed();
    void showEmbeddedTask();
    void addInsets(int index, int type, in Rect frame);
    void removeInsets(int index, int type);
    void setTaskVisibility(boolean visibility);
    void reorderTask(boolean onTop);
}
