package com.android.wm.shell.ext;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a handle to the lifecycle of the host (container) that creates the
 * {@link CarTaskViewController}.
 * The container can be an activity, fragment, view or a window.
 */
public final class CarTaskViewControllerHostLifecycle {
    /** An interface for observing the lifecycle of the container (host). */
    public interface CarTaskViewControllerHostLifecycleObserver {
        /** Gets called when the container is destroyed. */
        void onHostDestroyed(CarTaskViewControllerHostLifecycle lifecycle);

        /** Gets called when the container has appeared. */
        void onHostAppeared(CarTaskViewControllerHostLifecycle lifecycle);

        /** Gets called when the container has disappeared. */
        void onHostDisappeared(CarTaskViewControllerHostLifecycle lifecycle);
    }

    private final List<CarTaskViewControllerHostLifecycleObserver> mObserverList =
            new ArrayList<>();

    private boolean mIsVisible = false;

    /**
     * Notifies the lifecycle observers that the host has been destroyed and they can clean their
     * internal state.
     */
    public void hostDestroyed() {
        for (CarTaskViewControllerHostLifecycleObserver observer : mObserverList) {
            observer.onHostDestroyed(this);
        }
    }

    /** Notifies the lifecycle observers that the host has appeared. */
    public void hostAppeared() {
        mIsVisible = true;
        for (CarTaskViewControllerHostLifecycleObserver observer : mObserverList) {
            observer.onHostAppeared(this);
        }
    }

    /** Notifies the lifecycle observers that the host has disappeared. */
    public void hostDisappeared() {
        mIsVisible = false;
        for (CarTaskViewControllerHostLifecycleObserver observer : mObserverList) {
            observer.onHostDisappeared(this);
        }
    }

    /** @return true if the container is visible, false otherwise. */
    public boolean isVisible() {
        return mIsVisible;
    }

    /** Registers the given observer to listen to lifecycle of the container. */
    public void registerObserver(CarTaskViewControllerHostLifecycleObserver observer) {
        mObserverList.add(observer);
    }

    /** Unregisters the given observer to stop listening to the lifecycle of the container. */
    public void unregisterObserver(CarTaskViewControllerHostLifecycleObserver observer) {
        mObserverList.remove(observer);
    }
}
