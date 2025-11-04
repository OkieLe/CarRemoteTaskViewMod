package com.android.wm.shell.ext;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceControl;

import androidx.annotation.NonNull;

/**
 * An adapter that adapts {@link ICarTaskViewHost} to {@link CarTaskViewHost}.
 */
final class CarTaskViewHostAidlToImplAdapter extends ICarTaskViewHost.Stub {
    private static final String TAG = "CarTaskViewHostImpl";
    private final CarTaskViewHost mCarTaskViewHost;

    CarTaskViewHostAidlToImplAdapter(CarTaskViewHost carTaskViewHost) {
        mCarTaskViewHost = carTaskViewHost;
    }

    @Override
    public void release() {
        Log.d(TAG, "release");
        mCarTaskViewHost.release();
    }

    @Override
    public void startActivity(
            PendingIntent pendingIntent, Intent intent, Bundle options, Rect launchBounds) {
        Log.d(TAG, "startActivity");
        mCarTaskViewHost.startActivity(pendingIntent, intent, options, launchBounds);
    }

    @Override
    public void notifySurfaceCreated(SurfaceControl control) {
        Log.d(TAG, "notifySurfaceCreated");
        mCarTaskViewHost.notifySurfaceCreated(control);
    }

    @Override
    public void setWindowBounds(Rect windowBoundsOnScreen) {
        Log.d(TAG, "setWindowBounds " + windowBoundsOnScreen);
        mCarTaskViewHost.setWindowBounds(windowBoundsOnScreen);
    }

    @Override
    public void notifySurfaceDestroyed() {
        Log.d(TAG, "notifySurfaceDestroyed");
        mCarTaskViewHost.notifySurfaceDestroyed();
    }

    @Override
    public void showEmbeddedTask() {
        Log.d(TAG, "showEmbeddedTask");
        mCarTaskViewHost.showEmbeddedTask();
    }

    @Override
    public void setTaskVisibility(boolean visibility) {
        Log.d(TAG, "setTaskVisibility " + visibility);
        mCarTaskViewHost.setTaskVisibility(visibility);
    }

    @Override
    public void reorderTask(boolean onTop) {
        Log.d(TAG, "reorderTask " + onTop);
        mCarTaskViewHost.reorderTask(onTop);
    }

    @Override
    public void addInsets(int index, int type, @NonNull Rect frame) {
        mCarTaskViewHost.addInsets(index, type, frame);
    }

    @Override
    public void removeInsets(int index, int type) {
        mCarTaskViewHost.removeInsets(index, type);
    }
}
