package com.android.wm.shell.ext;

import androidx.annotation.NonNull;

/**
 * A blueprint for the system ui proxy which is meant to host all the system ui interaction that is
 * required by other apps.
 */
public interface CarSystemUIProxy {
    /**
     * Creates the host side of the task view and links the provided {@code carTaskViewClient}
     * to the same.
     * This method will be deprecated in Android 15. Please use
     * {@link CarSystemUIProxy#createCarTaskView(CarTaskViewClient)} instead.
     * @return a handle to the host side of task view.
     */
    @NonNull
    CarTaskViewHost createControlledCarTaskView(@NonNull CarTaskViewClient carTaskViewClient);

    /**
     * Creates the host side of the task view and links the provided {@code
     * carTaskViewClient} to the same.
     * @return a handle to the host side of task view.
     */
    @NonNull
    CarTaskViewHost createCarTaskView(@NonNull CarTaskViewClient carTaskViewClient);
}
