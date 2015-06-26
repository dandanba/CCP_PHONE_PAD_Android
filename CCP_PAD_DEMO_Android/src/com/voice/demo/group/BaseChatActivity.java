package com.voice.demo.group;

import java.sql.SQLException;
import java.util.ArrayList;
import kr.re.Dev.ArduinoEcho.BTManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.voice.demo.CCPApplication;
import com.voice.demo.R;
import com.voice.demo.group.model.IMChatMessageDetail;
import com.voice.demo.sqlite.CCPSqliteManager;
import com.voice.demo.tools.CCPIntentUtils;
import com.voice.demo.ui.CCPBaseActivity;
import com.voice.demo.ui.CCPHelper;

public abstract class BaseChatActivity extends CCPBaseActivity {
	public static final String KEY_GROUP_ID = "groupId";
	public static final String KEY_MESSAGE_ID = "messageId";
	public static final int CHAT_MODE_IM_POINT = 0x1;
	public static final int CHAT_MODE_IM_GROUP = 0x2;
	private static final String USER_DATA = "yuntongxun.com";
	public String mGroupId;
	private android.os.Handler mIMChatHandler = new android.os.Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle b = null;
			int reason = -1;
			if (msg.obj instanceof Bundle) {
				b = (Bundle) msg.obj;
			}
			switch (msg.what) {
			// This call may be redundant, but to ensure compatibility system event more,
			// not only is the system call
			case CCPHelper.WHAT_ON_RECEIVE_SYSTEM_EVENTS:
				onPause();
				break;
			default:
				break;
			}
		}
	};
	private BTManager mBTManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerReceiver(new String[] { CCPIntentUtils.INTENT_IM_RECIVE, CCPIntentUtils.INTENT_REMOVE_FROM_GROUP, CCPIntentUtils.INTENT_DELETE_GROUP_MESSAGE });
		CCPHelper.getInstance().setHandler(mIMChatHandler);
		if (checkeDeviceHelper()) {
			initialize(savedInstanceState);
		}
		mBTManager = new BTManager() {
			@Override
			public void receiveStringData(String data) {
				super.receiveStringData(data);
				onSendTextMesage(data);
			}
		};
		if (mBTManager.create(getApplicationContext())) {
			mBTManager.initDeviceListDialog(this);
			mBTManager.showDeviceListDialog();
			mBTManager.resume(getApplicationContext());
		}
	}

	private void initialize(Bundle savedInstanceState) {
		// Read parameters or previously saved state of this activity.
		mGroupId = getIntent().getStringExtra(KEY_GROUP_ID);
	}

	public void onReceiveTextMessage(String text) {
		Log.i("Chat", "receive<--------" + text);
		if (mBTManager != null) {
			mBTManager.sendStringData(text);
		}
	}

	public void onSendTextMesage(String text) {
		if (TextUtils.isEmpty(text)) {
			return;
		}
		Log.i("Chat", "send-------->" + text);
		IMChatMessageDetail chatMessageDetail = IMChatMessageDetail.getGroupItemMessage(IMChatMessageDetail.TYPE_MSG_TEXT, IMChatMessageDetail.STATE_IM_SENDING, mGroupId);
		chatMessageDetail.setMessageContent(text);
		if (!checkeDeviceHelper()) {
			return;
		}
		try {
			String uniqueID = getDeviceHelper().sendInstanceMessage(mGroupId, text.toString(), null, null);
			if (TextUtils.isEmpty(uniqueID)) {
				CCPApplication.getInstance().showToast(R.string.toast_send_group_message_failed);
				chatMessageDetail.setImState(IMChatMessageDetail.STATE_IM_SEND_FAILED);
				return;
			}
			chatMessageDetail.setMessageId(uniqueID);
			chatMessageDetail.setUserData(USER_DATA);
			CCPSqliteManager.getInstance().insertIMMessage(chatMessageDetail);
			sendbroadcast();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		text = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		// mBTManager.resume(getApplicationContext());
		updateReadStatus();
	}

	private void updateReadStatus() {
		try {
			CCPSqliteManager.getInstance().updateIMMessageUnreadStatusToReadBySessionId(mGroupId, IMChatMessageDetail.STATE_READED);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mBTManager != null) {
			mBTManager.destroy();
			mBTManager = null;
		}
		if (mIMChatHandler != null) {
			mIMChatHandler = null;
		}
	}

	// @Override
	// protected void onPause() {
	// super.onPause();
	// mBTManager.pause(getApplicationContext());
	// }
	private void sendbroadcast() {
		Intent intent = new Intent(CCPIntentUtils.INTENT_IM_RECIVE);
		intent.putExtra(KEY_GROUP_ID, mGroupId);
		sendBroadcast(intent);
	}

	class IMListyncTask extends AsyncTask<String, Void, ArrayList<IMChatMessageDetail>> {
		boolean isReceiveNewMessage = false;

		@Override
		protected ArrayList<IMChatMessageDetail> doInBackground(String... params) {
			if (params != null && params.length > 0) {
				try {
					if (params.length > 1) {
						// new Message .
						ArrayList<IMChatMessageDetail> newImMessages = CCPSqliteManager.getInstance().queryNewIMMessagesBySessionId(params[0]);
						CCPSqliteManager.getInstance().updateIMMessageUnreadStatusToRead(newImMessages, IMChatMessageDetail.STATE_READED);
						isReceiveNewMessage = true;
						return newImMessages;
					}
					updateReadStatus();
					return CCPSqliteManager.getInstance().queryIMMessagesBySessionId(params[0]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(ArrayList<IMChatMessageDetail> result) {
			super.onPostExecute(result);
			if (result != null && !result.isEmpty()) {
				final IMChatMessageDetail detail = result.get(result.size() - 1);
				if (!USER_DATA.equalsIgnoreCase(detail.getUserData())) {
					onReceiveTextMessage(detail.getMessageContent());
				}
			}
		}
	}

	@Override
	protected void onReceiveBroadcast(Intent intent) {
		super.onReceiveBroadcast(intent);
		if (intent == null) {
			return;
		}
		if (CCPIntentUtils.INTENT_IM_RECIVE.equals(intent.getAction()) || CCPIntentUtils.INTENT_DELETE_GROUP_MESSAGE.equals(intent.getAction())) {
			// update UI...
			// new IMListyncTask().execute();
			if (intent.hasExtra(KEY_GROUP_ID)) {
				String sender = intent.getStringExtra(KEY_GROUP_ID);
				// receive new message ,then load this message set adapter of listView.
				String newMessageId = null;
				if (intent.hasExtra(KEY_MESSAGE_ID)) {
					newMessageId = intent.getStringExtra(KEY_MESSAGE_ID);
				}
				if (!TextUtils.isEmpty(sender) && sender.equals(mGroupId)) {
					if (TextUtils.isEmpty(newMessageId)) {
						try {
							new IMListyncTask().execute(mGroupId);
						} catch (Exception e) {
							Log.i("Chat", "===地跪了吧====" + mGroupId);
						}
					} else {
						new IMListyncTask().execute(mGroupId, newMessageId);
					}
				}
			}
		} else if (CCPIntentUtils.INTENT_REMOVE_FROM_GROUP.equals(intent.getAction())) {
			// remove from group ...
			this.finish();
		}
	}

	@Override
	public void addNotificatoinToView(CharSequence text) {
		logText(text.toString());
	}

	public void logText(String text) {
		Log.i("cpp", text);
	}

	public void logText(int text) {
		Log.i("cpp", getString(text));
	}
}
