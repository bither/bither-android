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
import net.bither.util.UIUtil;
import android.content.Context;
import android.widget.TextView;

public class DialogTransactionConfidence extends DialogWithArrow {

	public DialogTransactionConfidence(Context context, long confidence) {
		super(context);
		setContentView(R.layout.dialog_transaction_confidence);
		TextView tv = (TextView) findViewById(R.id.tv_confirmation);
		if (confidence <= 100) {
			tv.setText(Long.toString(confidence));
		} else {
			tv.setText("100+");
		}
	}

	@Override
	public int getSuggestHeight() {
		return UIUtil.dip2pix(40);
	}

}
