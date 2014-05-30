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
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

public class PinnedHeaderExpandableListView extends ExpandableListView
		implements OnScrollListener {

	private int overScrollTopHeight = -1;
	private int overScrollBottomHeight = -1;

	public PinnedHeaderExpandableListView(Context context) {
		super(context);
		setOnScrollListener(this);
		setOverScrollMode(OVER_SCROLL_ALWAYS);
	}

	public PinnedHeaderExpandableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnScrollListener(this);
		setOverScrollMode(OVER_SCROLL_ALWAYS);
	}

	public PinnedHeaderExpandableListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		setOnScrollListener(this);
		setOverScrollMode(OVER_SCROLL_ALWAYS);
	}

	public interface PinnedExpandableListViewAdapter {
		
		public static final int PINNED_HEADER_GONE = 0;
		
		public static final int PINNED_HEADER_VISIBLE = 1;
		
		public static final int PINNED_HEADER_PUSHED_UP = 2;

		public int getPinnedHeaderState(int groupPosition, int childPosition);

		public void configurePinnedHeader(View header, int groupPosition,
				int childPosition, int alpha);

	}

	private static final int MAX_ALPHA = 255;

	private PinnedExpandableListViewAdapter mAdapter;
	private View mHeaderView;
	private boolean mHeaderVisible;
	private int mHeaderViewWidth;
	private int mHeaderViewHeight;

	public void setPinnedHeaderView(View view) {
		mHeaderView = view;
		if (mHeaderView != null) {
			setFadingEdgeLength(0);
			mHeaderView.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						final long flatPos = getExpandableListPosition(getFirstVisiblePosition());
						final int groupPos = ExpandableListView
								.getPackedPositionGroup(flatPos);
						collapseGroup(groupPos);
					}
					return true;
				}
			});
		}
		requestLayout();
	}

	@Override
	public void setAdapter(ExpandableListAdapter adapter) {
		super.setAdapter(adapter);
		mAdapter = (PinnedExpandableListViewAdapter) adapter;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (mHeaderView != null) {
			try {
				measureChild(mHeaderView, widthMeasureSpec, heightMeasureSpec);
				mHeaderViewWidth = mHeaderView.getMeasuredWidth();
				mHeaderViewHeight = mHeaderView.getMeasuredHeight();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private int mOldState = -1;

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		final long flatPostion = getExpandableListPosition(getFirstVisiblePosition());
		final int groupPos = ExpandableListView
				.getPackedPositionGroup(flatPostion);
		final int childPos = ExpandableListView
				.getPackedPositionChild(flatPostion);
		int state = mAdapter.getPinnedHeaderState(groupPos, childPos);
		
		if (mHeaderView != null && mAdapter != null && state != mOldState) {
			mOldState = state;
			mHeaderView.layout(0, 0, mHeaderViewWidth, mHeaderViewHeight);
		}

		configureHeaderView(groupPos, childPos);
	}

	private int groupPosition = -1;
	private int childPosition = -1;

	public void configureHeaderView(int groupPosition, int childPosition) {
		if (mHeaderView == null || mAdapter == null) {
			return;
		}
		this.groupPosition = groupPosition;
		this.childPosition = childPosition;
		final int state = mAdapter.getPinnedHeaderState(groupPosition,
				childPosition);
		switch (state) {
		case PinnedExpandableListViewAdapter.PINNED_HEADER_GONE: {
			mHeaderVisible = false;
			break;
		}

		case PinnedExpandableListViewAdapter.PINNED_HEADER_VISIBLE: {
			mAdapter.configurePinnedHeader(mHeaderView, groupPosition,
					childPosition, MAX_ALPHA);
			if (mHeaderView.getTop() != 0) {
				mHeaderView.layout(0, 0, mHeaderViewWidth, mHeaderViewHeight);
			}
			mHeaderVisible = true;
			break;
		}

		case PinnedExpandableListViewAdapter.PINNED_HEADER_PUSHED_UP: {
			final View firstView = getChildAt(0);
			if (firstView == null) {
				break;
			}
			int bottom = firstView.getBottom();
			int headerHeight = mHeaderView.getHeight();
			int y;
			int alpha;
			if (bottom < headerHeight) {
				y = bottom - headerHeight;
				alpha = MAX_ALPHA * (headerHeight + y) / headerHeight;
			} else {
				y = 0;
				alpha = MAX_ALPHA;
			}
			mAdapter.configurePinnedHeader(mHeaderView, groupPosition,
					childPosition, alpha);
			if (mHeaderView.getTop() != y) {
				mHeaderView.layout(0, y, mHeaderViewWidth, mHeaderViewHeight
						+ y);
			}
			mHeaderVisible = true;
			break;
		}

		default:
			break;
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		
		if (mHeaderVisible) {
			drawChild(canvas, mHeaderView, getDrawingTime());
		}
	}

	private float mDownX;
	private float mDownY;

	private static final float FINGER_WIDTH = 20;

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean result = false;
		if (mHeaderVisible) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mDownX = ev.getX();
				mDownY = ev.getY();
				if (mDownX <= mHeaderViewWidth && mDownY <= mHeaderViewHeight) {
					mHeaderView.dispatchTouchEvent(ev);
					result = true;
				}
				break;
			case MotionEvent.ACTION_UP:
				float x = ev.getX();
				float y = ev.getY();

				float offsetX = Math.abs(x - mDownX);
				float offsetY = Math.abs(y - mDownY);
				
				if (x <= mHeaderViewWidth && y <= mHeaderViewHeight
						&& offsetX <= FINGER_WIDTH && offsetY <= FINGER_WIDTH) {
					mHeaderView.dispatchTouchEvent(ev);
					result = true;
				}
				break;
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_CANCEL:
				if (mDownX <= mHeaderViewWidth && mDownY <= mHeaderViewHeight) {
					mHeaderView.dispatchTouchEvent(ev);
					result = true;
				}
				break;
			default:
				break;
			}
		}
		if (groupPosition >= 0 && ev.getX() <= mHeaderViewWidth
				&& ev.getY() <= mHeaderViewHeight) {
			configureHeaderView(groupPosition, childPosition);
			requestLayout();
		}
		if (result) {
			return true;
		}
		return super.dispatchTouchEvent(ev);
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// final long flatPos = getExpandableListPosition(firstVisibleItem);
		// int groupPosition =
		// ExpandableListView.getPackedPositionGroup(flatPos);
		// int childPosition =
		// ExpandableListView.getPackedPositionChild(flatPos);
		// configureHeaderView(groupPosition, childPosition);
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
