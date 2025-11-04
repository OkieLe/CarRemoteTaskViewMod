package com.android.wm.shell.ext.system;

import static android.app.WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Process;
import android.util.ArraySet;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;

import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.common.SyncTransactionQueue;
import com.android.wm.shell.dagger.WMSingleton;
import com.android.wm.shell.ext.CarActivityManager;
import com.android.wm.shell.ext.CarActivityServiceProvider;
import com.android.wm.shell.ext.CarSystemUIProxy;
import com.android.wm.shell.ext.CarTaskViewClient;
import com.android.wm.shell.ext.CarTaskViewHost;
import com.android.wm.shell.ext.system.taskview.RemoteCarTaskViewServerImpl;
import com.android.wm.shell.taskview.TaskViewTransitions;

import java.util.List;

import javax.inject.Inject;

/**
 * This class provides a concrete implementation for {@link CarSystemUIProxy}. It hosts all the
 * system ui interaction that is required by other apps.
 */
@WMSingleton
public final class CarSystemUIProxyImpl
        implements CarSystemUIProxy, CarActivityServiceProvider.ServiceConnectedListener {
    private static final String TAG = CarSystemUIProxyImpl.class.getSimpleName();

    private final Context mContext;
    private final SyncTransactionQueue mSyncQueue;
    private final ShellTaskOrganizer mTaskOrganizer;
    private final TaskViewTransitions mTaskViewTransitions;
    private final ArraySet<RemoteCarTaskViewServerImpl> mRemoteCarTaskViewServerSet =
            new ArraySet<>();
    private final DisplayManager mDisplayManager;

    private boolean mConnected;
    private CarActivityManager mCarActivityManager;

    /**
     * Returns true if {@link CarSystemUIProxyImpl} should be registered, false otherwise.
     * This could be false because of reasons like:
     * <ul>
     *     <li>Current user is not a system user.</li>
     *     <li>Or {@code config_registerCarSystemUIProxy} is disabled.</li>
     * </ul>
     */
    public static boolean shouldRegisterCarSystemUIProxy(Context context) {
        if (!Process.myUserHandle().isSystem()) {
            Log.i(TAG, "Non system user.");
            return false;
        }
        return true;
    }

    @Inject
    CarSystemUIProxyImpl(
            Context context,
            CarActivityServiceProvider carServiceProvider,
            SyncTransactionQueue syncTransactionQueue,
            ShellTaskOrganizer taskOrganizer,
            TaskViewTransitions taskViewTransitions) {
        mContext = context;
        mTaskOrganizer = taskOrganizer;
        mSyncQueue = syncTransactionQueue;
        mTaskViewTransitions = taskViewTransitions;
        mDisplayManager = mContext.getSystemService(DisplayManager.class);

        if (!shouldRegisterCarSystemUIProxy(mContext)) {
            Log.i(TAG, "Not registering CarSystemUIProxy.");
            return;
        }
        carServiceProvider.addListener(this);
    }

    /** Returns the list of all the task views. */
    public ArraySet<RemoteCarTaskViewServerImpl> getAllTaskViews() {
        return mRemoteCarTaskViewServerSet;
    }

    @NonNull
    @Override
    public CarTaskViewHost createControlledCarTaskView(@NonNull CarTaskViewClient carTaskViewClient) {
        return createCarTaskView(carTaskViewClient);
    }

    @NonNull
    @Override
    public CarTaskViewHost createCarTaskView(@NonNull CarTaskViewClient carTaskViewClient) {
        ensureManageSystemUIPermission(mContext);
        RemoteCarTaskViewServerImpl remoteCarTaskViewServerImpl =
                new RemoteCarTaskViewServerImpl(
                        mContext,
                        mTaskOrganizer,
                        mSyncQueue,
                        carTaskViewClient,
                        this,
                        mTaskViewTransitions);
        mRemoteCarTaskViewServerSet.add(remoteCarTaskViewServerImpl);
        return remoteCarTaskViewServerImpl.getHostImpl();
    }

    /** Clears the taskview from the internal state. */
    public void onCarTaskViewReleased(RemoteCarTaskViewServerImpl remoteCarTaskViewServer) {
        mRemoteCarTaskViewServerSet.remove(remoteCarTaskViewServer);
    }

    @Override
    public void onConnected(CarActivityManager manager) {
        mConnected = true;
        removeExistingTaskViewTasks();

        mCarActivityManager = manager;
        mCarActivityManager.registerTaskMonitor();
        mCarActivityManager.registerCarSystemUIProxy(this);
    }

    @Override
    public void onDisconnected() {
        mConnected = false;
        mCarActivityManager = null;
    }

    private void removeExistingTaskViewTasks() {
        Display[] displays = mDisplayManager.getDisplays();
        for (int i = 0; i < displays.length; i++) {
            List<ActivityManager.RunningTaskInfo> taskInfos =
                    mTaskOrganizer.getRunningTasks(displays[i].getDisplayId());
            removeMultiWindowTasks(taskInfos);
        }
    }

    private static void removeMultiWindowTasks(List<ActivityManager.RunningTaskInfo> taskInfos) {
        ActivityTaskManager atm = ActivityTaskManager.getInstance();
        for (ActivityManager.RunningTaskInfo taskInfo : taskInfos) {
            // In Auto, only TaskView tasks have WINDOWING_MODE_MULTI_WINDOW as of now.
            if (taskInfo.getWindowingMode() == WINDOWING_MODE_MULTI_WINDOW) {
                Log.d(TAG, "Found a dangling task, removing: " + taskInfo.taskId);
                atm.removeTask(taskInfo.taskId);
            }
        }
    }

    /**
     * Checks the permission of the calling process. Throws {@link SecurityException} if
     * {Car#PERMISSION_MANAGE_CAR_SYSTEM_UI} is not granted.
     */
    public static void ensureManageSystemUIPermission(Context context) {
        if (Binder.getCallingPid() == Process.myPid()) {
            // If called from within SystemUI, allow.
            return;
        }
        // No op here
        Log.i(TAG, "Checking permission of " + Binder.getCallingPid());
    }
}
