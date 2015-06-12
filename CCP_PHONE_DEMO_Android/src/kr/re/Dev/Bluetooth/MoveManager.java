package kr.re.Dev.Bluetooth;

import android.app.Activity;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.AbsoluteLayout.LayoutParams;
import android.widget.ImageView;
import com.voice.demo.R;

/**
 * 
 * @author Lenovo
 * 
 */
public class MoveManager implements OnTouchListener {
	private static final String TAG = MoveManager.class.getSimpleName();
	private final ImageView mFingerImage;
	private final ImageView mIr1, mIr2, mIr3, mIr4, mIr5;
	private final AbsoluteLayout mFingerLayout;
	private final LayoutParams mFingerLayoutParams;
	private final int mFingerWidth, mFingerHeight, mWidth, mHeight;
	private final int mHalfFingerWidth, mHalfFingerHeight, mHalfWidth, mHalfHeight;

	public MoveManager(Activity activity) {
		mFingerImage = (ImageView) activity.findViewById(R.id.finger_image);
		mFingerLayout = (AbsoluteLayout) activity.findViewById(R.id.finger_layout);
		mIr1 = (ImageView) activity.findViewById(R.id.ir1);
		mIr2 = (ImageView) activity.findViewById(R.id.ir2);
		mIr3 = (ImageView) activity.findViewById(R.id.ir3);
		mIr4 = (ImageView) activity.findViewById(R.id.ir4);
		mIr5 = (ImageView) activity.findViewById(R.id.ir5);
		mFingerLayout.setOnTouchListener(this);
		mFingerLayoutParams = (LayoutParams) mFingerImage.getLayoutParams();
		final ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) mFingerLayout.getLayoutParams();
		mWidth = lp.width;
		mHeight = lp.height;
		mFingerWidth = mFingerLayoutParams.width;
		mFingerHeight = mFingerLayoutParams.height;
		mHalfFingerWidth = (mFingerWidth >> 1);
		mHalfFingerHeight = (mFingerHeight >> 1);
		mHalfWidth = (mWidth >> 1);
		mHalfHeight = (mHeight >> 1);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		handleTouch(event);
		return true;
	}

	// 范围0-250，单位mm/s 单位0.1°
	public void touch(int direction, int speed) {
		Log.i(TAG, "direction " + direction + " speed " + speed);
	}

	private void handleTouch(MotionEvent event) {
		final int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
			final float x = event.getX();
			final float y = event.getY();
			mFingerLayoutParams.x = (int) (x - mHalfFingerWidth);
			mFingerLayoutParams.y = (int) (y - mHalfFingerHeight);
			calc(x, y);
		} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) { // 回位
			mFingerLayoutParams.x = mHalfWidth - mHalfFingerWidth;
			mFingerLayoutParams.y = mHalfHeight - mHalfFingerHeight;
			touch(0, 0);
		}
		updateFinger();
	}

	private void updateFinger() {
		if (mFingerLayoutParams.y < 0) {
			mFingerLayoutParams.y = 0;
		} else if (mFingerLayoutParams.y > mHeight) {
			mFingerLayoutParams.y = mHeight;
		}
		if (mFingerLayoutParams.x < 0) {
			mFingerLayoutParams.x = 0;
		} else if (mFingerLayoutParams.x > mWidth) {
			mFingerLayoutParams.x = mWidth;
		}
		mFingerImage.setLayoutParams(mFingerLayoutParams);
	}

	private void calc(float x, float y) {
		// 转换为直角坐标
		x -= mHalfWidth;
		y -= mHalfHeight;
		final float r = FloatMath.sqrt(x * x + y * y);
		float direction = (float) (Math.atan2(-y, x) / Math.PI * 180);
		direction = direction > 0 ? direction : direction + 360;
		direction *= 10;
		float speed = r > mHalfWidth ? 250 : r / mHalfWidth * 250;
		touch((int) direction, (int) speed);
	}

	public void alert(int irId) {
		if (irId == 0) {
			mIr1.setVisibility(View.GONE);
			mIr2.setVisibility(View.GONE);
			mIr3.setVisibility(View.GONE);
			mIr4.setVisibility(View.GONE);
			mIr5.setVisibility(View.GONE);
		} else if (irId == 1) {
			mIr1.setVisibility(View.VISIBLE);
		} else if (irId == 2) {
			mIr2.setVisibility(View.VISIBLE);
		} else if (irId == 3) {
			mIr3.setVisibility(View.VISIBLE);
		} else if (irId == 4) {
			mIr4.setVisibility(View.VISIBLE);
		} else if (irId == 5) {
			mIr5.setVisibility(View.VISIBLE);
		}
	}
}