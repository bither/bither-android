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
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Created by songchenwen on 14-5-24.
 */
public class OverScrollableListView extends ListView {

    private int overScrollTopHeight = -1;
    private int overScrollBottomHeight = -1;
    private FrameLayout vExtraFooter;

    public OverScrollableListView(Context context) {
        super(context);
        setOverScrollMode(OVER_SCROLL_ALWAYS);
    }

    public OverScrollableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOverScrollMode(OVER_SCROLL_ALWAYS);
    }

    public OverScrollableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOverScrollMode(OVER_SCROLL_ALWAYS);
    }

    private void initExtraFooter() {
        if (vExtraFooter == null) {
            vExtraFooter = new FrameLayout(getContext());
            super.addFooterView(vExtraFooter, null, false);
        }
    }

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY,
                                   int scrollRangeX, int scrollRangeY, int maxOverScrollX,
                                   int maxOverScrollY, boolean isTouchEvent) {
        int newScrollY = scrollY + deltaY;
        if (newScrollY < 0) {
            maxOverScrollY = overScrollTopHeight < 0 ? getHeight() / 4 : overScrollTopHeight;
        } else if (newScrollY > scrollRangeY) {
            maxOverScrollY = overScrollBottomHeight < 0 ? getHeight() / 4 : overScrollBottomHeight;
        } else {
            maxOverScrollY = 0;
        }
        float overScrollRatio = 1;
        if (maxOverScrollY > 0 && isTouchEvent) {
            overScrollRatio = 1.0f - (float) Math.abs(newScrollY) / (float) maxOverScrollY;
            overScrollRatio = Math.abs(overScrollRatio);
        }
        return super.overScrollBy(deltaX, (int) (deltaY * overScrollRatio), scrollX, scrollY,
                scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (vExtraFooter != null && vExtraFooter.getLayoutParams() != null) {
            int height = getFooterViewHeight();
            vExtraFooter.getLayoutParams().height = height;
        }
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    public void addFooterView(View v, Object data, boolean isSelectable) {
        if (vExtraFooter != null) {
            removeFooterView(vExtraFooter);
            super.addFooterView(v, data, isSelectable);
            super.addFooterView(vExtraFooter);
        } else {
            super.addFooterView(v, data, isSelectable);
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        initExtraFooter();
    }

    public int getFooterViewHeight() {
        if (vExtraFooter == null || getAdapter() == null) {
            return 0;
        }
        if (getLastVisiblePosition() - getFirstVisiblePosition() + 1 < getAdapter().getCount()) {
            return 0;
        }
        int contentHeight = 0;
        for (int i = 0;
             i < getChildCount();
             i++) {
            View v = getChildAt(i);
            if (v != vExtraFooter) {
                contentHeight += v.getHeight();
            }
        }
        int height = Math.max(0, getHeight() - contentHeight);
        return height;
    }

    public void setOverScrollTopHeight(int topHeight) {
        this.overScrollTopHeight = topHeight;
    }

    public void setOverScrollBottomHeight(int bottomHeight) {
        this.overScrollBottomHeight = bottomHeight;
    }

}
