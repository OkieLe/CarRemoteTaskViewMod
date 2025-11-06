package com.android.launcher3;

import android.annotation.LayoutRes;
import android.app.Activity;
import android.content.Intent;

import com.android.systemui.plugins.shared.LauncherOverlayManager;

import io.github.ole.taskview.R;

/**
 * Mock class to make TaskOverlayManager compilable
 * @see [com.android.launcher3.Launcher] for details
 *
 * Check mOverlayManager in real Launcher for lifecycle dispatching
 */
public class Launcher extends Activity {
    @LayoutRes
    public int getContainerLayout() {
        return R.layout.task_overlay;
    }

    public Intent getTaskLaunchIntent() {
        return new Intent();
    }

    public void setLauncherOverlay(LauncherOverlayManager.LauncherOverlayTouchProxy touchProxy) {

    }
}
