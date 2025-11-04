package com.android.wm.shell.ext.utils;

import android.graphics.Rect;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

public final class ViewHelper {
    /**
     * See {@link View#getBoundsOnScreen(Rect)}}.
     */
    public static void getBoundsOnScreen(@NonNull View v, @NonNull Rect outRect) {
        v.getBoundsOnScreen(outRect);
    }

    /**
     * See {@link SurfaceView#setResizeBackgroundColor(int)}}.
     */
    public static void seResizeBackgroundColor(@NonNull SurfaceView surfaceView, int color) {
        surfaceView.setResizeBackgroundColor(color);
    }

    private ViewHelper() {
        throw new UnsupportedOperationException();
    }
}
