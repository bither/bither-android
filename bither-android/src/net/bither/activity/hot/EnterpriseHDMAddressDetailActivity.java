/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.activity.hot;

import android.os.Bundle;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.api.http.BitherUrl;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.utils.Utils;
import net.bither.ui.base.dialog.DialogWithActions;
import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by songchenwen on 15/6/12.
 */
public class EnterpriseHDMAddressDetailActivity extends AddressDetailActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initAddress() {
        if (getIntent().getExtras().containsKey(BitherSetting.INTENT_REF
                .ADDRESS_POSITION_PASS_VALUE_TAG)) {
            if (AddressManager.getInstance().hasEnterpriseHDMKeychain()) {
                addressPosition = getIntent().getExtras().getInt(BitherSetting.INTENT_REF
                        .ADDRESS_POSITION_PASS_VALUE_TAG);
                address = AddressManager.getInstance().getEnterpriseHDMKeychain().getAddresses()
                        .get(addressPosition);
            }
        }

    }

    @Override
    protected void optionClicked() {
        new DialogWithActions(this) {

            @Override
            protected List<Action> getActions() {
                ArrayList<Action> actions = new ArrayList<Action>();
                actions.add(new Action(R.string.address_option_view_on_blockchain_info, new
                        Runnable() {
                    @Override
                    public void run() {
                        UIUtil.gotoBrower(EnterpriseHDMAddressDetailActivity.this, BitherUrl
                                .BLOCKCHAIN_INFO_ADDRESS_URL + address.getAddress());
                    }
                }));
                String defaultCountry = Locale.getDefault().getCountry();
                if (Utils.compareString(defaultCountry, "CN") || Utils.compareString
                        (defaultCountry, "cn")) {
                    actions.add(new Action(R.string.address_option_view_on_btc, new
                            Runnable() {
                        @Override
                        public void run() {
                            UIUtil.gotoBrower(EnterpriseHDMAddressDetailActivity.this, BitherUrl
                                    .BTC_COM_ADDRESS_URL + address.getAddress());
                        }
                    }));
                }
                actions.add(new Action(R.string.address_option_view_on_blockchair,
                        new Runnable() {
                            @Override
                            public void run() {
                                UIUtil.gotoBrower(EnterpriseHDMAddressDetailActivity.this,
                                        BitherUrl.BLOCKCHAIR_ADDRESS_URL + address
                                                .getAddress());
                            }
                }));
                return actions;
            }
        }.show();
    }

}
