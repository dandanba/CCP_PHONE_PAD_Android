/*
 *  Copyright (c) 2013 The CCP project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a Beijing Speedtong Information Technology Co.,Ltd license
 *  that can be found in the LICENSE file in the root of the web site.
 *
 *   http://www.cloopen.com
 *
 *  An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.voice.demo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import com.voice.demo.R;
import com.voice.demo.tools.preference.CCPPreferenceSettings;
import com.voice.demo.tools.preference.CcpPreferences;
import com.voice.demo.ui.experience.AccountChooseActivity;
import com.voice.demo.ui.experience.ExperienceLogin;

/**
 * 
 * <p>
 * Title: LauncherUI.java
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2013
 * </p>
 * <p>
 * Company: http://www.cloopen.com
 * </p>
 * 
 * @author Jorstin Chan
 * @date 2013-11-27
 * @version 3.6
 */
public class LauncherUI extends Activity {
	private static final int STOPSPLASH = 0;
	// time in milliseconds
	private static final long SPLASHTIME = 3000;
	private Handler splashHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case STOPSPLASH:
				if (checkAutoLogin()) {
					startActivity(new Intent(LauncherUI.this, AccountChooseActivity.class));
					return;
				}
				startActivity(new Intent(LauncherUI.this, ExperienceLogin.class));
				finish();
			}
			super.handleMessage(msg);
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutId());
		Message msg = new Message();
		msg.what = STOPSPLASH;
		splashHandler.sendMessageDelayed(msg, SPLASHTIME);
	}
	// @Override
	protected int getLayoutId() {
		return R.layout.launcher;
	}
	/**
	 * @return
	 */
	boolean checkAutoLogin() {
		String userName = CcpPreferences.getSharedPreferences().getString(CCPPreferenceSettings.SETTING_USERNAME_MAIL.getId(), (String) CCPPreferenceSettings.SETTING_USER_PASSWORD.getDefaultValue());
		String userpas = CcpPreferences.getSharedPreferences().getString(CCPPreferenceSettings.SETTING_USER_PASSWORD.getId(), (String) CCPPreferenceSettings.SETTING_USER_PASSWORD.getDefaultValue());
		if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(userpas)) {
			return false;
		}
		return true;
	}
}
