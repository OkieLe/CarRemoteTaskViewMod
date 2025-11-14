package io.github.ole.taskview;

import io.github.ole.taskview.ITaskOverlayCallback;

interface ITaskOverlay {
    /**
     * Register an overlay to the host.
     * @param overlay overlay to register
     */
    oneway void registerOverlayCallback(ITaskOverlayCallback overlay);
    /**
     * Unregister an overlay from the host.
     * @param overlay overlay to unregister
     */
    oneway void unregisterOverlayCallback(ITaskOverlayCallback overlay);

    /**
     * Set whether the back event should be intercepted by the host.
     */
    oneway void setBackInterceptable(boolean enabled);
}
