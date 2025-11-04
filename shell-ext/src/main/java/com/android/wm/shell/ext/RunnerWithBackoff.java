package com.android.wm.shell.ext;

import static com.android.wm.shell.ext.CarTaskViewController.DBG;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** A wrapper class for {@link Runnable} which retries in an exponential backoff manner. */
final class RunnerWithBackoff {
    private static final String TAG = RunnerWithBackoff.class.getSimpleName();
    private static final int MAXIMUM_ATTEMPTS = 5;
    private static final int FIRST_BACKOFF_TIME_MS = 1_000; // 1 second
    private static final int MAXIMUM_BACKOFF_TIME_MS = 8_000; // 8 seconds
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mAction;

    private int mBackoffTimeMs;
    private int mAttempts;

    RunnerWithBackoff(Runnable action) {
        mAction = action;
    }

    private final Runnable mRetryRunnable = new Runnable() {
        @Override
        public void run() {
            if (mAttempts >= MAXIMUM_ATTEMPTS) {
                Log.e(TAG, "Failed to perform action, even after " + mAttempts + " attempts");
                return;
            }
            if (DBG) {
                Log.d(TAG, "Executing the action. Attempt number " + mAttempts);
            }
            mAction.run();

            mHandler.postDelayed(mRetryRunnable, mBackoffTimeMs);
            increaseBackoff();
            mAttempts++;
        }
    };

    private void increaseBackoff() {
        mBackoffTimeMs *= 2;
        if (mBackoffTimeMs > MAXIMUM_BACKOFF_TIME_MS) {
            mBackoffTimeMs = MAXIMUM_BACKOFF_TIME_MS;
        }
    }

    /** Starts the retrying. The first try happens synchronously. */
    public void start() {
        if (DBG) {
            Log.d(TAG, "start backoff runner");
        }
        // Stop the existing retrying as a safeguard to prevent multiple starts.
        stopInternal();

        mBackoffTimeMs = FIRST_BACKOFF_TIME_MS;
        mAttempts = 0;
        // Call .run() instead of posting to handler so that first try can happen synchronously.
        mRetryRunnable.run();
    }

    /** Stops the retrying. */
    public void stop() {
        if (DBG) {
            Log.d(TAG, "stop backoff runner");
        }
        stopInternal();
    }

    private void stopInternal() {
        mHandler.removeCallbacks(mRetryRunnable);
    }
}
