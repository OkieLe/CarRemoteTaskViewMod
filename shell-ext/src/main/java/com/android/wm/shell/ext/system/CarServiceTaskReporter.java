package com.android.wm.shell.ext.system;

import static com.android.wm.shell.ext.system.CarFullscreenTaskMonitorListener.DBG;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceControl;

import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.ext.CarActivityManager;
import com.android.wm.shell.ext.CarActivityServiceProvider;
import com.android.wm.shell.taskview.TaskViewTransitions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class reports the task events to CarService using {@link CarActivityManager}.
 */
final class CarServiceTaskReporter implements CarActivityServiceProvider.ServiceConnectedListener {
    private static final String TAG = "CarServiceTaskReporter";
    private final DisplayManager mDisplayManager;
    private final AtomicReference<CarActivityManager> mCarActivityManagerRef =
            new AtomicReference<>();
    private final boolean mShouldConnectToCarActivityService;
    private final TaskViewTransitions mTaskViewTransitions;
    private final ShellTaskOrganizer mShellTaskOrganizer;

    CarServiceTaskReporter(Context context, CarActivityServiceProvider carServiceProvider,
                           TaskViewTransitions taskViewTransitions,
                           ShellTaskOrganizer shellTaskOrganizer) {
        mDisplayManager = context.getSystemService(DisplayManager.class);
        mTaskViewTransitions = taskViewTransitions;
        // Rely on whether or not CarSystemUIProxy should be registered to account for these
        // cases:
        // 1. Legacy system where System UI + launcher both register a TaskOrganizer.
        //    CarFullScreenTaskMonitorListener will not forward the task lifecycle to the car
        //    service, as launcher has its own FullScreenTaskMonitorListener.
        // 2. MUMD system where only System UI registers a TaskOrganizer but the user associated
        //    with the current display is not a system user. CarSystemUIProxy will be registered
        //    for system user alone and hence CarFullScreenTaskMonitorListener should be
        //    registered only then.
        mShouldConnectToCarActivityService = CarSystemUIProxyImpl.shouldRegisterCarSystemUIProxy(
                context);
        mShellTaskOrganizer = shellTaskOrganizer;

        if (mShouldConnectToCarActivityService) {
            carServiceProvider.addListener(this);
        }
    }

    public void reportTaskAppeared(ActivityManager.RunningTaskInfo taskInfo, SurfaceControl leash) {
        if (!mShouldConnectToCarActivityService) {
            if (DBG) {
                Log.w(TAG, "onTaskAppeared() handled in SystemUI as conditions not met for "
                        + "connecting to car service.");
            }
            return;
        }

        if (mTaskViewTransitions.isTaskViewTask(taskInfo)) {
            if (DBG) {
                Log.w(TAG, "not reporting onTaskAppeared for taskview task = " + taskInfo.taskId);
            }
            return;
        }
        CarActivityManager carAM = mCarActivityManagerRef.get();
        if (carAM != null) {
            carAM.onTaskAppeared(taskInfo, leash);
        } else {
            Log.w(TAG, "CarActivityManager is null, skip onTaskAppeared: taskInfo=" + taskInfo);
        }
    }

    public void reportTaskInfoChanged(ActivityManager.RunningTaskInfo taskInfo) {
        if (!mShouldConnectToCarActivityService) {
            if (DBG) {
                Log.w(TAG, "onTaskInfoChanged() handled in SystemUI as conditions not met for "
                        + "connecting to car service.");
            }
            return;
        }

        if (mTaskViewTransitions.isTaskViewTask(taskInfo)) {
            if (DBG) {
                Log.w(TAG,
                        "not reporting onTaskInfoChanged for taskview task = " + taskInfo.taskId);
            }
            return;
        }

        CarActivityManager carAM = mCarActivityManagerRef.get();
        if (carAM != null) {
            carAM.onTaskInfoChanged(taskInfo);
        } else {
            Log.w(TAG, "CarActivityManager is null, skip onTaskInfoChanged: taskInfo=" + taskInfo);
        }
    }

    public void reportTaskVanished(ActivityManager.RunningTaskInfo taskInfo) {
        if (!mShouldConnectToCarActivityService) {
            if (DBG) {
                Log.w(TAG, "onTaskVanished() handled in SystemUI as conditions not met for "
                        + "connecting to car service.");
            }
            return;
        }

        if (mTaskViewTransitions.isTaskViewTask(taskInfo)) {
            if (DBG) {
                Log.w(TAG, "not reporting onTaskVanished for taskview task = " + taskInfo.taskId);
            }
            return;
        }

        CarActivityManager carAM = mCarActivityManagerRef.get();
        if (carAM != null) {
            carAM.onTaskVanished(taskInfo);
        } else {
            Log.w(TAG, "CarActivityManager is null, skip onTaskVanished: taskInfo=" + taskInfo);
        }
    }

    @Override
    public void onConnected(CarActivityManager manager) {
        mCarActivityManagerRef.set(manager);
        // The tasks that have already appeared need to be reported to the CarActivityManager.
        // The code uses null as the leash because there is no way to get the leash at the moment.
        // And the leash is only required for mirroring cases. Those tasks will anyway appear
        // after the car service is connected and hence will go via the {@link #onTaskAppeared}
        // flow.
        List<ActivityManager.RunningTaskInfo> runningTasks = getRunningNonTaskViewTasks();
        for (ActivityManager.RunningTaskInfo runningTaskInfo : runningTasks) {
            Log.d(TAG, "Sending onTaskAppeared for an already existing task: "
                    + runningTaskInfo.taskId);
            mCarActivityManagerRef.get().onTaskAppeared(runningTaskInfo, /* leash = */ null);
        }
    }

    @Override
    public void onDisconnected() {
        mCarActivityManagerRef.set(null);
    }

    private List<ActivityManager.RunningTaskInfo> getRunningNonTaskViewTasks() {
        Display[] displays = mDisplayManager.getDisplays();
        List<ActivityManager.RunningTaskInfo> tasksToReturn = new ArrayList<>();
        for (int i = 0; i < displays.length; i++) {
            List<ActivityManager.RunningTaskInfo> taskInfos = mShellTaskOrganizer.getRunningTasks(
                    displays[i].getDisplayId());
            for (ActivityManager.RunningTaskInfo taskInfo : taskInfos) {
                if (!mTaskViewTransitions.isTaskViewTask(taskInfo)) {
                    tasksToReturn.add(taskInfo);
                }
            }
        }
        return tasksToReturn;
    }
}
