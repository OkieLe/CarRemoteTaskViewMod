package com.android.wm.shell.ext;

import com.android.wm.shell.ext.ICarSystemUIProxy;

/**
  * Callback interface to monitor the lifecycle of the CarSystemUIProxy.
  */
oneway interface ICarSystemUIProxyCallback {
    /** Called when the System UI proxy is connected. */
    void onConnected(ICarSystemUIProxy carSystemUIProxy);
}
