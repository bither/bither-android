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

import net.bither.R;
import net.bither.SignMessageActivity;
import net.bither.activity.hot.AddressDetailActivity;
import net.bither.bitherj.api.http.BitherUrl;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.utils.Utils;
import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DialogAddressWithPrivateKeyOption extends DialogWithActions {
    private Address address;
    private Activity activity;

    public DialogAddressWithPrivateKeyOption(Activity context, Address address) {
        super(context);
        this.activity = context;
        this.address = address;
    }

    @Override
    protected List<Action> getActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new Action(R.string.address_option_view_on_blockchain_info, new Runnable() {
            @Override
            public void run() {
                UIUtil.gotoBrower(activity, BitherUrl.BLOCKCHAIN_INFO_ADDRESS_URL + address
                        .getAddress());
            }
        }));
        String defaultCountry = Locale.getDefault().getCountry();
        if (Utils.compareString(defaultCountry, "CN") || Utils.compareString(defaultCountry,
                "cn")) {
            actions.add(new Action(R.string.address_option_view_on_blockmeta, new Runnable() {
                @Override
                public void run() {
                    UIUtil.gotoBrower(activity, BitherUrl.BLOCKMETA_ADDRESS_URL + address
                            .getAddress());
                }
            }));
        }
        actions.add(new Action(R.string.private_key_management, new Runnable() {
            @Override
            public void run() {
                new DialogAddressWithShowPrivateKey(activity, address, null).show();
            }
        }));
        actions.add(new Action(R.string.sign_message_activity_name, new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(activity, SignMessageActivity.class);
                intent.putExtra(SignMessageActivity.AddressKey, address.getAddress());
                activity.startActivity(intent);
            }
        }));
        actions.add(new Action(R.string.address_alias_manage, new Runnable() {
            @Override
            public void run() {
                new DialogAddressAlias(activity, address,
                        activity instanceof AddressDetailActivity ? (AddressDetailActivity)
                                activity : null).show();
            }
        }));
        actions.add(new Action(R.string.vanity_address_length, new Runnable() {
            @Override
            public void run() {
                new DialogEditVanityLength(activity, address).show();
            }
        }));
        return actions;
    }
}
