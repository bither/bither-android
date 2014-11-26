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
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import net.bither.R;
import net.bither.util.LogUtil;


public class SyncProgressView extends FrameLayout {

	private ImageView iv;
	private double progress;

	public SyncProgressView(Context context) {
		super(context);
		initView();
	}

	public SyncProgressView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public SyncProgressView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}
                
	private void initView() {
		removeAllViews();
		iv = new ImageView(getContext());
		iv.setBackgroundResource(R.drawable.sync_progress_foreground);
		addView(iv, 0, LayoutParams.WRAP_CONTENT);
	}

	public void setProgress(final double progress) {
		removeCallbacks(delayedShowProgress);
		removeCallbacks(delayHide);
		this.progress = progress;
		if (progress >= 0 && progress < 1) {
			if (getWidth() <= 0) {
				postDelayed(delayedShowProgress, 100);
				return;
			}
			LogUtil.d("progress", "progress:" + progress);
			double p = Math.max(Math.min(progress, 1.0f), 0.1f);
			iv.getLayoutParams().width = (int) (p * getWidth());
			iv.requestLayout();
			setVisibility(View.VISIBLE);
		} else {
			if (getVisibility() == VISIBLE) {
				post(delayHide);
			} else {
				setVisibility(View.GONE);
			}
		}
	}

	private Runnable delayHide = new Runnable() {
		@Override
		public void run() {
			int width = iv.getLayoutParams().width + getWidth() / 10;
			if (width >= getWidth()) {
				setVisibility(View.GONE);
			} else {
				iv.getLayoutParams().width = width;
				iv.requestLayout();
				postDelayed(delayHide, 100);
				LogUtil.d("progress", "delayhide");
			}
		}
	};

	private Runnable delayedShowProgress = new Runnable() {

		@Override
		public void run() {
			setVisibility(View.VISIBLE);
			setProgress(progress);
			LogUtil.d("progress", "postDelayed:" + progress);
		}
	};

}
