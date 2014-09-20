/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.ui.base.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.utils.Utils;
import net.bither.util.StringUtil;

public class DialogConfirmTask extends CenterDialog {

	private TextView tvOk;
	private TextView tvCancel;
	private TextView tvMessage;
	private Object mTask;
	private Object mCancel;

	public DialogConfirmTask(Context context, CharSequence message, Object task) {
		this(context, message, task, null);
	}

	public DialogConfirmTask(Context context, CharSequence message,
			Object task, Object cancelTask) {
		this(context, message, null, null, task, cancelTask);
	}

	public DialogConfirmTask(Context context, CharSequence message,
			String okText, String cancelText, Object task) {
		this(context, message, okText, cancelText, task, null);
	}

	public DialogConfirmTask(Context context, CharSequence message,
			String okText, String cancelText, Object task, Object cancelTask) {
		super(context);
		mTask = task;
		mCancel = cancelTask;
		this.setContentView(R.layout.dialog_task_confirm);
		initView();
		tvMessage.setText(message);
		if (!Utils.isEmpty(okText)) {
			tvOk.setText(okText);
		}
		if (!Utils.isEmpty(cancelText)) {
			tvCancel.setText(cancelText);
		}
	}

	private void initView() {
		tvOk = (TextView) findViewById(R.id.tv_ok);
		tvCancel = (TextView) findViewById(R.id.tv_cancel);
		tvMessage = (TextView) findViewById(R.id.tv_confirm_message);
		tvOk.setOnClickListener(okClick);
		tvCancel.setOnClickListener(cancelClick);
	}

	private android.view.View.OnClickListener cancelClick = new View.OnClickListener() {
		public void onClick(View v) {
			DialogConfirmTask.this.dismiss();
			if (mCancel != null) {
				if (Runnable.class.isInstance(mCancel)) {
					new Thread((Runnable) mCancel).start();
				}
			}
		}
	};

	private android.view.View.OnClickListener okClick = new View.OnClickListener() {
		public void onClick(View v) {
			DialogConfirmTask.this.dismiss();
			if (Runnable.class.isInstance(mTask)) {
				new Thread((Runnable) mTask).start();
			}
		}
	};
}
