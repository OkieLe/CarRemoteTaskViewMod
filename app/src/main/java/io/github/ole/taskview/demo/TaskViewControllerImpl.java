package io.github.ole.taskview.demo;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_STARTING;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.UiContext;

import com.android.wm.shell.ext.CarActivityManager;
import com.android.wm.shell.ext.CarActivityServiceProvider;
import com.android.wm.shell.ext.CarTaskViewController;
import com.android.wm.shell.ext.CarTaskViewControllerCallback;
import com.android.wm.shell.ext.CarTaskViewControllerHostLifecycle;
import com.android.wm.shell.ext.ControlledRemoteCarTaskView;
import com.android.wm.shell.ext.ControlledRemoteCarTaskViewCallback;
import com.android.wm.shell.ext.ControlledRemoteCarTaskViewConfig;
import com.android.wm.shell.ext.RemoteCarTaskView;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A car launcher view model to manage the lifecycle of {RemoteCarTaskView}.
 */
public final class TaskViewControllerImpl implements TaskViewController,
        CarActivityServiceProvider.ServiceConnectedListener {
    private static final String TAG = "TaskViewController";
    private static final boolean DEBUG = true;
    private static final boolean sAutoRestartOnCrash = false;

    private final CarActivityServiceProvider mServiceProvider;

    @SuppressLint("StaticFieldLeak") // We're not leaking this context as it is the window context.
    private final Context mWindowContext;
    private final Handler mHandler;
    private final Intent mLaunchIntent;

    private CarActivityManager mCarActivityManager;
    private final CarTaskViewControllerHostLifecycle mHostLifecycle;
    private RemoteCarTaskView mRemoteCarTaskView = null;
    private Consumer<RemoteCarTaskView> mTaskViewConsumer;

    public TaskViewControllerImpl(@UiContext Context context, Intent intent) {
        mWindowContext = context.createWindowContext(TYPE_APPLICATION_STARTING, /* options */ null);
        mLaunchIntent = intent;

        HandlerThread handlerThread = new HandlerThread("car_task_view");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        mServiceProvider = new CarActivityServiceProvider(context, mHandler);
        mServiceProvider.addListener(this);
        mHostLifecycle = new CarTaskViewControllerHostLifecycle();
    }

    @Override
    public void onConnected(CarActivityManager manager) {
        mCarActivityManager = manager;
        initializeRemoteCarTaskView();
    }

    @Override
    public void onDisconnected() {
        mCarActivityManager = null;
    }

    /**
     * Initialize the remote car task view with the maps intent.
     */
    public void initializeRemoteCarTaskView() {
        if (DEBUG) {
            Log.d(TAG, "Intent in the task view " + mLaunchIntent.getComponent());
        }
        if (mRemoteCarTaskView != null) {
            // Release the remote car task view instance if it exists since otherwise there could
            // be a memory leak
            mRemoteCarTaskView.release();
        }
        ControlledRemoteCarTaskViewCallback controlledRemoteCarTaskViewCallback =
                new ControlledRemoteCarTaskViewCallbackImpl();

        CarTaskViewControllerCallback carTaskViewControllerCallback =
                new CarTaskViewControllerCallbackImpl(controlledRemoteCarTaskViewCallback);

        mCarActivityManager.getCarTaskViewController(mWindowContext, mHostLifecycle,
                mWindowContext.getMainExecutor(), carTaskViewControllerCallback);
    }

    @Override
    public void getRemoteCarTaskView(Consumer<RemoteCarTaskView> consumer) {
        Objects.requireNonNull(consumer);
        mTaskViewConsumer = consumer;
        mTaskViewConsumer.accept(mRemoteCarTaskView);
    }

    @Override
    public int getRemoteCarTaskViewTaskId() {
        if (mRemoteCarTaskView != null && mRemoteCarTaskView.getTaskInfo() != null) {
            return mRemoteCarTaskView.getTaskInfo().taskId;
        }
        return INVALID_TASK_ID;
    }

    @Override
    public void onStart() {
        // Do not trigger 'hostAppeared()' in onResume.
        // If the host Activity was hidden by an Activity, the Activity is moved to the other
        // display, what the system expects would be the new moved Activity becomes the top one.
        // But, at the time, the host Activity became visible and 'onResume()' is triggered.
        // If 'hostAppeared()' is called in onResume, which moves the embeddedTask to the top and
        // breaks the contract (the newly moved Activity becomes top).
        // The contract is maintained by android.server.wm.multidisplay.MultiDisplayClientTests.
        // BTW, if we don't invoke 'hostAppeared()', which makes the embedded task invisible if
        // the host Activity gets the new Intent, so we'd call 'hostAppeared()' in onNewIntent.
        mHostLifecycle.hostAppeared();
    }

    @Override
    public void onResume() {
        if (getRemoteCarTaskViewTaskId() != INVALID_TASK_ID) {
            Log.d(TAG, "Resume task view with task " + getRemoteCarTaskViewTaskId());
            mRemoteCarTaskView.setSurfaceCreatedDeferred(false);
        }
    }

    @Override
    public void onPause() {
        if (getRemoteCarTaskViewTaskId() != INVALID_TASK_ID) {
            Log.d(TAG, "Pause task view with task " + getRemoteCarTaskViewTaskId());
            mRemoteCarTaskView.setSurfaceCreatedDeferred(true);
        }
    }

    @Override
    public void onViewMoved() {
        if (getRemoteCarTaskViewTaskId() != INVALID_TASK_ID) {
            Log.d(TAG, "Task view moved, update task bounds");
            mRemoteCarTaskView.updateWindowBounds();
        }
    }

    @Override
    public void onStop() {
        mHostLifecycle.hostDisappeared();
    }

    @Override
    public void onDestroy() {
        if (mRemoteCarTaskView != null) {
            mRemoteCarTaskView.release();
        }
        mServiceProvider.removeListener(this);
        mServiceProvider.release();
        mHostLifecycle.hostDestroyed();
    }

    private final class ControlledRemoteCarTaskViewCallbackImpl implements
            ControlledRemoteCarTaskViewCallback {

        @Override
        public void onTaskViewCreated(@NonNull ControlledRemoteCarTaskView taskView) {
            if (DEBUG) {
                Log.d(TAG, "LauncherTaskView: onTaskViewCreated");
            }
            taskView.setSurfaceLifecycle(SurfaceView.SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT);
            mRemoteCarTaskView = taskView;
            if (mTaskViewConsumer != null) {
                mTaskViewConsumer.accept(mRemoteCarTaskView);
            }
        }

        @Override
        public void onTaskViewInitialized() {
            if (DEBUG) {
                Log.d(TAG, "LauncherTaskView: onTaskViewInitialized");
            }
        }

        @Override
        public void onTaskAppeared(@NonNull ActivityManager.RunningTaskInfo taskInfo) {
            if (DEBUG) {
                Log.d(TAG, "LauncherTaskView: onTaskAppeared: taskId=" + taskInfo.taskId);
            }
            if (!sAutoRestartOnCrash) {
                mRemoteCarTaskView.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        @Override
        public void onTaskVanished(@NonNull ActivityManager.RunningTaskInfo taskInfo) {
            if (DEBUG) {
                Log.d(TAG, "LauncherTaskView: onTaskVanished: taskId=" + taskInfo.taskId);
            }
            if (!sAutoRestartOnCrash) {
                // RemoteCarTaskView color is set to red to indicate
                // that nothing is wrong with the task view but maps
                // in the task view has crashed. More details in
                // b/247156851.
                mRemoteCarTaskView.setBackgroundColor(Color.RED);
            }
        }
    }

    private final class CarTaskViewControllerCallbackImpl implements CarTaskViewControllerCallback {
        private final ControlledRemoteCarTaskViewCallback mControlledRemoteCarTaskViewCallback;

        private CarTaskViewControllerCallbackImpl(
                ControlledRemoteCarTaskViewCallback controlledRemoteCarTaskViewCallback) {
            mControlledRemoteCarTaskViewCallback = controlledRemoteCarTaskViewCallback;
        }

        @Override
        public void onConnected(@NonNull CarTaskViewController carTaskViewController) {
            carTaskViewController.createControlledRemoteCarTaskView(
                    new ControlledRemoteCarTaskViewConfig.Builder()
                            .setActivityIntent(mLaunchIntent)
                            .setShouldAutoRestartOnTaskRemoval(sAutoRestartOnCrash)
                            .build(),
                    mWindowContext.getMainExecutor(),
                    mControlledRemoteCarTaskViewCallback);
        }

        @Override
        public void onDisconnected(@NonNull CarTaskViewController carTaskViewController) {
            if (mTaskViewConsumer != null) {
                mTaskViewConsumer.accept(null);
            }
            mRemoteCarTaskView = null;
        }
    }
}
