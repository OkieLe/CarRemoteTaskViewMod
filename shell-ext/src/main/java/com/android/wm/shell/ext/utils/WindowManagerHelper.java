package com.android.wm.shell.ext.utils;

import android.view.WindowManager;

import androidx.annotation.NonNull;

public final class WindowManagerHelper {
    /**
     * See {@link WindowManager.LayoutParams#inputFeatures}}.
     */
    public static void setInputFeatureSpy(@NonNull WindowManager.LayoutParams p) {
        p.inputFeatures = WindowManager.LayoutParams.INPUT_FEATURE_SPY;
    }

    /**
     * See {@link WindowManager.LayoutParams#privateFlags}}.
     */
    public static void setTrustedOverlay(@NonNull WindowManager.LayoutParams p) {
        p.setTrustedOverlay();
    }

    private WindowManagerHelper() {
        throw new UnsupportedOperationException();
    }
}
