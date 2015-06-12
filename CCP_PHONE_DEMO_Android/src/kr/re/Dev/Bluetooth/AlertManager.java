package kr.re.Dev.Bluetooth;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;
import com.voice.demo.R;

public class AlertManager implements OnClickListener {
	private final ImageView mBetteryImage, mCallImage, mPhotoImage;
	private final ImageView mAlertView;
	private final Activity mActivity;
	private Toast mAlert;

	public AlertManager(Activity activity) {
		mActivity = activity;
		mBetteryImage = (ImageView) activity.findViewById(R.id.bettery_image);
		mCallImage = (ImageView) activity.findViewById(R.id.call_image);
		mPhotoImage = (ImageView) activity.findViewById(R.id.photo_image);
		mCallImage.setOnClickListener(this);
		mAlertView = new ImageView(activity);
		mAlertView.setImageResource(R.drawable.toast);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.call_image:
			stopCall();
			break;
		default:
			break;
		}
	}

	public void stopCall() {
	}

	/**
	 * 0-150
	 * 
	 * @param voltage
	 */
	public void bettery(int voltage) {
		final String name = String.format("bettery_%1$d", voltage);
		final String pn = mActivity.getPackageName();
		mBetteryImage.setImageResource(mActivity.getResources().getIdentifier(name, "drawable", pn));
	}

	/**
	 * 
	 */
	public void alert(int irId) {
		if (mAlert != null) {
			mAlert.cancel();
			mAlert = null;
		}
		mAlert = new Toast(mActivity);
		mAlert.setGravity(Gravity.CENTER, 0, 0);
		mAlert.setDuration(Toast.LENGTH_SHORT);
		mAlert.setView(mAlertView);
		mAlert.show();
	}
}
