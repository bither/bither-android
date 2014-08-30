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
import android.widget.TextView;

import net.bither.R;

public class ProgressDialog extends CenterDialog {
	private Object task;

	private String message;
	private TextView tvMessage;

	public ProgressDialog(Context context, String message, Object task) {
		super(context);
		this.task = task;
		this.message = message;
		this.setContentView(R.layout.progress_dialog);
		this.setCanceledOnTouchOutside(false);
		this.setCancelable(false);
		initView();
		initContent();
	}

	private void initView() {
		tvMessage = (TextView) findViewById(R.id.tv_message);
	}

	private void initContent() {
		tvMessage.setText(message);
	}

	public void dismiss() {
		try {
			if (task != null) {
				if (Thread.class.isInstance(task)) {
					((Thread) task).interrupt();
				}
			}
			super.dismiss();
		} catch (Exception e) {
		}
	}

	public void show() {
		try {
			super.show();
		} catch (Exception e) {
		}
	}
}
