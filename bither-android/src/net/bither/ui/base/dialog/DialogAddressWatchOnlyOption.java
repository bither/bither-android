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
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.ui.base.DropdownMessage;

public class DialogAddressWatchOnlyOption extends CenterDialog {
    private DialogFancyQrCode dialogQr;
    private Address address;
    private TextView tvViewOnBlockchainInfo;
    private TextView tvClose;
    private Activity activity;
    private Runnable afterDelete;

    public DialogAddressWatchOnlyOption(Activity context, Address address,
                                        Runnable afterDelete) {
        super(context);
        this.activity = context;
        this.address = address;
        this.afterDelete = afterDelete;
        setContentView(R.layout.dialog_address_watch_only_option);
        tvViewOnBlockchainInfo = (TextView) findViewById(R.id.tv_view_on_blockchaininfo);
        tvClose = (TextView) findViewById(R.id.tv_close);
        tvViewOnBlockchainInfo.setOnClickListener(viewOnBlockchainInfoClick);
        tvClose.setOnClickListener(closeClick);
        dialogQr = new DialogFancyQrCode(context, address.getAddress(), false, true);
    }

    @Override
    public void show() {
        super.show();
    }

    private View.OnClickListener viewOnBlockchainInfoClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            dismiss();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://blockchain.info/address/" + address.getAddress())
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                getContext().startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                DropdownMessage.showDropdownMessage(activity, R.string.find_browser_error);
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
