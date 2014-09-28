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

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.util.ImageManageUtil;
import net.bither.qrcode.Qr;
import net.bither.util.UIUtil;

public class ShowFullScreenQrView extends FrameLayout {
	private static final int AnimationDuration = 250;
	private int statusBarHeight;
	private int screenWidth;
	private int screenHeight;
	private View vMask;
	private ImageView ivPlaceHolder;
	private FrameLayout flImages;
	private ImageView iv;
	private View vFrom;

	public ShowFullScreenQrView(Context context) {
		super(context);
		firstInit();
	}

	public ShowFullScreenQrView(Context context, AttributeSet attrs) {
		super(context, attrs);
		firstInit();
	}

	public ShowFullScreenQrView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		firstInit();
	}

	private void firstInit() {
		statusBarHeight = ImageManageUtil
				.getStatusBarHeight(BitherApplication.hotActivity.getWindow());
		screenHeight = UIUtil.getScreenHeight();
		screenWidth = UIUtil.getScreenWidth();
	}

	private void initView() {
		removeAllViews();
		LayoutInflater.from(getContext()).inflate(
				R.layout.layout_show_full_screen_qr, this);
		vMask = findViewById(R.id.v_mask);
		iv = (ImageView) findViewById(R.id.iv_large_photo);
		ivPlaceHolder = (ImageView) findViewById(R.id.iv_place_holder);
		flImages = (FrameLayout) findViewById(R.id.fl_images);
		flImages.getLayoutParams().width = screenWidth;
		flImages.getLayoutParams().height = screenWidth;
		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hideToFromView();
			}
		});
	}

	public void showPicFromView(String content, View v, Bitmap placeHolder) {
		vFrom = v;
		int[] location = new int[2];
		v.getLocationInWindow(location);
		int x = location[0];
		int y = location[1];
		int width = v.getWidth();
		showPicFromPosition(content, placeHolder, x, y, width);
	}

	public void showPicFromPosition(final String content,
			final Bitmap placeHolder, final int x, final int y, final int width) {
		initView();
		setVisibility(View.VISIBLE);
		iv.setVisibility(View.INVISIBLE);
		ivPlaceHolder.setImageBitmap(placeHolder);
		final int size = Math.min(screenHeight, screenWidth);
		new Thread() {
			public void run() {
				final Bitmap bmp = Qr.bitmap(content, size);
				post(new Runnable() {
					@Override
					public void run() {
						iv.setImageBitmap(bmp);
						iv.setVisibility(View.VISIBLE);
					}
				});
			};
		}.start();
		AlphaAnimation animAlpha = new AlphaAnimation(0, 1);
		animAlpha.setDuration(AnimationDuration);
		vMask.startAnimation(animAlpha);
		int toX = 0;
		int toY = (screenHeight - statusBarHeight - screenWidth) / 2
				+ statusBarHeight;
		int toWidth = UIUtil.getScreenWidth();
		float scale = (float) width / (float) toWidth;
		ScaleAnimation animScale = new ScaleAnimation(scale, 1, scale, 1);
		animScale.setDuration(AnimationDuration);
		TranslateAnimation animTrans = new TranslateAnimation(x - toX, 0, y
				- toY, 0);
		animTrans.setDuration(AnimationDuration);
		AnimationSet animSet = new AnimationSet(true);
		animSet.setFillBefore(true);
		animSet.setDuration(AnimationDuration);
		animSet.addAnimation(animScale);
		animSet.addAnimation(animTrans);
		flImages.startAnimation(animSet);
	}

	public void hideToFromView() {
		if (vFrom != null) {
			AlphaAnimation animAlpha = new AlphaAnimation(1, 0);
			animAlpha.setDuration(AnimationDuration);
			vMask.startAnimation(animAlpha);
			int x = 0;
			int y = (screenHeight - statusBarHeight - screenWidth) / 2
					+ statusBarHeight;
			int[] location = new int[2];
			vFrom.getLocationInWindow(location);
			int toX = location[0];
			int toY = location[1];
			float scale = (float) vFrom.getWidth()
					/ (float) UIUtil.getScreenWidth();
			ScaleAnimation animScale = new ScaleAnimation(1, scale, 1, scale);
			animScale.setDuration(AnimationDuration);
			TranslateAnimation animTrans = new TranslateAnimation(0, toX - x,
					0, toY - y);
			animTrans.setDuration(AnimationDuration);
			AnimationSet animSet = new AnimationSet(true);
			animSet.addAnimation(animScale);
			animSet.addAnimation(animTrans);
			flImages.startAnimation(animSet);
			postDelayed(new Runnable() {
				@Override
				public void run() {
					setVisibility(View.GONE);
				}
			}, AnimationDuration);
		} else {
			setVisibility(View.GONE);
		}
	}

	public void setVisibility(int visibility) {
		super.setVisibility(visibility);
		if (visibility != VISIBLE) {
			iv.setImageBitmap(null);
			iv.setTag(null);
			ivPlaceHolder.setImageBitmap(null);
			vFrom = null;
		}
	};

}
