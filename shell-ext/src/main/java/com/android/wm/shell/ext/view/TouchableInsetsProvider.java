package com.android.wm.shell.ext.view;

import android.graphics.Rect;
import android.graphics.Region;
import android.view.View;
import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.ViewTreeObserver.OnComputeInternalInsetsListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

/**
 * Calculates {InternalInsetsInfo#TOUCHABLE_INSETS_REGION} for the given {@link View}.
 * <p>The touch events on the View will pass through the host and be delivered to the window
 * below it.
 *
 * <p>It also provides the api {@link #setObscuredTouchRegion(Region)} to specify the region which
 * the view host can accept the touch events on it.
 */
@UiThread
public final class TouchableInsetsProvider {
    private static final String TAG = TouchableInsetsProvider.class.getSimpleName();
    private final View mView;
    private final OnComputeInternalInsetsListener mListener = this::onComputeInternalInsets;
    private final int[] mLocation = new int[2];
    private final Rect mRect = new Rect();

    @Nullable
    private Region mObscuredTouchRegion;

    public TouchableInsetsProvider(@NonNull View view) {
        mView = view;
    }

    /**
     * Specifies the region of the view which the view host can accept the touch events.
     *
     * @param obscuredRegion the obscured region of the view.
     */
    public void setObscuredTouchRegion(@Nullable Region obscuredRegion) {
        mObscuredTouchRegion = obscuredRegion;
    }

    private void onComputeInternalInsets(InternalInsetsInfo inoutInfo) {
        if (!mView.isVisibleToUser()) {
            return;
        }
        if (inoutInfo.touchableRegion.isEmpty()) {
            // This is the first View to set touchableRegion, then set the entire Window as
            // touchableRegion first, then subtract each View's region from it.
            inoutInfo.setTouchableInsets(InternalInsetsInfo.TOUCHABLE_INSETS_REGION);
            View root = mView.getRootView();
            root.getLocationInWindow(mLocation);
            mRect.set(mLocation[0], mLocation[1],
                    mLocation[0] + root.getWidth(), mLocation[1] + root.getHeight());
            inoutInfo.touchableRegion.set(mRect);
        }
        mView.getLocationInWindow(mLocation);
        mRect.set(mLocation[0], mLocation[1],
                mLocation[0] + mView.getWidth(), mLocation[1] + mView.getHeight());
        inoutInfo.touchableRegion.op(mRect, Region.Op.DIFFERENCE);

        if (mObscuredTouchRegion != null) {
            inoutInfo.touchableRegion.op(mObscuredTouchRegion, Region.Op.UNION);
        }
    }

    ;

    /**
     * Registers this to the internal insets computation callback.
     */
    public void addToViewTreeObserver() {
        mView.getViewTreeObserver().addOnComputeInternalInsetsListener(mListener);
    }

    /**
     * Removes this from the internal insets computation callback.
     */
    public void removeFromViewTreeObserver() {
        mView.getViewTreeObserver().removeOnComputeInternalInsetsListener(mListener);
    }

    @Override
    public String toString() {
        return TAG + "(rect=" + mRect + ", obscuredTouch=" + mObscuredTouchRegion + ")";
    }
}
