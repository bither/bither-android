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
import net.bither.util.ImageManageUtil;
import net.bither.util.UIUtil;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class DialogWithArrow extends Dialog {
	private static final int ArrowWidth = UIUtil.dip2pix(19);
	private static final int HorizontalPadding = UIUtil.dip2pix(16);
	private static final int MinHorizontalMargin = UIUtil.dip2pix(2);
	private static final int Offset = UIUtil.dip2pix(2);
	protected Window mWindow;
	protected FrameLayout container;
	private LinearLayout.LayoutParams lpArrowTop;
	private ImageView ivArrowTop;
	private ImageView ivArrowBottom;
	private LinearLayout.LayoutParams lpArrowBottom;
	protected LayoutInflater inflater;

	public DialogWithArrow(Context context) {
		super(context, R.style.tipsDialog);
		this.setCanceledOnTouchOutside(true);
		this.mWindow = this.getWindow();
		mWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		mWindow.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		mWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		mWindow.getAttributes().dimAmount = 0.5f;
		mWindow.getAttributes().gravity = Gravity.LEFT + Gravity.TOP;
		super.setContentView(R.layout.dialog_with_arrow);
		this.container = (FrameLayout) findViewById(R.id.fl_center_dialog_container);
		ivArrowTop = (ImageView) findViewById(R.id.iv_arrow_top);
		lpArrowTop = (LinearLayout.LayoutParams) ivArrowTop.getLayoutParams();
		ivArrowBottom = (ImageView) findViewById(R.id.iv_arrow_bottom);
		lpArrowBottom = (LinearLayout.LayoutParams) ivArrowBottom
				.getLayoutParams();
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

	public void show(View fromView) {
		int[] location = new int[2];
		fromView.getLocationInWindow(location);
		Activity activity = (Activity) fromView.getContext();
		int statusBarHeight = ImageManageUtil.getStatusBarHeight(activity
				.getWindow());
		int x = location[0] + (fromView.getWidth() / 2);
		int y = location[1] + fromView.getHeight() - statusBarHeight;
		boolean top = true;
		if (y + Offset + getSuggestHeight() > UIUtil.getScreenHeight()
				- statusBarHeight - Offset) {
			top = false;
			y = UIUtil.getScreenHeight() - location[1];
		}
		show(x, y, top);
	}

	public void show(int x, int y, boolean top) {
		if (top) {
			ivArrowTop.setVisibility(View.VISIBLE);
			ivArrowBottom.setVisibility(View.GONE);
			mWindow.getAttributes().gravity = Gravity.LEFT + Gravity.TOP;
		} else {
			ivArrowBottom.setVisibility(View.VISIBLE);
			ivArrowTop.setVisibility(View.GONE);
			mWindow.getAttributes().gravity = Gravity.LEFT + Gravity.BOTTOM;
		}
		container.measure(0, 0);
		int windowWidth = container.getMeasuredWidth();
		int windowX = Math.max(x - windowWidth / 2, MinHorizontalMargin);
		windowX = Math.max(windowX, MinHorizontalMargin);
		windowX = Math.min(windowX, UIUtil.getScreenWidth()
				- MinHorizontalMargin - windowWidth);
		int arrowLeft = x - windowX - ArrowWidth / 2;
		arrowLeft = Math.max(arrowLeft, HorizontalPadding);
		arrowLeft = Math.min(arrowLeft, windowWidth - HorizontalPadding
				- ArrowWidth);
		if (top) {
			lpArrowTop.leftMargin = arrowLeft;
		} else {
			lpArrowBottom.leftMargin = arrowLeft;
		}
		mWindow.getAttributes().x = windowX;
		mWindow.getAttributes().y = y + Offset;
		show();
	}

	public int getSuggestHeight() {
		return UIUtil.dip2pix(200);
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
