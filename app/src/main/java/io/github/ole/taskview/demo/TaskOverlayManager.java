package io.github.ole.taskview.demo;

import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.android.launcher3.Launcher;
import com.android.systemui.plugins.shared.LauncherOverlayManager;
import com.android.wm.shell.ext.RemoteCarTaskView;

import java.io.PrintWriter;

public class TaskOverlayManager implements LauncherOverlayManager {
    private static final String TAG = "TaskOverlayManager";
    private static final int OVERLAY_HIDDEN_X = -1080; // screen width

    private final Launcher mLauncher;
    private final TaskViewController mTaskViewController;

    private final FrameLayout mOverlayContainer;
    private final WindowManager.LayoutParams mOverlayLayoutParams;
    private final WindowManager mWindowManager;

    private boolean mIsOverlayAttached = false;
    private boolean mIsOverlayVisible = false;

    private final View.OnAttachStateChangeListener mViewAttachListener = new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(@NonNull View v) {
            if (v instanceof RemoteCarTaskView) {
                Log.i(TAG, "task view added");
            } else {
                Log.i(TAG, "task container added");
                mIsOverlayAttached = true;
            }
        }
        @Override
        public void onViewDetachedFromWindow(@NonNull View v) {
            if (v instanceof RemoteCarTaskView) {
                Log.i(TAG, "task view removed");
            } else {
                Log.i(TAG, "task container removed");
                mIsOverlayAttached = false;
            }
        }
    };

    public TaskOverlayManager(Launcher activity) {
        mLauncher = activity;
        mLauncher.getWindow().addPrivateFlags(
                WindowManager.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY);
        mTaskViewController = new TaskViewController.Factory(mLauncher,
                mLauncher.getTaskLaunchIntent()).create();
        mLauncher.setLauncherOverlay(new TaskOverlayTouchProxy());
        mOverlayContainer = (FrameLayout) LayoutInflater.from(mLauncher)
                .inflate(mLauncher.getContainerLayout(), null, false);
        mOverlayLayoutParams = new WindowManager.LayoutParams(
                1080, // screen width
                2240, // screen height
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        mOverlayLayoutParams.token = mLauncher.getWindow().getAttributes().token;
        mOverlayLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mOverlayLayoutParams.x = OVERLAY_HIDDEN_X;
        mOverlayLayoutParams.y = 0;
        mWindowManager = mLauncher.getSystemService(WindowManager.class);
        mOverlayContainer.addOnAttachStateChangeListener(mViewAttachListener);
    }

    @Override
    public void onDeviceProvideChanged() {
        Log.d(TAG, "onDeviceProvideChanged");
    }

    @Override
    public void onAttachedToWindow() {
        Log.d(TAG, "onAttachedToWindow");
        mOverlayContainer.setOnClickListener(v -> {
            addTaskView();
        });
        mWindowManager.addView(mOverlayContainer, mOverlayLayoutParams);
    }

    private void addTaskView() {
        if (mOverlayContainer.getChildAt(0) instanceof RemoteCarTaskView) {
            Log.i(TAG, "TaskView already added " + mTaskViewController.getRemoteCarTaskViewTaskId());
            return;
        }
        mTaskViewController.getRemoteCarTaskView(taskView -> {
            Log.i(TAG, "TaskView is " + taskView);
            View child = mOverlayContainer.getChildAt(0);
            if (child instanceof RemoteCarTaskView) {
                ((RemoteCarTaskView) child).release();
                mOverlayContainer.removeAllViews();
            }
            if (taskView != null) {
                taskView.addOnAttachStateChangeListener(mViewAttachListener);
                mOverlayContainer.post(() -> {
                    mOverlayContainer.addView(taskView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                });
            }
        });
    }

    @Override
    public void onDetachedFromWindow() {
        mWindowManager.removeView(mOverlayContainer);
        mOverlayContainer.removeOnAttachStateChangeListener(mViewAttachListener);
        Log.d(TAG, "onDetachedFromWindow");
    }

    @Override
    public void dump(String prefix, PrintWriter w) {
        w.println(prefix + "TaskLauncherOverlay");
        w.println(prefix + " mIsOverlayVisible: " + mIsOverlayVisible);
        w.println(prefix + " mOverlayLayoutParams: " + mOverlayLayoutParams);
    }

    @Override
    public void openOverlay() {
        if (mIsOverlayVisible) {
            return;
        }
        Log.d(TAG, "openOverlay");
        mIsOverlayVisible = true;
        mOverlayLayoutParams.x = 0;
        mOverlayLayoutParams.y = 0;
        if (!mIsOverlayAttached) {
            return;
        }
        mTaskViewController.onResume();
        mWindowManager.updateViewLayout(mOverlayContainer, mOverlayLayoutParams);
        mOverlayContainer.post(mTaskViewController::onViewMoved);
    }

    @Override
    public void hideOverlay(boolean animate) {
        Log.d(TAG, "hideOverlay");
        hideOverlay(animate ? 200 : 0);
    }

    @Override
    public void hideOverlay(int duration) {
        if (!mIsOverlayVisible) {
            return;
        }
        Log.d(TAG, "hideOverlay");
        mOverlayLayoutParams.x = OVERLAY_HIDDEN_X;
        mOverlayLayoutParams.y = 0;
        mIsOverlayVisible = false;
        if (!mIsOverlayAttached) {
            return;
        }
        mWindowManager.updateViewLayout(mOverlayContainer, mOverlayLayoutParams);
        mOverlayContainer.post(() -> {
            mTaskViewController.onViewMoved();
            mTaskViewController.onPause();
        });
    }

    @Override
    public void onActivityStarted() {
        Log.d(TAG, "onActivityStarted");
    }

    @Override
    public void onActivityResumed() {
        Log.d(TAG, "onActivityResumed");
    }

    @Override
    public void onActivityPaused() {
        Log.d(TAG, "onActivityPaused");
    }

    @Override
    public void onActivityStopped() {
        Log.d(TAG, "onActivityStopped");
        mTaskViewController.onStop();
    }

    @Override
    public void onActivityDestroyed() {
        Log.d(TAG, "onActivityDestroyed");
        mTaskViewController.onDestroy();
    }

    @Override
    public void onDisallowSwipeToMinusOnePage() {
        Log.d(TAG, "onDisallowSwipeToMinusOnePage");
    }

    private class TaskOverlayTouchProxy implements LauncherOverlayManager.LauncherOverlayTouchProxy {

        @Override
        public void onFlingVelocity(float velocity) {
            Log.d(TAG, "onFlingVelocity " + velocity);
            if (mIsOverlayVisible && velocity < -5000) {
                hideOverlay(0);
            }
        }

        @Override
        public void onOverlayMotionEvent(MotionEvent ev, float scrollProgress) {
            Log.d(TAG, "onOverlayMotionEvent " + ev.getAction() + " " + scrollProgress);
            if (!mIsOverlayVisible && ev.getAction() == MotionEvent.ACTION_UP && scrollProgress > 0.5f) {
                openOverlay();
            }
        }
    }
}
