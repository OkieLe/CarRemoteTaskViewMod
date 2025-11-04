package com.android.wm.shell.ext.system.taskview;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static com.android.wm.shell.ext.system.CarSystemUIProxyImpl.ensureManageSystemUIPermission;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.DeadSystemRuntimeException;
import android.util.Log;
import android.util.SparseArray;
import android.view.InsetsSource;
import android.view.SurfaceControl;
import android.window.WindowContainerTransaction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.Keep;
import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.common.SyncTransactionQueue;
import com.android.wm.shell.ext.CarTaskViewClient;
import com.android.wm.shell.ext.CarTaskViewHost;
import com.android.wm.shell.ext.system.CarSystemUIProxyImpl;
import com.android.wm.shell.taskview.TaskViewBase;
import com.android.wm.shell.taskview.TaskViewTaskController;
import com.android.wm.shell.taskview.TaskViewTransitions;

/** Server side implementation for {@code RemoteCarTaskView}. */
public class RemoteCarTaskViewServerImpl implements TaskViewBase {
    private static final String TAG = RemoteCarTaskViewServerImpl.class.getSimpleName();

    private final Context mContext;
    private final CarTaskViewClient mCarTaskViewClient;
    private final TaskViewTaskController mTaskViewTaskController;
    private final CarSystemUIProxyImpl mCarSystemUIProxy;
    private final Binder mInsetsOwner = new Binder();
    private final SparseArray<Rect> mInsets = new SparseArray<>();
    private final ShellTaskOrganizer mShellTaskOrganizer;
    private final TaskViewTransitions mTaskViewTransitions;

    private boolean mReleased;

    private final CarTaskViewHost mHostImpl = new CarTaskViewHost() {
        @Override
        public void release() {
            ensureManageSystemUIPermission(mContext);
            if (mReleased) {
                Log.w(TAG, "TaskView server part already released");
                return;
            }
            mInsets.clear();
            int taskIdToRemove = INVALID_TASK_ID;
            if (mTaskViewTaskController.getTaskInfo() != null) {
                taskIdToRemove = mTaskViewTaskController.getTaskInfo().taskId;
            }
            mTaskViewTaskController.release();

            if (taskIdToRemove != INVALID_TASK_ID) {
                Log.w(TAG, "Removing embedded task: " + taskIdToRemove);
                ActivityTaskManager.getInstance().removeTask(taskIdToRemove);
            }
            mCarSystemUIProxy.onCarTaskViewReleased(RemoteCarTaskViewServerImpl.this);
            mReleased = true;
        }

        @Override
        public void notifySurfaceCreated(SurfaceControl control) {
            ensureManageSystemUIPermission(mContext);
            mTaskViewTaskController.surfaceCreated(control);
        }

        @Override
        public void setWindowBounds(Rect bounds) {
            ensureManageSystemUIPermission(mContext);
            mTaskViewTaskController.setWindowBounds(bounds);
        }

        @Override
        public void notifySurfaceDestroyed() {
            ensureManageSystemUIPermission(mContext);
            mTaskViewTaskController.surfaceDestroyed();
        }

        @Override
        public void startActivity(
                PendingIntent pendingIntent,
                Intent fillInIntent,
                Bundle options,
                Rect launchBounds) {
            ensureManageSystemUIPermission(mContext);
            ActivityOptions opt = ActivityOptions.fromBundle(options);
            // Need this for the pending intent to work under BAL hardening.
            opt.setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS);
            opt.setTaskAlwaysOnTop(true);
            mTaskViewTaskController.startActivity(
                    pendingIntent,
                    fillInIntent,
                    opt,
                    launchBounds);
        }

        @Override
        public void showEmbeddedTask() {
            ensureManageSystemUIPermission(mContext);
            ActivityManager.RunningTaskInfo taskInfo = mTaskViewTaskController.getTaskInfo();
            if (taskInfo == null) {
                return;
            }
            if (mTaskViewTaskController.isUsingShellTransitions() && mTaskViewTransitions != null) {
                mTaskViewTransitions.setTaskViewVisible(mTaskViewTaskController, /* visible= */
                        true, /* reorder= */ true);
                return;
            }

            WindowContainerTransaction wct = new WindowContainerTransaction();
            // Clears the hidden flag to make it TopFocusedRootTask: b/228092608
            wct.setHidden(taskInfo.token, /* hidden= */ false);
            // Moves the embedded task to the top to make it resumed: b/225388469
            wct.reorder(taskInfo.token, /* onTop= */ true);
            mShellTaskOrganizer.applyTransaction(wct);
        }

        @Override
        // TODO(b/24087642): Remove @Keep once this method is promoted to SystemApi.
        // @Keep is used to prevent the removal of this method by the compiler as it is a hidden api
        // in the base class.
        @Keep
        public void setTaskVisibility(boolean visibility) {
            ensureManageSystemUIPermission(mContext);
            ActivityManager.RunningTaskInfo taskInfo = mTaskViewTaskController.getTaskInfo();
            if (taskInfo == null) {
                return;
            }
            if (mTaskViewTaskController.isUsingShellTransitions()) {
                mTaskViewTransitions.setTaskViewVisible(mTaskViewTaskController, visibility);
                return;
            }

            WindowContainerTransaction wct = new WindowContainerTransaction();
            wct.setHidden(taskInfo.token, !visibility);
            mShellTaskOrganizer.applyTransaction(wct);
        }

        @Override
        // TODO(b/24087642): Remove @Keep once this method is promoted to SystemApi.
        // @Keep is used to prevent the removal of this method by the compiler as it is a hidden api
        // in the base class.
        @Keep
        public void reorderTask(boolean onTop) {
            ensureManageSystemUIPermission(mContext);
            ActivityManager.RunningTaskInfo taskInfo = mTaskViewTaskController.getTaskInfo();
            if (taskInfo == null) {
                return;
            }

            if (mTaskViewTaskController.isUsingShellTransitions()) {
                mTaskViewTransitions.reorderTaskViewTask(mTaskViewTaskController, onTop);
                return;
            }

            WindowContainerTransaction wct = new WindowContainerTransaction();
            wct.reorder(taskInfo.token, onTop);
            mShellTaskOrganizer.applyTransaction(wct);
        }

        @Override
        public void addInsets(int index, int type, @NonNull Rect frame) {
            ensureManageSystemUIPermission(mContext);
            mInsets.append(InsetsSource.createId(mInsetsOwner, index, type), frame);

            if (mTaskViewTaskController.getTaskInfo() == null) {
                // The insets will be applied later as part of onTaskAppeared.
                Log.w(TAG, "Cannot apply insets as the task token is not present.");
                return;
            }
            WindowContainerTransaction wct = new WindowContainerTransaction();
            wct.addInsetsSource(mTaskViewTaskController.getTaskInfo().token,
                    mInsetsOwner, index, type, frame, /* boundingRects = */ null, /* flags = */ 0);
            mShellTaskOrganizer.applyTransaction(wct);
        }

        @Override
        public void removeInsets(int index, int type) {
            ensureManageSystemUIPermission(mContext);
            if (mInsets.size() == 0) {
                Log.w(TAG, "No insets set.");
                return;
            }
            int id = InsetsSource.createId(mInsetsOwner, index, type);
            if (!mInsets.contains(id)) {
                Log.w(TAG, "Insets type: " + type + " can't be removed as it was not "
                        + "applied as part of the last addInsets()");
                return;
            }
            mInsets.remove(id);

            if (mTaskViewTaskController.getTaskInfo() == null) {
                Log.w(TAG, "Cannot remove insets as the task token is not present.");
                return;
            }
            WindowContainerTransaction wct = new WindowContainerTransaction();
            wct.removeInsetsSource(mTaskViewTaskController.getTaskInfo().token,
                    mInsetsOwner, index, type);
            mShellTaskOrganizer.applyTransaction(wct);
        }
    };

    public RemoteCarTaskViewServerImpl(
            Context context,
            ShellTaskOrganizer organizer,
            SyncTransactionQueue syncQueue,
            CarTaskViewClient carTaskViewClient,
            CarSystemUIProxyImpl carSystemUIProxy,
            TaskViewTransitions taskViewTransitions
    ) {
        mContext = context;
        mCarTaskViewClient = carTaskViewClient;
        mCarSystemUIProxy = carSystemUIProxy;
        mShellTaskOrganizer = organizer;
        mTaskViewTransitions = taskViewTransitions;

        mTaskViewTaskController =
                new TaskViewTaskController(context, organizer, taskViewTransitions, syncQueue);
        mTaskViewTaskController.setTaskViewBase(this);
    }

    public CarTaskViewHost getHostImpl() {
        return mHostImpl;
    }

    @Override
    public Rect getCurrentBoundsOnScreen() {
        try {
            return mCarTaskViewClient.getCurrentBoundsOnScreen();
        } catch (DeadSystemRuntimeException ex) {
            Log.w(TAG, "Failed to call getCurrentBoundsOnScreen() as TaskView client has "
                    + "already died. Host part will be released shortly.");
        }
        return new Rect(0, 0, 0, 0); // If it reaches here, it means that
        // the host side is already being released so it doesn't matter what is returned from here.
    }

    @Override
    public String toString() {
        ActivityManager.RunningTaskInfo taskInfo = mTaskViewTaskController.getTaskInfo();
        return "RemoteCarTaskViewServerImpl {"
                + "insets=" + mInsets
                + ", taskId=" + (taskInfo == null ? "null" : taskInfo.taskId)
                + ", taskInfo=" + (taskInfo == null ? "null" : taskInfo)
                + "}";
    }

    @Override
    public void setResizeBgColor(SurfaceControl.Transaction transaction, int color) {
        try {
            mCarTaskViewClient.setResizeBackgroundColor(transaction, color);
        } catch (DeadSystemRuntimeException e) {
            Log.w(TAG, "Failed to call setResizeBackgroundColor() as TaskView client has "
                    + "already died. Host part will be released shortly.");
        }
    }

    @Override
    public void onTaskAppeared(ActivityManager.RunningTaskInfo taskInfo, SurfaceControl leash) {
        applyAllInsets();
        try {
            mCarTaskViewClient.onTaskAppeared(taskInfo, leash);
        } catch (DeadSystemRuntimeException e) {
            Log.w(TAG, "Failed to call onTaskAppeared() as TaskView client has already died, "
                    + "already died. Host part will be released shortly.");
        }
    }

    @Override
    public void onTaskInfoChanged(ActivityManager.RunningTaskInfo taskInfo) {
        try {
            mCarTaskViewClient.onTaskInfoChanged(taskInfo);
        } catch (DeadSystemRuntimeException e) {
            Log.w(TAG, "Failed to call onTaskInfoChanged() as TaskView client has already died, "
                    + "already died. Host part will be released shortly.");
        }
    }

    @Override
    public void onTaskVanished(ActivityManager.RunningTaskInfo taskInfo) {
        try {
            mCarTaskViewClient.onTaskVanished(taskInfo);
        } catch (DeadSystemRuntimeException e) {
            Log.w(TAG, "Failed to call onTaskVanished() as TaskView client has already died, "
                    + "already died. Host part will be released shortly.");
        }
    }

    private void applyAllInsets() {
        if (mInsets.size() == 0) {
            Log.w(TAG, "Cannot apply null or empty insets");
            return;
        }
        if (mTaskViewTaskController.getTaskInfo() == null) {
            Log.w(TAG, "Cannot apply insets as the task token is not present.");
            return;
        }
        WindowContainerTransaction wct = new WindowContainerTransaction();
        for (int i = 0; i < mInsets.size(); i++) {
            final int id = mInsets.keyAt(i);
            final Rect frame = mInsets.valueAt(i);
            wct.addInsetsSource(mTaskViewTaskController.getTaskInfo().token,
                    mInsetsOwner, InsetsSource.getIndex(id), InsetsSource.getType(id), frame,
                    null /* boundingRects */, 0 /* flags */);
        }
        mShellTaskOrganizer.applyTransaction(wct);
    }

    /**
     * Gets the task info of the running task.
     */
    @Nullable
    ActivityManager.RunningTaskInfo getTaskInfo() {
        if (mTaskViewTaskController == null) {
            return null;
        }
        return mTaskViewTaskController.getTaskInfo();
    }
}
