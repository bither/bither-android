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
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class OverScrollableViewPager extends ViewPager {

	private int overScrollLeftWidth = -1;
	private int overScrollRightWidth = -1;

	public OverScrollableViewPager(Context context) {
		super(context);
		setOverScrollMode(OVER_SCROLL_ALWAYS);
	}

	public OverScrollableViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOverScrollMode(OVER_SCROLL_ALWAYS);
	}

	@Override
	protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
			int scrollY, int scrollRangeX, int scrollRangeY,
			int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
		int newScrollX = scrollX + deltaX;
		if (newScrollX < 0) {
			maxOverScrollX = overScrollLeftWidth < 0 ? getWidth() / 4
					: overScrollLeftWidth;
		} else if (newScrollX > scrollRangeX) {
			maxOverScrollX = overScrollRightWidth < 0 ? getWidth() / 4
					: overScrollRightWidth;
		} else {
			maxOverScrollX = 0;
		}
		float overScrollRatio = 1;
		if (maxOverScrollX > 0 && isTouchEvent) {
			overScrollRatio = 1.0f - (float) Math.abs(newScrollX)
					/ (float) maxOverScrollX;
			overScrollRatio = Math.abs(overScrollRatio);
		}
		return super.overScrollBy((int) (deltaX * overScrollRatio), deltaY,
				scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX,
				maxOverScrollY, isTouchEvent);
	}

	public void setOverScrollLeftWidth(int topHeight) {
		this.overScrollLeftWidth = topHeight;
	}

	public void setOverScrollRightWidth(int bottomHeight) {
		this.overScrollRightWidth = bottomHeight;
	}

}
