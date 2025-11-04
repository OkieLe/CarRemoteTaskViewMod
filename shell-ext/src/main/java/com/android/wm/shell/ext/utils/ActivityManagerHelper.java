package com.android.wm.shell.ext.utils;

import android.app.Activity;
import android.os.IBinder;

public class ActivityManagerHelper {

    /** See {Activity#getActivityToken()} */
    public static IBinder getActivityToken(Activity activity) {
        return activity.getActivityToken();
    }

    /** See {Activity#isVisibleForAutofill()} */
    public static boolean isVisible(Activity activity) {
        return activity.isVisibleForAutofill();
    }
}
