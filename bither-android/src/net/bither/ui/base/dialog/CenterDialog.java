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

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;

import net.bither.R;

public class CenterDialog extends Dialog {
	protected Window mWindow;
	protected FrameLayout container;
	protected LayoutInflater inflater;

	public CenterDialog(Context context) {
		super(context, R.style.tipsDialog);
		this.setCanceledOnTouchOutside(true);
		this.mWindow = this.getWindow();
		mWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		mWindow.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		mWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		mWindow.getAttributes().dimAmount = 0.5f;
		super.setContentView(R.layout.center_dialog_container);
		this.container = (FrameLayout) findViewById(R.id.fl_center_dialog_container);
		this.inflater = LayoutInflater.from(getContext());
	}

	@Override
	public void setContentView(int layoutResID) {
		this.container.addView(inflater.inflate(layoutResID, null),
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	}

	@Override
	public void setContentView(View view,
			android.view.ViewGroup.LayoutParams params) {
		this.container.addView(view, params);
	}

	@Override
	public void show() {
		try {
			super.show();
		} catch (Exception e) {
		}
	}

	@Override
	public void dismiss() {
		try {
			super.dismiss();
		} catch (Exception e) {

		}
	}
}
