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

package net.bither.ui.base.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import net.bither.R;

public class DialogShowHeight extends DialogWithArrow {

	private TextView tvShowBlockHeight;
	private TextView tvShowWalletHeight;

	public DialogShowHeight(Context context) {
		super(context);
		setContentView(R.layout.dialog_show_height);
		tvShowBlockHeight = (TextView) findViewById(R.id.tv_show_block_height);
		tvShowWalletHeight = (TextView) findViewById(R.id.tv_show_wallet_height);
	}

	public void setPrompt(String blockPrompt, String walletPrompt) {
		tvShowBlockHeight.setText(blockPrompt);
		tvShowWalletHeight.setText(walletPrompt);

	}

	@Override
	public void show(View fromView) {
		super.show(fromView);

	}
}
