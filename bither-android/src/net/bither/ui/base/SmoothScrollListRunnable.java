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

import android.widget.AbsListView;

public class SmoothScrollListRunnable implements Runnable {
	private static final int GapDelay = 30;
	private int smoothLength = 10;
	private int smoothDelay = 50;
	private AbsListView lv;
	private int destPosition;
	private Runnable afterRun;

	public SmoothScrollListRunnable(AbsListView lv, int position,
			Runnable afterRun) {
		this.lv = lv;
		this.destPosition = position;
		this.afterRun = afterRun;
	}

	public SmoothScrollListRunnable(AbsListView lv, int position,
			Runnable afterRun, int smoothLength, int smoothDelay) {
		this(lv, position, afterRun);
		this.smoothDelay = smoothDelay;
		this.smoothLength = smoothLength;
	}

	@Override
	public void run() {
		if (lv.getFirstVisiblePosition() <= destPosition
				&& lv.getLastVisiblePosition() >= destPosition) {
			lv.smoothScrollToPosition(destPosition);
			if (afterRun != null) {
				lv.post(afterRun);
			}
		} else {
			if (lv.getFirstVisiblePosition() > destPosition + smoothLength) {
				lv.setSelection(destPosition + smoothLength);
			}
			if (lv.getLastVisiblePosition() < destPosition - smoothLength) {
				lv.setSelection(destPosition - smoothLength);
			}
			lv.postDelayed(new Runnable() {
				@Override
				public void run() {
					lv.smoothScrollToPosition(destPosition);
					lv.postDelayed(new SmoothScrollListRunnable(lv,
							destPosition, afterRun, smoothLength, smoothDelay),
							smoothDelay);
				}
			}, GapDelay);
		}
	}
}
