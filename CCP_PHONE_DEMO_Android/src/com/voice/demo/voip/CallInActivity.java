package com.voice.demo.voip;

import kr.re.Dev.Bluetooth.AlertManager;
import kr.re.Dev.Bluetooth.OmmiBotManager;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.CCP.phone.CameraInfo;
import com.hisun.phone.core.voice.Device;
import com.hisun.phone.core.voice.Device.CallType;
import com.hisun.phone.core.voice.model.CallStatisticsInfo;
import com.hisun.phone.core.voice.model.NetworkStatistic;
import com.hisun.phone.core.voice.util.Log4Util;
import com.hisun.phone.core.voice.util.VoiceUtil;
import com.voice.demo.CCPApplication;
import com.voice.demo.R;
import com.voice.demo.interphone.InviteInterPhoneActivity;
import com.voice.demo.tools.CCPIntentUtils;
import com.voice.demo.ui.CCPHelper;
import com.voice.demo.video.VideoActivity;

public class CallInActivity extends AudioVideoCallActivity implements OnClickListener {
	private TextView mVoipInputEt;
	// 键盘
	private ImageView mDiaerpadBtn;
	// 键盘区
	private LinearLayout mDiaerpad;
	// 挂机按钮
	private ImageView mVHangUp;
	// 名称显示区
	private TextView mVtalkName;
	// 号码
	private String mPhoneNumber;
	// 名称
	private String mNickName;
	// 通话 ID
	// private String mCurrentCallId;
	// voip 账号
	private String mVoipAccount;
	// 透传号码参数
	private static final String KEY_TEL = "tel";
	// 透传名称参数
	private static final String KEY_NAME = "nickname";
	private static final int REQUEST_CODE_VOIP_CALL = 120;
	private static final int REQUEST_CODE_CALL_TRANSFER = 130;
	private boolean isDialerShow = false;
	private Button mVideoBegin;
	private ImageView mVideoIcon;
	// private TextView mVideoTopTips;
	// private TextView mVideoCallTips;
	private SurfaceView mVideoView;
	// Remote Video
	private FrameLayout mVideoLayout;
	CameraInfo[] cameraInfos;
	Intent currIntent;
	int numberOfCameras;
	private int mWidth;
	private int mHeight;
	private OmmiBotManager mOmmiBotManager;
	private AlertManager mAlertManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isIncomingCall = true;
		initProwerManager();
		enterIncallMode();
		currIntent = getIntent();
		initialize(currIntent);
		initResourceRefs();
		mOmmiBotManager = new OmmiBotManager(this) {
			@Override
			protected void onSendTextMesage(String text) {
				super.onSendTextMesage(text);
				CallInActivity.this.onSendTextMesage(text);
			}
		};
		mOmmiBotManager.start();
		registerReceiver(new String[] { CCPIntentUtils.INTENT_P2P_ENABLED });
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (!isConnect) {
			if (getCallHandler() != null) {
				getCallHandler().removeCallbacks(finish);
			}
			releaseCurrCall(false);
			currIntent = intent;
			initialize(currIntent);
			initResourceRefs();
		}
	}

	/**
	 * Initialize all UI elements from resources.
	 * 
	 */
	private void initResourceRefs() {
		isConnect = false;
		if (mCallType == Device.CallType.VIDEO) {
			// video ..
			setContentView(R.layout.layout_video_activity);
			mVideoIcon = (ImageView) findViewById(R.id.video_icon);
			// Top tips view invited ...
			logText(getString(R.string.str_video_invited_recivie, mVoipAccount.substring(mVoipAccount.length() - 3, mVoipAccount.length())));
			// 接受
			mVideoBegin = (Button) findViewById(R.id.video_botton_cancle);
			mVideoBegin.setBackgroundResource(R.drawable.video_button_begin);
			mVideoBegin.setText(R.string.str_video_call_begin);
			mVideoBegin.setVisibility(View.VISIBLE);
			mVideoBegin.setOnClickListener(this);
			mVideoView = (SurfaceView) findViewById(R.id.video_view);
			mVideoView.getHolder().setFixedSize(240, 320);
			mLoaclVideoView = (RelativeLayout) findViewById(R.id.localvideo_view);
			mVideoLayout = (FrameLayout) findViewById(R.id.Video_layout);
			if (checkeDeviceHelper()) {
				cameraInfos = getDeviceHelper().getCameraInfo();
				// Find the ID of the default camera
				if (cameraInfos != null) {
					numberOfCameras = cameraInfos.length;
				}
				// Find the total number of cameras available
				for (int i = 0; i < numberOfCameras; i++) {
					if (cameraInfos[i].index == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
						defaultCameraId = i;
						comportCapbilityIndex(cameraInfos[i].caps);
					}
				}
				getDeviceHelper().setVideoView(mVideoView, null);
			}
			DisplayLocalSurfaceView();
			mAlertManager = new AlertManager(this) {
				@Override
				public void stopCall() {
					super.stopCall();
					doHandUpReleaseCall();
				}

				@Override
				public void alert(int irId) {
					super.alert(irId);
					mOmmiBotManager.alert(irId);
				}
			};
		} else {
			setContentView(R.layout.layout_callin);
			mVoipInputEt = (TextView) findViewById(R.id.voip_input);
			mVtalkName = (TextView) findViewById(R.id.layout_callin_name);
			mVtalkNumber = (TextView) findViewById(R.id.layout_callin_number);
			((ImageButton) findViewById(R.id.layout_callin_cancel)).setOnClickListener(this);
			((ImageButton) findViewById(R.id.layout_callin_accept)).setOnClickListener(this);
			setDisplayNameNumber();
		}
	}

	@Override
	public void onReceiveTextMessage(String text) {
		super.onReceiveTextMessage(text);
		final String[] sa = text.split(":");
		mAlertManager.alert(0);
		// receive:IR1:IR2:IR3:IR4:bettery // receive:false:false:true:true:1
		if (sa[1].equals("true")) {
			mAlertManager.alert(1);
		} else if (sa[2].equals("true")) {
			mAlertManager.alert(2);
		} else if (sa[3].equals("true")) {
			mAlertManager.alert(3);
		} else if (sa[4].equals("true")) {
			mAlertManager.alert(4);
		}
		mOmmiBotManager.alert(0);
		mAlertManager.bettery(Integer.parseInt(sa[5]));
	}

	private void setDisplayNameNumber() {
		if (mCallType == Device.CallType.VOICE) {
			// viop call ...
			if (!TextUtils.isEmpty(mVoipAccount)) {
				mVtalkNumber.setText(mVoipAccount);
			}
		} else {
			// viop call ...
			if (!TextUtils.isEmpty(mPhoneNumber)) {
				mVtalkNumber.setText(mPhoneNumber);
				Log4Util.d(CCPHelper.DEMO_TAG, "[CallInActivity] mPhoneNumber " + mPhoneNumber);
			}
			if (!TextUtils.isEmpty(mNickName)) {
				mVtalkName.setText(mNickName);
				Log4Util.d(CCPHelper.DEMO_TAG, "[CallInActivity] VtalkName" + mVtalkName);
			} else {
				mVtalkName.setText(R.string.voip_unknown_user);
			}
		}
	}

	/**
	 * Read parameters or previously saved state of this activity.
	 * 
	 */
	private void initialize(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras == null) {
			finish();
			return;
		}
		mVoipAccount = extras.getString(Device.CALLER);
		mCurrentCallId = extras.getString(Device.CALLID);
		mCallType = (CallType) extras.get(Device.CALLTYPE);
		// 传入数据是否有误
		if (mVoipAccount == null || mCurrentCallId == null) {
			finish();
			return;
		}
		if (mGroupId == null) {
			mGroupId = mVoipAccount;
		}
		// 透传信息
		String[] infos = extras.getStringArray(Device.REMOTE);
		if (infos != null && infos.length > 0) {
			for (String str : infos) {
				if (str.startsWith(KEY_TEL)) {
					mPhoneNumber = VoiceUtil.getLastwords(str, "=");
				} else if (str.startsWith(KEY_NAME)) {
					mNickName = VoiceUtil.getLastwords(str, "=");
				}
			}
		}
	}

	public void initCallHold() {
		Log4Util.d(CCPHelper.DEMO_TAG, "[CallInActivity] initCallHold.收到呼叫连接，初始化通话界面.");
		isConnect = true;
		setContentView(R.layout.layout_call_interface);
		mCallTransferBtn = (ImageView) findViewById(R.id.layout_callin_transfer);
		mCallTransferBtn.setOnClickListener(this);
		mCallMute = (ImageView) findViewById(R.id.layout_callin_mute);
		mCallHandFree = (ImageView) findViewById(R.id.layout_callin_handfree);
		mVHangUp = (ImageView) findViewById(R.id.layout_call_reject);
		mVtalkName = (TextView) findViewById(R.id.layout_callin_name);
		mVtalkName.setVisibility(View.VISIBLE);
		mVtalkNumber = (TextView) findViewById(R.id.layout_callin_number);
		// 键盘
		mDiaerpadBtn = (ImageView) findViewById(R.id.layout_callin_diaerpad);
		mDiaerpad = (LinearLayout) findViewById(R.id.layout_diaerpad);
		setupKeypad();
		mDmfInput = (EditText) findViewById(R.id.dial_input_numer_TXT);
		mDiaerpadBtn.setOnClickListener(this);
		mCallMute.setOnClickListener(this);
		mCallHandFree.setOnClickListener(this);
		mVHangUp.setOnClickListener(this);
		setDisplayNameNumber();
		initCallTools();
	}

	@Override
	protected void onCallVideoRatioChanged(String callid, int width, int height) {
		super.onCallVideoRatioChanged(callid, width, height);
		if (mCallType != CallType.VIDEO) {
			return;
		}
		try {
			mWidth = width;
			mHeight = height;
			if (mVideoView != null) {
				// mVideoView.getHolder().setFixedSize(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
				LayoutParams layoutParams2 = (LayoutParams) mVideoView.getLayoutParams();
				layoutParams2.width = mWidth;
				layoutParams2.height = mHeight;
				mVideoView.setLayoutParams(layoutParams2);
			}
			int[] decodeDisplayMetrics = decodeDisplayMetrics();
			if (mWidth >= decodeDisplayMetrics[0] || mHeight >= decodeDisplayMetrics[1]) {
				return;
			}
			doResizeView(decodeDisplayMetrics[0]);
		} catch (Exception e) {
		}
	}

	@Override
	protected void onCallVideoRatioChanged(String callid, String resolution) {
		super.onCallVideoRatioChanged(callid, resolution);
		if (mCallType != CallType.VIDEO) {
			return;
		}
		if (TextUtils.isEmpty(resolution) || !resolution.contains("x")) {
			return;
		}
		String[] split = resolution.split("x");
		try {
			onCallVideoRatioChanged(callid, Integer.parseInt(split[0]), Integer.parseInt(split[1]));
		} catch (Exception e) {
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_select_voip: // select voip ...
			Intent intent = new Intent(this, InviteInterPhoneActivity.class);
			intent.putExtra("create_to", InviteInterPhoneActivity.CREATE_TO_VOIP_CALL);
			startActivityForResult(intent, REQUEST_CODE_VOIP_CALL);
			// keypad
		case R.id.zero: {
			keyPressed(KeyEvent.KEYCODE_0);
			return;
		}
		case R.id.one: {
			keyPressed(KeyEvent.KEYCODE_1);
			return;
		}
		case R.id.two: {
			keyPressed(KeyEvent.KEYCODE_2);
			return;
		}
		case R.id.three: {
			keyPressed(KeyEvent.KEYCODE_3);
			return;
		}
		case R.id.four: {
			keyPressed(KeyEvent.KEYCODE_4);
			return;
		}
		case R.id.five: {
			keyPressed(KeyEvent.KEYCODE_5);
			return;
		}
		case R.id.six: {
			keyPressed(KeyEvent.KEYCODE_6);
			return;
		}
		case R.id.seven: {
			keyPressed(KeyEvent.KEYCODE_7);
			return;
		}
		case R.id.eight: {
			keyPressed(KeyEvent.KEYCODE_8);
			return;
		}
		case R.id.nine: {
			keyPressed(KeyEvent.KEYCODE_9);
			return;
		}
		case R.id.star: {
			keyPressed(KeyEvent.KEYCODE_STAR);
			return;
		}
		case R.id.pound: {
			keyPressed(KeyEvent.KEYCODE_POUND);
			return;
		}
		case R.id.layout_callin_accept:
		case R.id.video_botton_cancle: // video ..
			// 接受呼叫
			// mTime = 0;
			try {
				if (checkeDeviceHelper() && mCurrentCallId != null) {
					getDeviceHelper().acceptCall(mCurrentCallId);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			Log4Util.d(CCPHelper.DEMO_TAG, "[CallInActivity] acceptCall...");
			break;
		case R.id.layout_callin_mute:
			// 设置静音
			setMuteUI();
			break;
		case R.id.layout_callin_handfree:
			// 设置免提
			sethandfreeUI();
			break;
		case R.id.layout_callin_diaerpad:
			// 设置键盘
			setDialerpadUI();
			break;
		case R.id.layout_callin_cancel:
		case R.id.layout_call_reject:
			doHandUpReleaseCall();
			break;
		case R.id.layout_callin_transfer: // select voip ...
			Intent i = new Intent(this, GetNumberToTransferActivity.class);
			startActivityForResult(i, REQUEST_CODE_CALL_TRANSFER);
			break;
		default:
			break;
		}
	}

	@Override
	protected void doHandUpReleaseCall() {
		super.doHandUpReleaseCall();
		try {
			if (isConnect) {
				// 通话中挂断
				if (checkeDeviceHelper() && mCurrentCallId != null) {
					getDeviceHelper().releaseCall(mCurrentCallId);
					stopVoiceRecording(mCurrentCallId);
				}
			} else {
				// 呼入拒绝
				if (checkeDeviceHelper() && mCurrentCallId != null) {
					getDeviceHelper().rejectCall(mCurrentCallId, 6);
				}
				finish();
			}
			Log4Util.d(CCPHelper.DEMO_TAG, "[CallInActivity] call stop isConnect: " + isConnect);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 延时关闭界面
	 */
	final Runnable finish = new Runnable() {
		public void run() {
			finish();
		}
	};

	@Override
	protected void onDestroy() {
		mOmmiBotManager.stop();
		releaseCurrCall(true);
		releaseWakeLock();
		super.onDestroy();
	}

	private void releaseCurrCall(boolean releaseAll) {
		currIntent = null;
		if (getCallHandler() != null && releaseAll) {
			setCallHandler(null);
		}
		mCallTransferBtn = null;
		mCallMute = null;
		mCallHandFree = null;
		mVHangUp = null;
		// mCallStateTips = null;
		mVtalkName = null;
		mVtalkNumber = null;
		if (checkeDeviceHelper()) {
			if (isMute) {
				getDeviceHelper().setMute(!isMute);
			}
			if (isHandsfree) {
				getDeviceHelper().enableLoudsSpeaker(!isMute);
			}
			if (TextUtils.isEmpty(mCurrentCallId))
				stopVoiceRecording(mCurrentCallId);
		}
		mPhoneNumber = null;
		CCPApplication.getInstance().setAudioMode(AudioManager.MODE_NORMAL);
	}

	/**
	 * 用于挂断时修改按钮属性及关闭操作
	 */
	private void finishCalling() {
		try {
			if (isConnect) {
				isConnect = false;
				if (mCallType == Device.CallType.VIDEO) {
					mVideoLayout.setVisibility(View.GONE);
					mVideoIcon.setVisibility(View.VISIBLE);
					// mVideoTopTips.setVisibility(View.VISIBLE);
					mLoaclVideoView.removeAllViews();
					mLoaclVideoView.setVisibility(View.GONE);
					logText(R.string.voip_calling_finish);
					// }
					// bottom can't click ...
				} else {
					logText(R.string.voip_calling_finish);
					mCallHandFree.setClickable(false);
					mCallMute.setClickable(false);
					mVHangUp.setClickable(false);
					mDiaerpadBtn.setClickable(false);
					mDiaerpadBtn.setImageResource(R.drawable.call_interface_diaerpad);
					mCallHandFree.setImageResource(R.drawable.call_interface_hands_free);
					mCallMute.setImageResource(R.drawable.call_interface_mute);
					mVHangUp.setBackgroundResource(R.drawable.call_interface_non_red_button);
				}
				// 延时关闭
				getCallHandler().postDelayed(finish, 3000);
			} else {
				finish();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void setDialerpadUI() {
		if (isDialerShow) {
			mDiaerpadBtn.setImageResource(R.drawable.call_interface_diaerpad);
			mDiaerpad.setVisibility(View.GONE);
			isDialerShow = false;
		} else {
			mDiaerpadBtn.setImageResource(R.drawable.call_interface_diaerpad_on);
			mDiaerpad.setVisibility(View.VISIBLE);
			isDialerShow = true;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			// do nothing.
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onCallAnswered(String callid) {
		super.onCallAnswered(callid);
		// answer
		Log4Util.d(CCPHelper.DEMO_TAG, "[CallInActivity] voip on call answered!!");
		if (callid != null && callid.equals(mCurrentCallId)) {
			if (mCallType == Device.CallType.VIDEO) {
				initResVideoSuccess();
				getDeviceHelper().enableLoudsSpeaker(true);
			} else {
				initialize(currIntent);
				// voip other ..
				initCallHold();
			}
			if (getCallHandler() != null) {
				getCallHandler().sendMessage(getCallHandler().obtainMessage(VideoActivity.WHAT_ON_CODE_CALL_STATUS));
			}
			// startVoiceRecording(callid);
		}
	}

	@Override
	protected void onCallReleased(String callid) {
		super.onCallReleased(callid);
		// 挂断
		Log4Util.d(CCPHelper.DEMO_TAG, "[CallInActivity] voip on call released!!");
		try {
			if (callid != null && callid.equals(mCurrentCallId)) {
				stopVoiceRecording(callid);
				finishCalling();
				// finish();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void handleNotifyMessage(Message msg) {
		super.handleNotifyMessage(msg);
		switch (msg.what) {
		case VideoActivity.WHAT_ON_CODE_CALL_STATUS:
			if (!checkeDeviceHelper()) {
				return;
			}
			if (!isConnect) {
				return;
			}
			CallStatisticsInfo callStatistics = getDeviceHelper().getCallStatistics(mCallType);
			NetworkStatistic trafficStats = null;
			if (mCallType == CallType.VOICE || mCallType == CallType.VIDEO) {
				trafficStats = getDeviceHelper().getNetworkStatistic(mCurrentCallId);
			}
			if (callStatistics != null) {
				int fractionLost = callStatistics.getFractionLost();
				int rttMs = callStatistics.getRttMs();
				if (mCallType == CallType.VOICE) {
					if (trafficStats != null) {
						logText(getString(R.string.str_call_traffic_status, rttMs, (fractionLost / 255), trafficStats.getTxBytes() / 1024, trafficStats.getRxBytes() / 1024));
					} else {
						logText(getString(R.string.str_call_status, rttMs, (fractionLost / 255)));
					}
				} else {
					if (trafficStats != null) {
						logText(getString(R.string.str_call_traffic_status, rttMs, (fractionLost / 255), trafficStats.getTxBytes() / 1024, trafficStats.getRxBytes() / 1024));
					} else {
						logText(getString(R.string.str_call_status, rttMs, (fractionLost / 255)));
					}
				}
			}
			if (getCallHandler() != null) {
				Message callMessage = getCallHandler().obtainMessage(VideoActivity.WHAT_ON_CODE_CALL_STATUS);
				getCallHandler().sendMessageDelayed(callMessage, 4000);
			}
			break;
		// This call may be redundant, but to ensure compatibility system event
		// more,
		// not only is the system call
		case CCPHelper.WHAT_ON_RECEIVE_SYSTEM_EVENTS:
			doHandUpReleaseCall();
			break;
		default:
			break;
		}
	}

	// video ..
	private void initResVideoSuccess() {
		mVideoLayout.setVisibility(View.VISIBLE);
		mVideoIcon.setVisibility(View.GONE);
		logText(getString(R.string.str_video_bottom_time, mVoipAccount.substring(mVoipAccount.length() - 3, mVoipAccount.length())));
		mVideoBegin.setVisibility(View.GONE);
		isConnect = true;
	}
	/*********************************** KeyPad *************************************************/
	private EditText mDmfInput;

	void keyPressed(int keyCode) {
		if (!checkeDeviceHelper()) {
			return;
		}
		KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
		mDmfInput.getText().clear();
		mDmfInput.onKeyDown(keyCode, event);
		getDeviceHelper().sendDTMF(mCurrentCallId, mDmfInput.getText().toString().toCharArray()[0]);
	}

	private void setupKeypad() {
		/** Setup the listeners for the buttons */
		findViewById(R.id.zero).setOnClickListener(this);
		findViewById(R.id.one).setOnClickListener(this);
		findViewById(R.id.two).setOnClickListener(this);
		findViewById(R.id.three).setOnClickListener(this);
		findViewById(R.id.four).setOnClickListener(this);
		findViewById(R.id.five).setOnClickListener(this);
		findViewById(R.id.six).setOnClickListener(this);
		findViewById(R.id.seven).setOnClickListener(this);
		findViewById(R.id.eight).setOnClickListener(this);
		findViewById(R.id.nine).setOnClickListener(this);
		findViewById(R.id.star).setOnClickListener(this);
		findViewById(R.id.pound).setOnClickListener(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log4Util.d(CCPHelper.DEMO_TAG, "[SelectVoiceActivity] onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
		// If there's no data (because the user didn't select a number and
		// just hit BACK, for example), there's nothing to do.
		if (requestCode != REQUEST_CODE_VOIP_CALL) {
			if (data == null) {
				return;
			}
		} else if (resultCode != RESULT_OK) {
			Log4Util.d(CCPHelper.DEMO_TAG, "[SelectVoiceActivity] onActivityResult: bail due to resultCode=" + resultCode);
			return;
		}
		String phoneStr = "";
		if (data.hasExtra("VOIP_CALL_NUMNBER")) {
			Bundle extras = data.getExtras();
			if (extras != null) {
				phoneStr = extras.getString("VOIP_CALL_NUMNBER");
			}
		}
		if (mCurrentCallId != null) {
			int setCallTransfer = setCallTransfer(mCurrentCallId, phoneStr);
			if (setCallTransfer != 0) {
				Toast.makeText(getApplicationContext(), "呼转发起失败！", 1).show();
			}
		} else {
			Toast.makeText(getApplicationContext(), "通话已经不存在", 1).show();
		}
	}

	/**
	 * @param progress
	 */
	private void doResizeView(int progress) {
		if (mVideoView != null) {
			LayoutParams layoutParams2 = (LayoutParams) mVideoView.getLayoutParams();
			layoutParams2.width = progress;
			layoutParams2.height = mHeight * progress / mWidth;
			mVideoView.setLayoutParams(layoutParams2);
		}
	}
}
