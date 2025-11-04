package com.android.wm.shell.ext.utils;

import android.hardware.input.InputManager;
import android.view.View;

import androidx.annotation.NonNull;

public class InputManagerHelper {

    private InputManagerHelper() {
        throw new UnsupportedOperationException();
    }

    /**
     * Injects the event passed as parameter in async mode.
     *
     * @param inputManager the Android input manager used to inject the event
     * @param event        the event to inject
     * @return {@code true} if injection succeeds
     */
    public static boolean injectInputEvent(@NonNull InputManager inputManager,
                                           @NonNull android.view.InputEvent event) {
        return inputManager.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    /**
     * See {@link InputManager#pilferPointers(IBinder)}.
     */
    public static void pilferPointers(@NonNull InputManager inputManager, @NonNull View v) {
        inputManager.pilferPointers(v.getViewRootImpl().getInputToken());
    }

    /**
     * See {@link InputManager#addUniqueIdAssociationByDescriptor(String, String)}.
     */
    public static void addUniqueIdAssociationByDescriptor(@NonNull InputManager inputManager,
                                                          @NonNull String inputDeviceDescriptor,
                                                          @NonNull String displayUniqueId) {
        // TODO(b/341949977): Improve addUniqueIdAssociationByDescriptor to handle incorrect
        // input
        inputManager.addUniqueIdAssociationByDescriptor(inputDeviceDescriptor, displayUniqueId);
    }

    /**
     * See {@link InputManager#removeUniqueIdAssociationByDescriptor(String)}.
     */
    public static void removeUniqueIdAssociationByDescriptor(@NonNull InputManager inputManager,
                                                             @NonNull String inputDeviceDescriptor) {
        // TODO(b/341949977): Improve removeUniqueIdAssociationByDescriptor to handle incorrect
        // input
        inputManager.removeUniqueIdAssociationByDescriptor(inputDeviceDescriptor);
    }
}
