package io.github.ole.taskview;

import io.github.ole.taskview.ITaskHostCallback;

interface ITaskHost {

    /**
     * Register a host to the channel
     * @param host host to register
     */
    oneway void registerHostCallback(ITaskHostCallback host);

    /**
     * Unregister a host from the channel
     * @param host host to unregister
     */
    oneway void unregisterHostCallback(ITaskHostCallback host);

    /**
     * User presses the back button in task, this should be handled by host
     */
    oneway void onOverlayBackPressed();
}
