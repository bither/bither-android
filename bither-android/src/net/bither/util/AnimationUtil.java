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

package net.bither.util;

import net.bither.ui.base.WrapFrameLayoutForAnimation;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

public class AnimationUtil {
	private static final int FADE_IN_DURATION = 300;
	private static final int MOVE_MARGIN_DURATION = 500;

	private AnimationUtil() {

	}

	public static void fadeIn(final View view) {
		Animation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
		alphaAnimation.setDuration(FADE_IN_DURATION);
		alphaAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				view.setVisibility(View.INVISIBLE);

			}
		});
		view.startAnimation(alphaAnimation);
	}

	public static void moveMarginAnimation(View view, int leftMargin,
			int bottomMargin) {
		FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view
				.getLayoutParams();
		WrapFrameLayoutForAnimation wrapLayoutParamsForAnimator = new WrapFrameLayoutForAnimation(
				view, layoutParams);
		ObjectAnimator animatorLeftMargin = ObjectAnimator.ofInt(
				wrapLayoutParamsForAnimator, "leftMargin", leftMargin)
				.setDuration(MOVE_MARGIN_DURATION);
		ObjectAnimator animatorBottomMargin = ObjectAnimator.ofInt(
				new WrapFrameLayoutForAnimation(
						view, layoutParams), "bottomMargin", bottomMargin)
				.setDuration(MOVE_MARGIN_DURATION);
		AnimatorSet animSetXY = new AnimatorSet();
		animSetXY.playTogether(animatorLeftMargin, animatorBottomMargin);
		animSetXY.start();

	}

}
