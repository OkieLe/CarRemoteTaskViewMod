package io.github.ole.taskview.demo;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.UiContext;

import com.android.wm.shell.ext.RemoteCarTaskView;

import java.util.Objects;
import java.util.function.Consumer;

public interface TaskViewController {

    /**
     * Observe changes of remote car task view
     * @param consumer to receive latest RemoteCarTaskView
     */
    void getRemoteCarTaskView(Consumer<RemoteCarTaskView> consumer);

    /**
     * Get remote car task view task Id.
     * @return the task id in task view
     */
    int getRemoteCarTaskViewTaskId();

    /**
     * Called when task view is bound to host
     */
    void onStart();

    /**
     * Called when position task view is changed
     */
    void onViewMoved();

    /**
     * Called when task view is unbound from host
     */
    void onStop();

    /**
     * Called when task view should be released
     */
    void onDestroy();

    final class Factory {
        private final Context mContext;
        private final Intent mLaunchIntent;

        public Factory(@UiContext Context context, @NonNull Intent intent) {
            mLaunchIntent = Objects.requireNonNull(intent);
            mContext = Objects.requireNonNull(context);
        }

        @NonNull
        public TaskViewController create() {
            return new TaskViewControllerImpl(mContext, mLaunchIntent);
        }
    }
}
