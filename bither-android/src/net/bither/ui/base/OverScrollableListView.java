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
import android.widget.ListView;

public class OverScrollableListView extends ListView {

	private int overScrollTopHeight = -1;
	private int overScrollBottomHeight = -1;

	public OverScrollableListView(Context context) {
		super(context);
		setOverScrollMode(OVER_SCROLL_ALWAYS);
	}

	public OverScrollableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOverScrollMode(OVER_SCROLL_ALWAYS);
	}

	public OverScrollableListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		setOverScrollMode(OVER_SCROLL_ALWAYS);
	}

	@Override
	protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
			int scrollY, int scrollRangeX, int scrollRangeY,
			int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
		int newScrollY = scrollY + deltaY;
		if (newScrollY < 0) {
			maxOverScrollY = overScrollTopHeight < 0 ? getHeight() / 4
					: overScrollTopHeight;
		} else if (newScrollY > scrollRangeY) {
			maxOverScrollY = overScrollBottomHeight < 0 ? getHeight() / 4
					: overScrollBottomHeight;
		} else {
			maxOverScrollY = 0;
		}
		float overScrollRatio = 1;
		if (maxOverScrollY > 0 && isTouchEvent) {
			overScrollRatio = 1.0f - (float) Math.abs(newScrollY)
					/ (float) maxOverScrollY;
			overScrollRatio = Math.abs(overScrollRatio);
		}
		return super.overScrollBy(deltaX, (int) (deltaY * overScrollRatio),
				scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX,
				maxOverScrollY, isTouchEvent);
	}

	public void setOverScrollTopHeight(int topHeight) {
		this.overScrollTopHeight = topHeight;
	}

	public void setOverScrollBottomHeight(int bottomHeight) {
		this.overScrollBottomHeight = bottomHeight;
	}

}
