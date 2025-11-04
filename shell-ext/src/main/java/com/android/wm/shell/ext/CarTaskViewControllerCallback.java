package com.android.wm.shell.ext;

import androidx.annotation.NonNull;

/**
 * Callback interface required to monitor the lifecycle of {@link CarTaskViewController}.
 */
public interface CarTaskViewControllerCallback {
    /**
     * Called when the {@code carTaskViewController} is connected.
     */
    void onConnected(@NonNull CarTaskViewController carTaskViewController);

    /**
     * Called when the {@code carTaskViewController} is disconnected.
     */
    void onDisconnected(@NonNull CarTaskViewController carTaskViewController);
}
