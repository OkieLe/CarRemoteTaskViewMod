package io.github.ole.taskview;

interface ITaskOverlayCallback {

    /**
     * User presses the back button in overlay, this should be handled by host
     */
    oneway void onOverlayBackPressed();
}
