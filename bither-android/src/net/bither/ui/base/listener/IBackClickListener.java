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

package net.bither.ui.base.listener;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;

public class IBackClickListener implements OnClickListener {
	private int enterAnim;
	private int exitAnim;

	public IBackClickListener(int enterAnim, int exitAnim) {
		this.enterAnim = enterAnim;
		this.exitAnim = exitAnim;
	}

	public IBackClickListener() {
		this.enterAnim = 0;
		this.exitAnim = 0;
	}

	@Override
	public void onClick(View v) {
		Context context = v.getContext();
		if (context instanceof Activity) {
			Activity activity = (Activity) context;
			activity.finish();
			if (enterAnim != 0 || exitAnim != 0) {
				activity.overridePendingTransition(enterAnim, exitAnim);
			}
		}
	}
}