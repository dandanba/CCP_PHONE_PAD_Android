package kr.re.Dev.Bluetooth;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
public class OmmiBotManager {
	private MoveManager mMoveManager;
	private ScrollManager mScrollManager;
	private String mText = "touch:" + 0 + ":" + 0;
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			final int what = msg.what;
			removeMessages(what);
			onSendTextMesage(mText);
			sendEmptyMessageDelayed(what, 500);
		};
	};

	public OmmiBotManager(Activity activity) {
		mMoveManager = new MoveManager(activity) {
			// 范围0-250，单位mm/s 单位0.1°
			public void touch(int direction, int speed) {
				super.touch(direction, speed);
				mText = "touch:" + direction + ":" + speed;
			}
		};
		mScrollManager = new ScrollManager(activity) {
			public void speed(int speed) {
				super.speed(speed);
				mText = "speed:" + speed;
			}
		};
	}

	public void start() {
		mHandler.sendEmptyMessage(1);
	}

	public void stop() {
		mHandler.removeMessages(1);
	}

	protected void onSendTextMesage(String text) {
	}

	public void alert(int irId) {
		mMoveManager.alert(irId);
	}
}
