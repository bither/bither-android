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
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.utils.LogUtil;
import net.bither.util.GenericUtils;
import net.bither.util.UIUtil;

import java.util.ArrayList;

public class SwipeRightTouchView extends RelativeLayout {
	private static final boolean showLog = false;
	private View rlFinanceDetail;
	private FrameLayout.LayoutParams lpRl;
	private FrameLayout.LayoutParams lpShadow;
	private FrameLayout fl;
	private ImageView ivShadow;
	private float downLeft = 0;
	private float downTop = 0;
	private float xMove = 0;
	private float yMove = 0;
	private int firstLeft;
	private ArrayList<View> ignoreViews;
	private Runnable mDragTask;
	private boolean toAddShadow;

	public SwipeRightTouchView(Context context) {
		super(context);
		init();
	}

	public SwipeRightTouchView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SwipeRightTouchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		toAddShadow = GenericUtils.hasHoneycomb();
		ignoreViews = new ArrayList<View>();
		ivShadow = new ImageView(getContext());
		if (toAddShadow) {
			ivShadow.setBackgroundResource(R.drawable.swipe_right_to_pop_shadow);
		}
		ivShadow.setVisibility(View.INVISIBLE);
		postDelayed(new Runnable() {
			@Override
			public void run() {
				rlFinanceDetail = SwipeRightTouchView.this;
				rlFinanceDetail.getLayoutParams().width = UIUtil.getScreenWidth();
				fl = (FrameLayout) rlFinanceDetail.getParent();
				fl.addView(ivShadow,
						android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
						android.view.ViewGroup.LayoutParams.MATCH_PARENT);
				rlFinanceDetail.bringToFront();
				lpRl = (android.widget.FrameLayout.LayoutParams) rlFinanceDetail
						.getLayoutParams();
				lpShadow = (android.widget.FrameLayout.LayoutParams) ivShadow
						.getLayoutParams();
				ivShadow.postDelayed(new Runnable() {

					@Override
					public void run() {
						lpShadow.width = ivShadow.getWidth();
					}
				}, 5);
			}
		}, 5);

	}

	public void setDragTask(Runnable run) {
		mDragTask = run;
	}

	public void addIgnoreView(View view) {
		if (!ignoreViews.contains(view)) {
			ignoreViews.add(view);
		}
	}

	public void removeIgnoreView(View view) {
		if (ignoreViews.contains(view)) {
			ignoreViews.remove(view);
		}
	}

	public void clearIgnoreView() {
		ignoreViews.clear();
	}

	boolean ignored = false;

	// TODO : solve the memory leak of touchevent in pic detail
	// https://code.google.com/p/android/issues/detail?id=24211
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (fl == null) {
			return false;
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			ivShadow.setVisibility(View.INVISIBLE);
			float x, y;
			x = event.getX();
			y = event.getY();
			int[] locationInWindow = new int[2];
			getLocationInWindow(locationInWindow);
			x = x + locationInWindow[0];
			y = y + locationInWindow[1];
			for (View view : ignoreViews) {
				if (view.isShown()) {
					view.getLocationInWindow(locationInWindow);
					if (x > locationInWindow[0]
							&& x < locationInWindow[0] + view.getWidth()) {
						if (y > locationInWindow[1]
								&& y < locationInWindow[1] + view.getHeight()) {
							ignored = true;
							break;
						}
					}
				}
			}
			if (ignored) {
				return super.dispatchTouchEvent(event);
			}
			downLeft = event.getX()
					+ BitherSetting.SwipeRightGesture.SCROLL_DELAY_HORIZONTAL;
			downTop = event.getY();
			xMove = 0;
			yMove = 0;
			if (toAddShadow) {
				firstLeft = lpRl.leftMargin;
			} else {
				firstLeft = fl.getPaddingLeft();
			}
			if (showLog) {
				LogUtil.i("Drag",
						"Down   downLeft : " + Float.toString(downLeft)
								+ " ; downTop : " + Float.toString(downTop));
			}
		}
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (ignored) {
				return super.dispatchTouchEvent(event);
			}
			xMove = event.getX() - downLeft;
			yMove = event.getY() - downTop;
			if (showLog) {
				LogUtil.i("Drag", "Move xMove : " + Float.toString(xMove));
			}
			if (xMove > 0) {
				int leftMargin = (int) (firstLeft + xMove);
				if (toAddShadow) {
					ivShadow.setVisibility(View.VISIBLE);
					if (leftMargin > lpRl.leftMargin) {
						lpShadow.leftMargin = leftMargin - ivShadow.getWidth();
						lpRl.leftMargin = leftMargin;
						rlFinanceDetail.forceLayout();
						rlFinanceDetail.requestLayout();
						if (showLog) {
							LogUtil.i(
                                    "Drag",
                                    "Move true xMove : "
                                            + Float.toString(xMove)
                                            + " ; yMove : "
                                            + Float.toString(yMove)
                                            + " ; leftMargin : "
                                            + Integer.toString(leftMargin));
						}
					}
				} else {
					ivShadow.setVisibility(View.INVISIBLE);
					if (leftMargin > fl.getPaddingLeft()) {
						fl.setPadding(leftMargin, 0, 0, 0);
						rlFinanceDetail.forceLayout();
						rlFinanceDetail.requestLayout();
						if (showLog) {
							LogUtil.i(
									"Drag",
									"Move true xMove : "
											+ Float.toString(xMove)
											+ " ; yMove : "
											+ Float.toString(yMove)
											+ " ; leftMargin : "
											+ Integer.toString(leftMargin));
						}
					}
				}
				return true;
			}
			if (yMove > -BitherSetting.SwipeRightGesture.SCROLL_DELAY_VERTICAL
					&& yMove < BitherSetting.SwipeRightGesture.SCROLL_DELAY_VERTICAL) {
				return true;
			}
			if (showLog) {
				LogUtil.i("Drag", "Move false xMove : " + Float.toString(xMove)
						+ " ; yMove : " + Float.toString(yMove));
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (ignored) {
				ignored = false;
				return super.dispatchTouchEvent(event);
			}
			xMove = event.getX() - downLeft;
			yMove = event.getY() - downTop;
			if (showLog) {
				LogUtil.i("Drag",
						"Up xMove : " + Float.toString(xMove) + " ; Width :"
								+ Integer.toString(rlFinanceDetail.getWidth()));
			}

			if (xMove > rlFinanceDetail.getWidth()
					/ BitherSetting.SwipeRightGesture.DISMISS_DISTANCE_DIVIDER) {
				if (mDragTask != null) {
					post(mDragTask);
				}
				if (showLog) {
					LogUtil.i("Drag",
							"Up true xMove : " + Float.toString(xMove)
									+ " ; yMove : " + Float.toString(yMove));
				}
				return true;
			} else {
				ivShadow.setVisibility(View.INVISIBLE);
				if (toAddShadow) {
					lpShadow.leftMargin = firstLeft - ivShadow.getWidth();
					lpRl.leftMargin = firstLeft;
				} else {
					fl.setPadding(firstLeft, 0, 0, 0);
				}
				rlFinanceDetail.forceLayout();
				rlFinanceDetail.requestLayout();
			}
			downLeft = 0;
			xMove = 0;
			if (showLog) {
				LogUtil.i("Drag", "Up false xMove : " + Float.toString(xMove)
						+ " ; yMove : " + Float.toString(yMove));
			}
		}
		return super.dispatchTouchEvent(event);
	}
}
