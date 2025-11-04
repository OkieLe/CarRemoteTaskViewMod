package io.github.ole.taskview.service;

import android.app.ActivityManager;
import android.app.TaskInfo;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.SurfaceControl;

import androidx.annotation.GuardedBy;

import com.android.wm.shell.ext.ICarActivityService;
import com.android.wm.shell.ext.ICarSystemUIProxy;
import com.android.wm.shell.ext.ICarSystemUIProxyCallback;
import com.android.wm.shell.ext.utils.TaskInfoHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Service responsible for Activities in Car.
 */
public final class CarActivityService extends ICarActivityService.Stub {

    private static final String TAG = "CarActivityService";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    private final Context mContext;

    private final Object mLock = new Object();

    // LinkedHashMap is used instead of SparseXXX because a predictable iteration order is needed.
    // The tasks here need be ordered as per their stack order. The stack order is maintained
    // using a combination of onTaskAppeared and onTaskInfoChanged callbacks.
    @GuardedBy("mLock")
    private final LinkedHashMap<Integer, ActivityManager.RunningTaskInfo> mTasks =
            new LinkedHashMap<>();
    @GuardedBy("mLock")
    private final SparseArray<SurfaceControl> mTaskToSurfaceMap = new SparseArray<>();

    @GuardedBy("mLock")
    private final ArrayMap<IBinder, IBinder.DeathRecipient> mMonitorTokens = new ArrayMap<>();

    @GuardedBy("mLock")
    private ICarSystemUIProxy mCarSystemUIProxy;
    @GuardedBy("mLock")
    private final RemoteCallbackList<ICarSystemUIProxyCallback> mCarSystemUIProxyCallbacks =
            new RemoteCallbackList<>();

    private IBinder mCurrentMonitor;

    public CarActivityService(Context context) {
        mContext = context;
    }

    @Override
    public void registerTaskMonitor(IBinder token) {
        if (DBG) Log.d(TAG, "registerTaskMonitor: " + token);

        IBinder.DeathRecipient deathRecipient = () -> cleanUpMonitorToken(token);
        synchronized (mLock) {
            try {
                token.linkToDeath(deathRecipient, /* flags= */ 0);
            } catch (RemoteException e) {
                // 'token' owner might be dead already.
                Log.e(TAG, "failed to linkToDeath: " + token);
                return;
            }
            mMonitorTokens.put(token, deathRecipient);
            mCurrentMonitor = token;
            // When new TaskOrganizer takes the control, it'll get the status of the whole tasks
            // in the system again. So drops the old status.
            mTasks.clear();
        }
    }

    private void cleanUpMonitorToken(IBinder token) {
        synchronized (mLock) {
            if (mCurrentMonitor == token) {
                mCurrentMonitor = null;
            }
            IBinder.DeathRecipient deathRecipient = mMonitorTokens.remove(token);
            if (deathRecipient != null) {
                token.unlinkToDeath(deathRecipient, /* flags= */ 0);
            }
        }
    }

    @Override
    public void onTaskAppeared(IBinder token,
                               ActivityManager.RunningTaskInfo taskInfo, SurfaceControl leash) {
        if (DBG) {
            Log.d(TAG, "onTaskAppeared: " + token + ", " + TaskInfoHelper.toString(taskInfo));
        }
        synchronized (mLock) {
            if (isDeniedToUpdateLocked(token)) {
                return;
            }
            mTasks.put(taskInfo.taskId, taskInfo);
            if (leash != null) {
                mTaskToSurfaceMap.put(taskInfo.taskId, leash);
            }
        }
    }

    @GuardedBy("mLock")
    private boolean isDeniedToUpdateLocked(IBinder token) {
        if (mCurrentMonitor != null && mCurrentMonitor == token) {
            return false;
        }
        // Fallback during no current Monitor exists.
        boolean allowed = (mCurrentMonitor == null && mMonitorTokens.containsKey(token));
        if (!allowed) {
            Log.w(TAG, "Report with the invalid token: " + token);
        }
        return !allowed;
    }

    @Override
    public void onTaskVanished(IBinder token, ActivityManager.RunningTaskInfo taskInfo) {
        if (DBG) {
            Log.d(TAG, "onTaskVanished: " + token + ", " + TaskInfoHelper.toString(taskInfo));
        }
        synchronized (mLock) {
            if (isDeniedToUpdateLocked(token)) {
                return;
            }
            // Do not remove the taskInfo from the mLastKnownDisplayIdForTask array since when
            // the task vanishes, the display ID becomes -1. We want to preserve this information
            // to finish the blocking ui for that display ID. mTasks and
            // mLastKnownDisplayIdForTask come in sync when the blocking ui is finished.
            mTasks.remove(taskInfo.taskId);
            mTaskToSurfaceMap.remove(taskInfo.taskId);
        }
    }

    @Override
    public void onTaskInfoChanged(IBinder token, ActivityManager.RunningTaskInfo taskInfo) {
        if (DBG) {
            Log.d(TAG, "onTaskInfoChanged: " + token + ", " + TaskInfoHelper.toString(taskInfo));
        }
        synchronized (mLock) {
            if (isDeniedToUpdateLocked(token)) {
                return;
            }
            // The key should be removed and added again so that it jumps to the front of the
            // LinkedHashMap.
            TaskInfo oldTaskInfo = mTasks.remove(taskInfo.taskId);
            mTasks.put(taskInfo.taskId, taskInfo);
        }
    }

    @Override
    public void unregisterTaskMonitor(IBinder token) {
        if (DBG) Log.d(TAG, "unregisterTaskMonitor: " + token);
        cleanUpMonitorToken(token);
    }

    /**
     * Returns all the visible tasks in the given display. The order is not guaranteed.
     */
    @Override
    public List<ActivityManager.RunningTaskInfo> getVisibleTasks(int displayId) {
        return getVisibleTasksInternal(displayId);
    }

    public List<ActivityManager.RunningTaskInfo> getVisibleTasksInternal() {
        return getVisibleTasksInternal(Display.INVALID_DISPLAY);
    }

    /** Car service internal version without the permission enforcement. */
    public List<ActivityManager.RunningTaskInfo> getVisibleTasksInternal(int displayId) {
        ArrayList<ActivityManager.RunningTaskInfo> tasksToReturn = new ArrayList<>();
        synchronized (mLock) {
            for (ActivityManager.RunningTaskInfo taskInfo : mTasks.values()) {
                // Activities launched in the private display or non-focusable display can't be
                // focusable. So we just monitor all visible Activities/Tasks.
                if (TaskInfoHelper.isVisible(taskInfo)
                        && (displayId == Display.INVALID_DISPLAY
                        || displayId == TaskInfoHelper.getDisplayId(taskInfo))) {
                    tasksToReturn.add(taskInfo);
                }
            }
        }
        // Reverse the order so that the resultant order is top to bottom.
        Collections.reverse(tasksToReturn);
        return tasksToReturn;
    }

    @Override
    public void registerCarSystemUIProxy(ICarSystemUIProxy carSystemUIProxy) {
        if (DBG) {
            Log.d(TAG, "registerCarSystemUIProxy " + carSystemUIProxy.toString());
        }
        synchronized (mLock) {
            if (mCarSystemUIProxy != null) {
                throw new UnsupportedOperationException("Car system UI proxy is already "
                        + "registered");
            }

            mCarSystemUIProxy = carSystemUIProxy;
            try {
                mCarSystemUIProxy.asBinder().linkToDeath(new IBinder.DeathRecipient(){
                    @Override
                    public void binderDied() {
                        synchronized (mLock) {
                            Log.d(TAG, "CarSystemUIProxy died " + mCarSystemUIProxy.toString());
                            mCarSystemUIProxy.asBinder().unlinkToDeath(this, /* flags= */ 0);
                            mCarSystemUIProxy = null;
                        }
                    }
                }, /* flags= */0);
            } catch (RemoteException remoteException) {
                mCarSystemUIProxy = null;
                throw new IllegalStateException("Linking to binder death failed for "
                        + "ICarSystemUIProxy, the System UI might already died", remoteException);
            }

            if (DBG) {
                Log.d(TAG, "CarSystemUIProxy registered.");
            }

            int numCallbacks = mCarSystemUIProxyCallbacks.beginBroadcast();
            if (DBG) {
                Log.d(TAG, "Broadcasting CarSystemUIProxy connected to callbacks" + numCallbacks);
            }
            for (int i = 0; i < numCallbacks; i++) {
                try {
                    mCarSystemUIProxyCallbacks.getBroadcastItem(i).onConnected(
                            mCarSystemUIProxy);
                } catch (RemoteException remoteException) {
                    Log.e(TAG, "Error dispatching onConnected", remoteException);
                }
            }
            mCarSystemUIProxyCallbacks.finishBroadcast();
        }
    }

    @Override
    public boolean isCarSystemUIProxyRegistered() {
        synchronized (mLock) {
            return mCarSystemUIProxy != null;
        }
    }

    @Override
    public void addCarSystemUIProxyCallback(ICarSystemUIProxyCallback callback) {
        if (DBG) {
            Log.d(TAG, "addCarSystemUIProxyCallback " + callback.toString());
        }
        synchronized (mLock) {
            boolean alreadyExists = mCarSystemUIProxyCallbacks.unregister(callback);
            mCarSystemUIProxyCallbacks.register(callback);

            if (alreadyExists) {
                // Do not trigger onConnected() if the callback already exists because it is either
                // already called or will be called when the mCarSystemUIProxy is registered.
                Log.d(TAG, "Callback exists already, skip calling onConnected()");
                return;
            }

            // Trigger onConnected() on the callback.
            if (mCarSystemUIProxy == null) {
                if (DBG) {
                    Log.d(TAG, "Callback stored locally, car system ui proxy not "
                            + "registered.");
                }
                return;
            }
            try {
                callback.onConnected(mCarSystemUIProxy);
            } catch (RemoteException remoteException) {
                Log.e(TAG, "Error when dispatching onConnected", remoteException);
            }
        }
    }

    @Override
    public void removeCarSystemUIProxyCallback(ICarSystemUIProxyCallback callback) {
        if (DBG) {
            Log.d(TAG, "removeCarSystemUIProxyCallback " + callback.toString());
        }
        synchronized (mLock) {
            mCarSystemUIProxyCallbacks.unregister(callback);
        }
    }
}
