package com.android.wm.shell.ext;

import android.app.ActivityManager;

import androidx.annotation.NonNull;

/**
 * A callback interface for the host activity that uses {@link RemoteCarTaskView} and its
 * derivatives.
 */
interface RemoteCarTaskViewCallback<T extends RemoteCarTaskView> {
    /**
     * Called when the underlying {@link RemoteCarTaskView} instance is created.
     *
     * @param taskView the new newly created {@link RemoteCarTaskView} instance.
     */
    default void onTaskViewCreated(@NonNull T taskView) {}

    /**
     * Called when the underlying {@link RemoteCarTaskView} is ready. A {@link RemoteCarTaskView}
     * can be considered ready when it has completed all the set up that is required.
     * This callback is only triggered once.
     */
    default void onTaskViewInitialized() {}

    /**
     * Called when the underlying {@link RemoteCarTaskView} is released.
     * This callback is only triggered once.
     */
    default void onTaskViewReleased() {}

    /**
     * Called when the task has appeared in the taskview.
     *
     * @param taskInfo the taskInfo of the task that has appeared.
     */
    default void onTaskAppeared(@NonNull ActivityManager.RunningTaskInfo taskInfo) {}

    /**
     * Called when the task's info has changed.
     *
     * @param taskInfo the taskInfo of the task that has a change in info.
     */
    default void onTaskInfoChanged(@NonNull ActivityManager.RunningTaskInfo taskInfo) {}

    /**
     * Called when the task has vanished.
     *
     * @param taskInfo the taskInfo of the task that has vanished.
     */
    default void onTaskVanished(@NonNull ActivityManager.RunningTaskInfo taskInfo) {}
}
