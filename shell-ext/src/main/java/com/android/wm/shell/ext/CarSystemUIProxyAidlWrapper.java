package com.android.wm.shell.ext;

import android.os.IBinder;
import android.os.RemoteException;

/**
 * This class wraps the client's {@link CarSystemUIProxy} implementation & facilitates the
 * communication with {@link ICarSystemUIProxy}.
 */
final class CarSystemUIProxyAidlWrapper extends ICarSystemUIProxy.Stub {
    private final CarSystemUIProxy mCarSystemUIProxy;

    CarSystemUIProxyAidlWrapper(CarSystemUIProxy carSystemUIProxy) {
        mCarSystemUIProxy = carSystemUIProxy;
    }

    @Override
    public ICarTaskViewHost createControlledCarTaskView(ICarTaskViewClient client) {
        return createCarTaskView(client);
    }

    @Override
    public ICarTaskViewHost createCarTaskView(ICarTaskViewClient client) {
        CarTaskViewHost carTaskViewHost =
                mCarSystemUIProxy.createCarTaskView(new CarTaskViewClient(client));

        IBinder.DeathRecipient clientDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                carTaskViewHost.release();
                client.asBinder().unlinkToDeath(this, /* flags= */ 0);
            }
        };

        try {
            client.asBinder().linkToDeath(clientDeathRecipient, /* flags= */ 0);
        } catch (RemoteException ex) {
            throw new IllegalStateException(
                    "Linking to binder death failed for "
                            + "ICarTaskViewClient, the System UI might already died");
        }

        return new CarTaskViewHostAidlToImplAdapter(carTaskViewHost);
    }
}
