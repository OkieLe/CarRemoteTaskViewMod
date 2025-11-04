package com.android.wm.shell.ext.utils;

import android.view.SurfaceControl;

public class SurfaceControlHelper {
    private static final String TAG = SurfaceControlHelper.class.getSimpleName();
    private SurfaceControlHelper() {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SurfaceControl (SurfaceControl)}}.
     */
    public static SurfaceControl copy(SurfaceControl source) {
        return new SurfaceControl(source, SurfaceControlHelper.class.getSimpleName());
    }
}
