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

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class WrapLayoutParamsForAnimator {
	ViewGroup.LayoutParams lp;
	View v;

	public WrapLayoutParamsForAnimator(View v) {
		this.v = v;
		this.lp = v.getLayoutParams();
	}

	public void setHeight(int height) {
		lp.height = height;
		v.requestLayout();
	}

	public int getHeight() {
		return lp.height;
	}

	public float getLayoutWeight() {
		if (lp instanceof LinearLayout.LayoutParams) {
			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.lp;
			return lp.weight;
		}
		return 0;
	}

	public void setLayoutWeight(float weight) {
		if (lp instanceof LinearLayout.LayoutParams) {
			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.lp;
			lp.weight = weight;
			v.requestLayout();
		}
	}

	public void setWidth(int width) {
		lp.width = width;
		v.requestLayout();
	}

	public int getWidth() {
		return lp.width;
	}

}
