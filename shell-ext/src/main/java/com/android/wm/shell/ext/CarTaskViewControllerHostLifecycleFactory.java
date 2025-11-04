package com.android.wm.shell.ext;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.wm.shell.ext.utils.ActivityManagerHelper;

/**
 * A factory to create instances of the {@link CarTaskViewControllerHostLifecycle}.
 */
public final class CarTaskViewControllerHostLifecycleFactory {
    private CarTaskViewControllerHostLifecycleFactory() {
    }

    /**
     * Creates an instance of {@link CarTaskViewControllerHostLifecycle} which adapts to the
     * activity lifecycle.
     *
     * @param activity the activity which the {@link CarTaskViewControllerHostLifecycle} needs to
     *                 be created for.
     */
    @NonNull
    public static CarTaskViewControllerHostLifecycle forActivity(@NonNull Activity activity) {
        return new CarTaskViewControllerHostActivityLifecycleAdapter(activity).getLifecycle();
    }

    private static class CarTaskViewControllerHostActivityLifecycleAdapter
            implements Application.ActivityLifecycleCallbacks {

        CarTaskViewControllerHostLifecycle mCarTaskViewControllerHostLifecycle;

        CarTaskViewControllerHostActivityLifecycleAdapter(Activity activity) {
            mCarTaskViewControllerHostLifecycle = new CarTaskViewControllerHostLifecycle();
            activity.registerActivityLifecycleCallbacks(this);
            // If the activity is already in resumed state, trigger the host appeared callback
            // so that the visibility information is latest.
            if (ActivityManagerHelper.isVisible(activity)) {
                mCarTaskViewControllerHostLifecycle.hostAppeared();
            }
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity,
                                      @Nullable Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            // Don't invoke hostAppeared() in onStart(), which breaks the CTS
            // ActivityLifecycleTests#testFinishBelowDialogActivity.
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            mCarTaskViewControllerHostLifecycle.hostAppeared();
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            mCarTaskViewControllerHostLifecycle.hostDisappeared();
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity,
                                                @NonNull Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            mCarTaskViewControllerHostLifecycle.hostDestroyed();
        }

        public CarTaskViewControllerHostLifecycle getLifecycle() {
            return mCarTaskViewControllerHostLifecycle;
        }
    }
}
