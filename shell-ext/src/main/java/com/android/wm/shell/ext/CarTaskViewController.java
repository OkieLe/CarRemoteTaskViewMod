package com.android.wm.shell.ext;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.UiContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * This class is used for creating task views & is created on a per activity basis.
 */
public final class CarTaskViewController {
    private static final String TAG = CarTaskViewController.class.getSimpleName();
    static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    private final ICarSystemUIProxy mService;
    private final Context mHostContext;
    private final CarTaskViewControllerHostLifecycle mLifecycle;
    private final List<RemoteCarTaskView> mRemoteCarTaskViews =
            new ArrayList<>();
    private final CarTaskViewInputInterceptor mTaskViewInputInterceptor;

    private boolean mReleased = false;

    /**
     * @param service the binder interface to communicate with the car system UI.
     */
    CarTaskViewController(@UiContext Context hostContext,
                          @NonNull CarTaskViewControllerHostLifecycle lifecycle,
                          @NonNull ICarSystemUIProxy service) {
        mHostContext = hostContext;
        mService = service;
        mLifecycle = lifecycle;
        mTaskViewInputInterceptor = new CarTaskViewInputInterceptor(hostContext, lifecycle, this);
    }

    /**
     * Creates a new {@link ControlledRemoteCarTaskView}.
     *
     * @param callbackExecutor the executor to get the {@link ControlledRemoteCarTaskViewCallback}
     *                         on.
     * @param controlledRemoteCarTaskViewCallback the callback to monitor the
     *                                            {@link ControlledRemoteCarTaskView} related
     *                                            events.
     */
    @MainThread
    public void createControlledRemoteCarTaskView(
            @NonNull ControlledRemoteCarTaskViewConfig controlledRemoteCarTaskViewConfig,
            @NonNull Executor callbackExecutor,
            @NonNull ControlledRemoteCarTaskViewCallback controlledRemoteCarTaskViewCallback) {
        if (mReleased) {
            throw new IllegalStateException("CarTaskViewController is already released");
        }
        ControlledRemoteCarTaskView taskViewClient =
                new ControlledRemoteCarTaskView(
                        mHostContext,
                        controlledRemoteCarTaskViewConfig,
                        callbackExecutor,
                        controlledRemoteCarTaskViewCallback,
                        /* carTaskViewController= */ this,
                        mHostContext.getSystemService(UserManager.class));

        try {
            ICarTaskViewHost host = mService.createControlledCarTaskView(
                    taskViewClient.mICarTaskViewClient);
            taskViewClient.setRemoteHost(host);
            mRemoteCarTaskViews.add(taskViewClient);

            if (controlledRemoteCarTaskViewConfig.mShouldCaptureGestures
                    || controlledRemoteCarTaskViewConfig.mShouldCaptureLongPress) {
                assertPermission(Manifest.permission.INJECT_EVENTS);
                assertPermission(Manifest.permission.INTERNAL_SYSTEM_WINDOW);
                mTaskViewInputInterceptor.init();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to create task view.", e);
        }
    }

    void onRemoteCarTaskViewReleased(@NonNull RemoteCarTaskView taskView) {
        if (mReleased) {
            Log.w(TAG, "Failed to remove the taskView as the "
                    + "CarTaskViewController is already released");
            return;
        }
        if (!mRemoteCarTaskViews.contains(taskView)) {
            Log.w(TAG, "This taskView has already been removed");
            return;
        }
        mRemoteCarTaskViews.remove(taskView);
    }

    private void assertPermission(String permission) {
        if (mHostContext.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("requires " + permission);
        }
    }

    /**
     * Releases all the resources held by the taskviews associated with this controller.
     *
     * <p> Once {@link #release()} is called, the current instance of {@link CarTaskViewController}
     * cannot be used further. A new instance should be requested using
     * {@link CarActivityManager#getCarTaskViewController(Activity, Executor,
     * CarTaskViewControllerCallback)}.
     */
    @MainThread
    public void release() {
        if (mReleased) {
            Log.w(TAG, "CarTaskViewController is already released");
            return;
        }
        releaseTaskViews();
        mTaskViewInputInterceptor.release();
        mReleased = true;
    }

    @MainThread
    void releaseTaskViews() {
        Iterator<RemoteCarTaskView> iterator = mRemoteCarTaskViews.iterator();
        while (iterator.hasNext()) {
            RemoteCarTaskView taskView = iterator.next();
            // Remove the task view here itself because release triggers removal again which can
            // result in concurrent modification exception.
            iterator.remove();
            taskView.release();
        }
    }

    /**
     * Brings all the embedded tasks to the front.
     */
    @MainThread
    public void showEmbeddedTasks() {
        if (mReleased) {
            throw new IllegalStateException("CarTaskViewController is already released");
        }
        for (int i = 0, length = mRemoteCarTaskViews.size(); i < length; i++) {
            RemoteCarTaskView remoteCarTaskView = mRemoteCarTaskViews.get(i);
            // TODO(b/267314188): Add a new method in ICarSystemUI to call
            // showEmbeddedTask in a single WCT for multiple tasks.
            remoteCarTaskView.showEmbeddedTask();
        }
    }

    /**
     * Brings all the embedded controlled tasks to the front.
     */
    @MainThread
    void showEmbeddedControlledTasks() {
        if (mReleased) {
            throw new IllegalStateException("CarTaskViewController is already released");
        }
        for (int i = 0, length = mRemoteCarTaskViews.size(); i < length; i++) {
            RemoteCarTaskView carTaskView = mRemoteCarTaskViews.get(i);
            // TODO(b/267314188): Add a new method in ICarSystemUI to call
            // showEmbeddedTask in a single WCT for multiple tasks.
            if (carTaskView instanceof ControlledRemoteCarTaskView) {
                carTaskView.showEmbeddedTask();
            }
        }
    }

    boolean isHostVisible() {
        return mLifecycle.isVisible();
    }

    List<RemoteCarTaskView> getRemoteCarTaskViews() {
        return mRemoteCarTaskViews;
    }
}
