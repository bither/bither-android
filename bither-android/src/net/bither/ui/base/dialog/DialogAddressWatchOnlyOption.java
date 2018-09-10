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
import android.support.v4.app.Fragment;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.activity.hot.AddressDetailActivity;
import net.bither.bitherj.api.http.BitherUrl;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.util.KeyUtil;
import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DialogAddressWatchOnlyOption extends DialogWithActions {
    private Address address;
    private Activity activity;
    private Runnable afterDelete;


    public DialogAddressWatchOnlyOption(Activity context, Address address,
                                        Runnable afterDelete) {
        super(context);
        this.activity = context;
        this.address = address;
        this.afterDelete = afterDelete;
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
            actions.add(new Action(R.string.address_option_view_on_btc, new Runnable() {
                @Override
                public void run() {
                    UIUtil.gotoBrower(activity, BitherUrl.BTC_COM_ADDRESS_URL + address
                            .getAddress());
                }
            }));
        }
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
        actions.add(new Action(R.string.address_option_delete, new Runnable() {
            @Override
            public void run() {
                new DialogConfirmTask(getContext(), getContext().getString(R.string
                        .address_delete_confirmation), new Runnable() {
                    @Override
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DialogProgress dp = new DialogProgress(activity,
                                        R.string.please_wait);
                                dp.show();
                                ThreadNeedService threadNeedService = new ThreadNeedService(dp,
                                        activity) {
                                    @Override
                                    public void runWithService(BlockchainService service) {
                                        KeyUtil.stopMonitor(service, address);
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dp.dismiss();
                                                afterDelete.run();
                                                Fragment f = BitherApplication.hotActivity
                                                        .getFragmentAtIndex(1);
                                                if (f instanceof HotAddressFragment) {
                                                    HotAddressFragment hotAddressFragment =
                                                            (HotAddressFragment) f;
                                                    hotAddressFragment.refresh();
                                                }
                                            }
                                        });
                                    }
                                };
                                threadNeedService.start();
                            }
                        });
                    }
                }).show();
            }
        }));
        return actions;
    }
}
