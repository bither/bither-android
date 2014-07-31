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

import net.bither.R;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

public class SwipeRightFragmentActivity extends FragmentActivity {
	protected SwipeRightTouchView mTouchView;

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		getWindow().getAttributes().dimAmount = 0.75f;
		initTouchView();
	}

	@Override
	public void setContentView(View view) {
		super.setContentView(view);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		getWindow().getAttributes().dimAmount = 0.75f;
		initTouchView();
	}

	@Override
	public void setContentView(View view, LayoutParams params) {
		super.setContentView(view, params);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		getWindow().getAttributes().dimAmount = 0.75f;
		initTouchView();
	}

	public void initTouchView() {
		View view = findViewById(R.id.swipe_right_touch_view);
		if (view instanceof SwipeRightTouchView) {
			mTouchView = (SwipeRightTouchView) view;
			mTouchView.setDragTask(dragTask);
		}
	}

	private Runnable dragTask = new Runnable() {
		@Override
		public void run() {
			finish();
			overridePendingTransition(0, R.anim.slide_out_right);
		}
	};

	public void onBackPressed() {
		finish();
	};

    public int getFinishAnimationDuration(){
        return 300;
    }

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, R.anim.slide_out_right);
	}

	@Override
	protected void onResume() {
		// Because this window is floating, if input method is showing,
		// when it pauses it's size remains full size minus soft input size.
		// So we restore the window's size on resume.
		getWindow().getAttributes().height = WindowManager.LayoutParams.MATCH_PARENT;
		super.onResume();
	}
}
