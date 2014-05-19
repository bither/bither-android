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

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.fragment.Refreshable;
import net.bither.model.BitherAddress;
import net.bither.util.WalletUtils;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;

public class DialogAddressWatchOnlyOption extends CenterDialog {
	private BitherAddress address;
	private TextView tvViewOnBlockchainInfo;
	private TextView tvDelete;
	private TextView tvClose;
	private Activity activity;
	private Runnable afterDelete;

	public DialogAddressWatchOnlyOption(Activity context,
			BitherAddress address, Runnable afterDelete) {
		super(context);
		this.activity = context;
		this.address = address;
		this.afterDelete = afterDelete;
		setContentView(R.layout.dialog_address_watch_only_option);
		tvViewOnBlockchainInfo = (TextView) findViewById(R.id.tv_view_on_blockchaininfo);
		tvDelete = (TextView) findViewById(R.id.tv_delete);
		tvClose = (TextView) findViewById(R.id.tv_close);
		tvViewOnBlockchainInfo.setOnClickListener(viewOnBlockchainInfoClick);
		tvDelete.setOnClickListener(deleteClick);
		tvClose.setOnClickListener(closeClick);
	}

	private View.OnClickListener viewOnBlockchainInfoClick = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			dismiss();
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("http://blockchain.info/address/"
							+ address.getAddress()))
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			try {
				getContext().startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
				DropdownMessage.showDropdownMessage(activity,
						R.string.find_browser_error);
			}
		}
	};
	private View.OnClickListener deleteClick = new View.OnClickListener() {

		@Override
		public void onClick(final View v) {
			dismiss();
			if (WalletUtils.getBitherAddressList().size() == 1) {
				DropdownMessage.showDropdownMessage(activity,
						R.string.address_detail_empty_address_list_notice);
				return;
			}
			if (BitherApplication.getBitherApplication().isCanStopMonitor()) {
				BitherApplication.getBitherApplication().setCanStopMonitor(
						false);
				new DialogConfirmTask(getContext(), getContext().getString(
						R.string.address_delete_confirmation), new Runnable() {
					@Override
					public void run() {
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								WalletUtils.removeBitherAddress(address);
								Fragment f = BitherApplication.warmActivity
										.getFragmentAtIndex(1);
								if (f instanceof Refreshable) {
									((Refreshable) f).doRefresh();
								}
								if (afterDelete != null) {
									activity.runOnUiThread(afterDelete);
								}
							}
						});
					}
				}).show();
			} else {
				DropdownMessage.showDropdownMessage(activity,
						R.string.address_detail_cannot_stop_monitoring);
			}
		}
	};
	private View.OnClickListener closeClick = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			dismiss();
		}
	};

}
