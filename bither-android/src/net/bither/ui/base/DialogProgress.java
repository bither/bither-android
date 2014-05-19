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

package net.bither.ui.base;

import net.bither.R;
import android.content.Context;
import android.widget.TextView;

public class DialogProgress extends CenterDialog {
	private TextView tv;
	private Thread thread;

	public DialogProgress(Context context, int msg) {
		this(context, context.getString(msg), null);
	}

	public DialogProgress(Context context, String msg) {
		this(context, msg, null);
	}

	public DialogProgress(Context context, int msg, Thread thread) {
		this(context, context.getString(msg), thread);
	}

	public DialogProgress(Context context, String msg, Thread thread) {
		super(context);
		setContentView(R.layout.dialog_progress);
		tv = (TextView) findViewById(R.id.tv);
		this.setCanceledOnTouchOutside(false);
		setMessage(msg);
		this.thread = thread;
	}

	public void setMessage(int message) {
		setMessage(getContext().getString(message));
	}

	public void setMessage(String message) {
		tv.setText(message);
	}

	public void setThread(Thread thread) {
		this.thread = thread;
	}

	@Override
	public void dismiss() {
		if (thread != null && thread.isAlive()) {
			thread.interrupt();
		}
		thread = null;
		super.dismiss();
	}

}
