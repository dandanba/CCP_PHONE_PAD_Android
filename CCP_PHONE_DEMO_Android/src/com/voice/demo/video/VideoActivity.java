package com.voice.demo.video;

import kr.re.Dev.Bluetooth.AlertManager;
import kr.re.Dev.Bluetooth.OmmiBotManager;
import org.webrtc.videoengine.ViERenderer;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.CCP.phone.CameraInfo;
import com.hisun.phone.core.voice.Device;
import com.hisun.phone.core.voice.DeviceListener.Reason;
import com.hisun.phone.core.voice.model.CallStatisticsInfo;
import com.hisun.phone.core.voice.model.NetworkStatistic;
import com.hisun.phone.core.voice.util.Log4Util;
import com.voice.demo.CCPApplication;
import com.voice.demo.R;
import com.voice.demo.tools.CCPIntentUtils;
import com.voice.demo.ui.CCPHelper;
import com.voice.demo.voip.AudioVideoCallActivity;

public class VideoActivity extends AudioVideoCallActivity implements View.OnClickListener {
	private Button mVideoCancle;
	private ImageView mVideoIcon;
	private SurfaceView mVideoView;
	private FrameLayout mVideoLayout; // Remote Video
	private String mVoipAccount; // voip 账号
	private String mCurrentCallId;// 通话 ID
	CameraInfo[] cameraInfos;
	int numberOfCameras;
	private int mWidth;
	private int mHeight;
	private AlertManager mAlertManager;
	private OmmiBotManager mOmmiBotManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_video_activity);
		isIncomingCall = false;
		mCallType = Device.CallType.VIDEO;
		initProwerManager();
		enterIncallMode();
		initResourceRefs();
		mOmmiBotManager = new OmmiBotManager(this) {
			@Override
			protected void onSendTextMesage(String text) {
				super.onSendTextMesage(text);
				VideoActivity.this.onSendTextMesage(text);
			}
		};
		mOmmiBotManager.start();
		initialize();
		if (checkeDeviceHelper()) {
			cameraInfos = getDeviceHelper().getCameraInfo();
		}
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
		DisplayLocalSurfaceView();
		if (checkeDeviceHelper())
			mCurrentCallId = getDeviceHelper().makeCall(Device.CallType.VIDEO, mVoipAccount);
		if (TextUtils.isEmpty(mCurrentCallId)) {
			CCPApplication.getInstance().showToast(R.string.no_support_voip);
			Log4Util.d(CCPHelper.DEMO_TAG, "[CallOutActivity] Sorry, " + getString(R.string.no_support_voip) + " , Call failed. ");
			finish();
			return;
		}
		registerReceiver(new String[] { CCPIntentUtils.INTENT_P2P_ENABLED });
	}

	private void initResourceRefs() {
		mVideoIcon = (ImageView) findViewById(R.id.video_icon);
		mVideoCancle = (Button) findViewById(R.id.video_botton_cancle);
		mVideoCancle.setOnClickListener(this);
		mVideoView = (SurfaceView) findViewById(R.id.video_view);
		mVideoView.setVisibility(View.INVISIBLE);
		mLoaclVideoView = (RelativeLayout) findViewById(R.id.localvideo_view);
		mVideoLayout = (FrameLayout) findViewById(R.id.Video_layout);
		mVideoView.getHolder().setFixedSize(240, 320);
		if (checkeDeviceHelper())
			getDeviceHelper().setVideoView(mVideoView, null);
		SurfaceView localView = ViERenderer.CreateLocalRenderer(this);
		mLoaclVideoView.addView(localView);
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
	}

	@Override
	public void onReceiveTextMessage(String text) {
		super.onReceiveTextMessage(text);
		if (!TextUtils.isEmpty(text) && text.startsWith("receive:")) {
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
			
			mAlertManager.bettery(Integer.parseInt(sa[5]));
		}
	}

	/**
	 * Read parameters or previously saved state of this activity.
	 */
	private void initialize() {
		Intent intent = getIntent();
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			if (bundle == null) {
				finish();
				return;
			}
			// Video...
			mVoipAccount = bundle.getString(CCPApplication.VALUE_DIAL_VOIP_INPUT);
			if (mVoipAccount == null) {
				finish();
				return;
			}
		}
	}

	@Override
	protected void onDestroy() {
		mOmmiBotManager.stop();
		releaseWakeLock();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void doHandUpReleaseCall() {
		super.doHandUpReleaseCall();
		// Hang up the video call...
		Log4Util.d(CCPHelper.DEMO_TAG, "[VideoActivity] onClick: Voip talk hand up, CurrentCallId " + mCurrentCallId);
		try {
			if (mCurrentCallId != null && checkeDeviceHelper()) {
				getDeviceHelper().releaseCall(mCurrentCallId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!isConnect) {
			finish();
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
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.video_botton_cancle:
			doHandUpReleaseCall();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onCallAlerting(String callid) {
		super.onCallAlerting(callid);
		// 连接到对端用户，播放铃音
		Log4Util.d(CCPHelper.DEMO_TAG, "[VideoActivity] handleMessage: voip alerting!!");
		if (callid != null && callid.equals(mCurrentCallId)) {// 等待对方接受邀请...
			logText(getString(R.string.str_tips_wait_invited));
		}
	}

	@Override
	protected void onCallProceeding(String callid) {
		super.onCallProceeding(callid);
		// 连接到服务器
		Log4Util.d(CCPHelper.DEMO_TAG, "[VideoActivity] handleMessage: voip on call proceeding!!");
		if (callid != null && callid.equals(mCurrentCallId)) {
			logText(getString(R.string.voip_call_connect));
		}
	}

	@Override
	protected void onCallAnswered(String callid) {
		super.onCallAnswered(callid);
		// 对端应答
		Log4Util.d(CCPHelper.DEMO_TAG, "[VideoActivity] handleMessage: voip on call answered!!");
		if (callid != null && callid.equals(mCurrentCallId) && !isConnect) {
			initResVideoSuccess();
		}
		if (getCallHandler() != null) {
			getCallHandler().sendMessage(getCallHandler().obtainMessage(VideoActivity.WHAT_ON_CODE_CALL_STATUS));
		}
		getDeviceHelper().enableLoudsSpeaker(false);
	}

	@Override
	protected void onCallReleased(String callid) {
		super.onCallReleased(callid);
		// 远端挂断，本地挂断在onClick中处理
		Log4Util.d(CCPHelper.DEMO_TAG, "[VideoActivity] handleMessage: voip on call released!!");
		if (callid != null && callid.equals(mCurrentCallId)) {
			finishCalling();
		}
	}

	@Override
	protected void onMakeCallFailed(String callid, Reason reason) {
		super.onMakeCallFailed(callid, reason);
		Log4Util.d(CCPHelper.DEMO_TAG, "[VideoActivity] handleMessage: voip on call makecall failed!!");
		if (callid != null && callid.equals(mCurrentCallId)) {
			finishCalling(reason);
		}
	}

	@Override
	protected void handleNotifyMessage(Message msg) {
		super.handleNotifyMessage(msg);
		switch (msg.what) {
		case WHAT_ON_CODE_CALL_STATUS:
			if (!checkeDeviceHelper()) {
				return;
			}
			CallStatisticsInfo callStatistics = getDeviceHelper().getCallStatistics(Device.CallType.VIDEO);
			if (callStatistics != null) {
				int fractionLost = callStatistics.getFractionLost();
				int rttMs = callStatistics.getRttMs();
				NetworkStatistic trafficStats = getDeviceHelper().getNetworkStatistic(mCurrentCallId);
				if (trafficStats != null) {
					logText(getString(R.string.str_call_traffic_status, rttMs, (fractionLost / 255), trafficStats.getTxBytes() / 1024, trafficStats.getRxBytes() / 1024));
				} else {
					logText(getString(R.string.str_call_status, rttMs, (fractionLost / 255)));
				}
			}
			if (isConnect) {
				Message callMessage = getCallHandler().obtainMessage(WHAT_ON_CODE_CALL_STATUS);
				getCallHandler().sendMessageDelayed(callMessage, 4000);
			}
			break;
		// This call may be redundant, but to ensure compatibility system event more,
		// not only is the system call
		case CCPHelper.WHAT_ON_RECEIVE_SYSTEM_EVENTS:
			doHandUpReleaseCall();
		default:
			break;
		}
	}

	//
	private void initResVideoSuccess() {
		isConnect = true;
		mVideoLayout.setVisibility(View.VISIBLE);
		mVideoIcon.setVisibility(View.GONE);
		mVideoCancle.setVisibility(View.GONE);
		logText(getString(R.string.str_video_bottom_time, mVoipAccount.substring(mVoipAccount.length() - 3, mVoipAccount.length())));
	}

	/**
	 * 根据状态,修改按钮属性及关闭操作
	 * 
	 * @param reason
	 */
	private void finishCalling() {
		try {
			logText(R.string.voip_calling_finish);
			if (isConnect) {
				// set Chronometer view gone..
				mVideoLayout.setVisibility(View.GONE);
				mVideoIcon.setVisibility(View.VISIBLE);
				mLoaclVideoView.removeAllViews();
				mLoaclVideoView.setVisibility(View.GONE);
			} else {
				mVideoCancle.setEnabled(false);
			}
			// 3 second mis , finsh this activit ...
			getCallHandler().postDelayed(finish, 3000);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			isConnect = false;
		}
	}

	@Override
	@Deprecated
	protected void onCallVideoRatioChanged(String callid, String resolution) {
		super.onCallVideoRatioChanged(callid, resolution);
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
	protected void onCallVideoRatioChanged(String callid, int width, int height) {
		super.onCallVideoRatioChanged(callid, width, height);
		try {
			mWidth = width;
			mHeight = height;
			if (mVideoView != null) {
				// mVideoView.getHolder().setFixedSize(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
				LayoutParams layoutParams2 = (LayoutParams) mVideoView.getLayoutParams();
				layoutParams2.width = mWidth;
				layoutParams2.height = mHeight;
				mVideoView.setLayoutParams(layoutParams2);
				mVideoView.setVisibility(View.VISIBLE);
			}
			int[] decodeDisplayMetrics = decodeDisplayMetrics();
			if (mWidth >= decodeDisplayMetrics[0] || mHeight >= decodeDisplayMetrics[1]) {
				return;
			}
			doResizeView(decodeDisplayMetrics[0]);
		} catch (Exception e) {
		}
	}

	private void finishCalling(Reason reason) {
		try {
			mLoaclVideoView.removeAllViews();
			mLoaclVideoView.setVisibility(View.GONE);
			if (isConnect) {
				mVideoLayout.setVisibility(View.GONE);
				mVideoIcon.setVisibility(View.VISIBLE);
				isConnect = false;
			} else {
				mVideoCancle.setEnabled(false);
			}
			isConnect = false;
			getCallHandler().postDelayed(finish, 3000);
			if (reason == Reason.DECLINED || reason == Reason.BUSY) {
				getCallHandler().removeCallbacks(finish);
				logText(getString(R.string.str_video_call_end, mVoipAccount.substring(mVoipAccount.length() - 3, mVoipAccount.length())));
			} else {
				if (reason == Reason.CALLMISSED) {
					logText(getString(R.string.voip_calling_timeout));
				} else if (reason == Reason.MAINACCOUNTPAYMENT) {
					logText(getString(R.string.voip_call_fail_no_cash));
				} else if (reason == Reason.UNKNOWN) {
					logText(getString(R.string.voip_calling_finish));
				} else if (reason == Reason.NOTRESPONSE) {
					logText(getString(R.string.voip_call_fail));
				} else if (reason == Reason.VERSIONNOTSUPPORT) {
					logText(getString(R.string.str_video_not_support));
				} else if (reason == Reason.OTHERVERSIONNOTSUPPORT) {
					logText(getString(R.string.str_other_voip_not_support));
				} else {
					ThirdSquareError(reason);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Increased Voip P2P, direct error code:
	 * <p>
	 * Title: ThirdSquareError
	 * </p>
	 * <p>
	 * Description:
	 * </p>
	 * 
	 * @param reason
	 */
	private void ThirdSquareError(Reason reason) {
		if (reason == Reason.AUTHADDRESSFAILED) {// -- 700 第三方鉴权地址连接失败
			logText(getString(R.string.voip_call_fail_connection_failed_auth));
		} else if (reason == Reason.MAINACCOUNTPAYMENT) {// -- 701 第三方主账号余额不足
			logText(getString(R.string.voip_call_fail_no_pay_account));
		} else if (reason == Reason.MAINACCOUNTINVALID) { // -- 702 第三方应用ID未找到
			logText(getString(R.string.voip_call_fail_not_find_appid));
		} else if (reason == Reason.CALLERSAMECALLED) {// -- 704 第三方应用未上线限制呼叫已配置测试号码
			logText(getString(R.string.voip_call_fail_not_online_only_call));
		} else if (reason == Reason.SUBACCOUNTPAYMENT) {// -- 705 第三方鉴权失败，子账号余额
			logText(getString(R.string.voip_call_auth_failed));
		} else {
			logText(getString(R.string.voip_calling_network_instability));
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
