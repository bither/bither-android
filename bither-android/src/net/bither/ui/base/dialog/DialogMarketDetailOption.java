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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import net.bither.R;
import net.bither.model.Market;
import net.bither.ui.base.DropdownMessage;

public class DialogMarketDetailOption extends CenterDialog implements
		OnDismissListener, View.OnClickListener {
	public static interface MarketDetailDialogDelegate {
		public void share();

		public Market getMarket();
	}

	private MarketDetailDialogDelegate delegate;

	private int clickedId;

	public DialogMarketDetailOption(Context context,
			MarketDetailDialogDelegate delegate) {
		super(context);
		this.delegate = delegate;
		setContentView(R.layout.dialog_market_detail_option);
		setOnDismissListener(this);
		findViewById(R.id.tv_share).setOnClickListener(this);
		((TextView) findViewById(R.id.tv_web)).setText(delegate.getMarket()
				.getDomainName());
		findViewById(R.id.ll_web).setOnClickListener(this);
		findViewById(R.id.tv_close).setOnClickListener(this);
	}

	@Override
	public void show() {
		super.show();
		clickedId = 0;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		if (clickedId == R.id.tv_share) {
			delegate.share();
		} else if (clickedId == R.id.ll_web) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(delegate
					.getMarket().getUrl()))
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			try {
				getContext().startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
				if (getContext() instanceof Activity) {
					DropdownMessage.showDropdownMessage(
                            (Activity) getContext(),
                            R.string.find_browser_error);
				}
			}
		}
	}

	@Override
	public void onClick(View v) {
		clickedId = v.getId();
		dismiss();
	}

}
