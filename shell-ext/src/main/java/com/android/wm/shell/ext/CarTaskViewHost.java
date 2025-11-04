package com.android.wm.shell.ext;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.SurfaceControl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This is a blueprint to implement the host part of {@link CarTaskViewClient}.
 */
public interface CarTaskViewHost {
    /** Releases the resources held by this task view's host side. */
    void release();

    /**
     * See {TaskView#startActivity(PendingIntent, Intent,
     * ActivityOptions, Rect)}
     */
    void startActivity(
            @NonNull PendingIntent pendingIntent, @Nullable Intent intent, @NonNull Bundle options,
            @Nullable Rect launchBounds);

    /**
     * Notifies the host side that the client surface has been created.
     *
     * @param control the {@link SurfaceControl} of the surface that has been created.
     */
    void notifySurfaceCreated(@NonNull SurfaceControl control);

    /**
     * Sets the bounds of the window for the underlying Task.
     *
     * @param windowBoundsOnScreen the new bounds in screen coordinates.
     */
    void setWindowBounds(@NonNull Rect windowBoundsOnScreen);

    /** Notifies the host side that the client surface has been destroyed. */
    void notifySurfaceDestroyed();

    /** Brings the embedded Task to the front in the WM Hierarchy. */
    void showEmbeddedTask();

    /**
     * Sets the visibility of the embedded task.
     */
    void setTaskVisibility(boolean visibility);

    /**
     * Reorders the embedded task to top when {@code onTop} is {@code true} and to bottom when
     * its {@code false}.
     */
    void reorderTask(boolean onTop);
    /**
     * Adds the given {@code insets} on the Task.
     *
     * <p>
     * The given rectangle for the given insets type is applied to the underlying task right
     * away.
     * If a rectangle for an insets type was added previously, it will be replaced with the
     * new value.
     * If a rectangle for an insets type was already added, but is not specified currently in
     * {@code insets}, it will remain applied to the task. Clients should explicitly call
     * {@link #removeInsets(int, int)} to remove the rectangle for that insets type from
     * the underlying task.
     *
     * @param index An owner might add multiple insets sources with the same type.
     *              This identifies them.
     * @param type  The insets type of the insets source. This doesn't accept the composite types.
     * @param frame The rectangle area of the insets source.
     */
    void addInsets(int index, int type, @NonNull Rect frame);

    /**
     * Removes the insets for the given @code index}, and {@code type} that were added via
     * {@link #addInsets(int, int, Rect)}
     */
    void removeInsets(int index, int type);
}
