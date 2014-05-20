package net.bither.ui.base;

import android.view.View;
import android.widget.FrameLayout;

public class WrapFrameLayoutForAnimation {
	private FrameLayout.LayoutParams lp;
	private View v;

	public WrapFrameLayoutForAnimation(View v, FrameLayout.LayoutParams lp) {
		this.v = v;
		this.lp = lp;
	}

	public void setLeftMargin(int leftMargin) {
		lp.leftMargin = leftMargin;
		v.requestLayout();
	}

	public int getLeftMargin() {
		return lp.leftMargin;
	}

	public void setBottomMargin(int bottomMargin) {
		lp.bottomMargin = bottomMargin;
		v.requestLayout();
	}

	public int getBottomMargin() {
		return lp.bottomMargin;
	}

}
