package kr.re.Dev.Bluetooth;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.AbsoluteLayout.LayoutParams;
import android.widget.ImageView;
import com.voice.demo.R;
public class ScrollManager implements OnTouchListener {
	private static final String TAG = ScrollManager.class.getSimpleName();
	private final ImageView mScrollImage;
	private final AbsoluteLayout mBarLayout;
	private final LayoutParams mBarLayoutParams;
	private final int mFingerWidth, mFingerHeight, mWidth, mHeight;
	private final int mHalfScrollWidth, mHalfScrollHeight, mHalfWidth, mHalfHeight;

	public ScrollManager(Activity activity) {
		mScrollImage = (ImageView) activity.findViewById(R.id.scroll_image);
		mBarLayout = (AbsoluteLayout) activity.findViewById(R.id.bar_layout);
		mBarLayout.setOnTouchListener(this);
		mBarLayoutParams = (LayoutParams) mScrollImage.getLayoutParams();
		final ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) mBarLayout.getLayoutParams();
		mWidth = lp.width;
		mHeight = lp.height;
		mFingerWidth = mBarLayoutParams.width;
		mFingerHeight = mBarLayoutParams.height;
		mHalfScrollWidth = (mFingerWidth >> 1);
		mHalfScrollHeight = (mFingerHeight >> 1);
		mHalfWidth = (mWidth >> 1);
		mHalfHeight = (mHeight >> 1);
		mBarLayoutParams.x = mHalfWidth - mHalfScrollWidth;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		handleTouch(event);
		return true;
	}

	public void speed(int speed) {
		Log.i(TAG, " speed " + speed);
	}

	private void handleTouch(MotionEvent event) {
		final int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
			final float y = event.getY();
			mBarLayoutParams.y = (int) (y - mHalfScrollHeight);
			calc(y);
		} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) { // 回位
			mBarLayoutParams.y = mHalfHeight - mHalfScrollHeight;
			speed(0);
		}
		updateFinger();
	}

	private void updateFinger() {
		if (mBarLayoutParams.y < 0) {
			mBarLayoutParams.y = 0;
		} else if (mBarLayoutParams.y > mHeight) {
			mBarLayoutParams.y = mHeight;
		}
		mScrollImage.setLayoutParams(mBarLayoutParams);
	}

	private void calc(float y) {
		y -= mHalfHeight;
		if (y < -mHalfHeight) {
			y = -mHalfHeight;
		} else if (y > mHalfHeight) {
			y = mHalfHeight;
		}
		speed((int) (y * 250 / mHalfHeight));
	}
}