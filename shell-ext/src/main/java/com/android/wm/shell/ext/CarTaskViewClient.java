package com.android.wm.shell.ext;

import android.app.ActivityManager;
import android.graphics.Rect;
import android.os.RemoteException;
import android.view.SurfaceControl;

import androidx.annotation.NonNull;

/**
 * Represents the client part of the task view as seen by the server. This wraps the AIDL based
 * communication with the client apps.
 */
public final class CarTaskViewClient {
    private final ICarTaskViewClient mICarTaskViewClient;

    CarTaskViewClient(ICarTaskViewClient iCarCarTaskViewClient) {
        mICarTaskViewClient = iCarCarTaskViewClient;
    }

    /** Returns the current bounds (in pixels) on screen for the task view's view part. */
    @NonNull
    public Rect getCurrentBoundsOnScreen() {
        try {
            return mICarTaskViewClient.getCurrentBoundsOnScreen();
        } catch (RemoteException ex) {
            ex.rethrowFromSystemServer();
        }
        return null; // cannot reach here. This is just to satisfy compiler.
    }

    /**
     * Sets the resize background color on the task view's view part.
     *
     * <p>See {android.view.SurfaceView#setResizeBackgroundColor(SurfaceControl.Transaction,
     * int)}
     */
    public void setResizeBackgroundColor(@NonNull SurfaceControl.Transaction transaction,
                                         int color) {
        try {
            mICarTaskViewClient.setResizeBackgroundColor(transaction, color);
        } catch (RemoteException ex) {
            ex.rethrowFromSystemServer();
        }
    }

    /** Called when a task has appeared on the TaskView. */
    public void onTaskAppeared(@NonNull ActivityManager.RunningTaskInfo taskInfo,
                               @NonNull SurfaceControl leash) {
        try {
            mICarTaskViewClient.onTaskAppeared(taskInfo, leash);
        } catch (RemoteException ex) {
            ex.rethrowFromSystemServer();
        }
    }

    /** Called when a task has vanished from the TaskView. */
    public void onTaskVanished(@NonNull ActivityManager.RunningTaskInfo taskInfo) {
        try {
            mICarTaskViewClient.onTaskVanished(taskInfo);
        } catch (RemoteException ex) {
            ex.rethrowFromSystemServer();
        }
    }

    /** Called when the task in the TaskView is changed. */
    public void onTaskInfoChanged(@NonNull ActivityManager.RunningTaskInfo taskInfo) {
        try {
            mICarTaskViewClient.onTaskInfoChanged(taskInfo);
        } catch (RemoteException ex) {
            ex.rethrowFromSystemServer();
        }
    }
}
