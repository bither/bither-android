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
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.bither.R;
import net.bither.SignMessageActivity;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.utils.Utils;
import net.bither.http.BitherUrl;

import java.util.Locale;

public class DialogAddressWithPrivateKeyOption extends CenterDialog implements View
        .OnClickListener, DialogInterface.OnDismissListener {
    private DialogFancyQrCode dialogQr;
    private Address address;
    private Activity activity;
    private int clickedView;
    private TextView tvBlockMeta;
    private ImageView ivBlockMeta;

    public DialogAddressWithPrivateKeyOption(Activity context, Address address) {
        super(context);
        this.activity = context;
        this.address = address;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_address_with_private_key_option);
        tvBlockMeta = (TextView) findViewById(R.id.tv_view_on_blockmeta);
        ivBlockMeta = (ImageView) findViewById(R.id.iv_blockmeta);
        findViewById(R.id.tv_view_on_blockchaininfo).setOnClickListener(this);
        findViewById(R.id.tv_private_key_management).setOnClickListener(this);
        findViewById(R.id.tv_sign_message).setOnClickListener(this);
        findViewById(R.id.tv_close).setOnClickListener(this);
        tvBlockMeta.setOnClickListener(this);
        dialogQr = new DialogFancyQrCode(context, address.getAddress(), false, true);
        String defaultCountry = Locale.getDefault().getCountry();
        if (Utils.compareString(defaultCountry, "CN") || Utils.compareString(defaultCountry,
                "cn")) {
        } else {
            tvBlockMeta.setVisibility(View.GONE);
            ivBlockMeta.setVisibility(View.GONE);
        }
    }

    @Override
    public void show() {
        clickedView = 0;
        super.show();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        switch (clickedView) {
            case R.id.tv_view_on_blockchaininfo:
                BitherUrl.gotoBrower(activity, BitherUrl.BLOCKCHAIN_INFO_ADDRESS_URL + address
                        .getAddress());
                break;
            case R.id.tv_view_on_blockmeta:
                BitherUrl.gotoBrower(activity, BitherUrl.BLOCKMETA_ADDRESS_URL + address
                        .getAddress());
                break;
            case R.id.tv_private_key_management:
                new DialogAddressWithShowPrivateKey(activity, address).show();
                break;
            case R.id.tv_sign_message:
                activity.startActivity(new Intent(activity, SignMessageActivity.class));
                break;
            default:
                return;
        }
    }

    @Override
    public void onClick(View v) {
        clickedView = v.getId();
        dismiss();
    }
}
