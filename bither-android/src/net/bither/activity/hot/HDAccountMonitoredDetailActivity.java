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

import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.utils.Utils;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogHdAccountOldAddresses;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogWithActions;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/4/17.
 */
public class HDAccountMonitoredDetailActivity extends AddressDetailActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initAddress() {
        address = AddressManager.getInstance().getHDAccountMonitored();
        addressPosition = 0;
    }

    @Override
    protected void optionClicked() {
        new DialogWithActions(this) {
            @Override
            protected List<Action> getActions() {
                ArrayList<Action> actions = new ArrayList<Action>();
                actions.add(new Action(R.string.hd_account_request_new_receiving_address, new
                        Runnable() {
                    @Override
                    public void run() {
                        final DialogProgress dp = new DialogProgress
                                (HDAccountMonitoredDetailActivity.this, R.string.please_wait);
                        dp.setCancelable(false);
                        dp.show();
                        new Thread() {
                            @Override
                            public void run() {
                                final boolean result = ((HDAccount) address)
                                        .requestNewReceivingAddress();
                                ThreadUtil.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dp.dismiss();
                                        if (result) {
                                            loadData();
                                        } else {
                                            DropdownMessage.showDropdownMessage
                                                    (HDAccountMonitoredDetailActivity.this, R
                                                            .string
                                                            .hd_account_request_new_receiving_address_failed);
                                        }
                                    }
                                });
                            }
                        }.start();
                    }
                }));
                actions.add(new Action(R.string.hd_account_old_addresses, new Runnable() {
                    @Override
                    public void run() {
                        new DialogHdAccountOldAddresses(HDAccountMonitoredDetailActivity.this,
                                (HDAccount) address).show();
                    }
                }));
                return actions;
            }
        }.show();
    }

    @Override
    protected void notifyAddressBalanceChange(String address) {
        if (Utils.compareString(address, HDAccount.HDAccountMonitoredPlaceHolder)) {
            loadData();
        }
    }
}
