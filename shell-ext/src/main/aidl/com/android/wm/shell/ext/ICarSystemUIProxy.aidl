package com.android.wm.shell.ext;

import android.view.SurfaceControl;
import android.os.Bundle;
import android.app.PendingIntent;
import android.graphics.Rect;
import com.android.wm.shell.ext.ICarTaskViewClient;
import com.android.wm.shell.ext.ICarTaskViewHost;


/**
 * Binder API for {@link CarSystemUIProxy}
 */
interface ICarSystemUIProxy {
    /**
     * @deprecated, use createRootTaskView()
     */
    ICarTaskViewHost createControlledCarTaskView(in ICarTaskViewClient client);

    /**
     * Creates the host side of the task view and links the provided {@code carTaskVIewClient}
     * to the same.
     *
     * @return a handle to the host side of task view.
     */
    ICarTaskViewHost createCarTaskView(in ICarTaskViewClient client);
}
