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

package net.bither.ui.base.dialog;

import android.app.Activity;
import android.support.v4.app.Fragment;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.activity.hot.AddressDetailActivity;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.api.http.BitherUrl;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.cold.ColdAddressFragment;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by songchenwen on 15/1/12.
 */
public class DialogHDMAddressOptions extends DialogWithActions {
    private HDMAddress address;
    private Activity activity;

    public DialogHDMAddressOptions(Activity activity, HDMAddress address) {
        super(activity);
        this.address = address;
        this.activity = activity;
    }

    @Override
    protected List<Action> getActions() {
        ArrayList<Action> acitons = new ArrayList<Action>();
        acitons.add(new Action(R.string.address_option_view_on_blockchain_info, new Runnable() {
            @Override
            public void run() {
                UIUtil.gotoBrower(activity, BitherUrl.BLOCKCHAIN_INFO_ADDRESS_URL + address
                        .getAddress());
            }
        }));
        String defaultCountry = Locale.getDefault().getCountry();
        if (Utils.compareString(defaultCountry, "CN") || Utils.compareString(defaultCountry,
                "cn")) {
            acitons.add(new Action(R.string.address_option_view_on_blockmeta, new Runnable() {
                @Override
                public void run() {
                    UIUtil.gotoBrower(activity, BitherUrl.BLOCKMETA_ADDRESS_URL + address
                            .getAddress());
                }
            }));
        }
        acitons.add(new Action(R.string.trash_private_key, new Runnable() {
            @Override
            public void run() {
                if (address.getBalance() > 0) {
                    new DialogConfirmTask(getContext(), getContext().getString(R.string
                            .trash_with_money_warn), null).show();
                    return;
                }
                if (AddressManager.getInstance().getHdmKeychain().getAddresses().size() <= 1) {
                    new DialogConfirmTask(getContext(), getContext().getString(R.string
                            .hdm_address_trash_at_least_one_warn), null).show();
                    return;
                }
                new DialogPassword(activity, new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(SecureCharSequence password) {
                        final DialogProgress dp = new DialogProgress(getContext(),
                                R.string.trashing_private_key, null);
                        dp.show();
                        new Thread() {
                            @Override
                            public void run() {
                                AddressManager.getInstance().trashPrivKey(address);
                                ThreadUtil.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dp.dismiss();
                                        if (activity instanceof AddressDetailActivity) {
                                            activity.finish();
                                        }
                                        if (AppSharedPreference.getInstance().getAppMode() ==
                                                BitherjSettings.AppMode.HOT) {
                                            Fragment f = BitherApplication.hotActivity
                                                    .getFragmentAtIndex(1);
                                            if (f instanceof HotAddressFragment) {
                                                HotAddressFragment hotAddressFragment =
                                                        (HotAddressFragment) f;
                                                hotAddressFragment.refresh();
                                            }
                                        } else {
                                            Fragment f = BitherApplication.coldActivity
                                                    .getFragmentAtIndex(1);
                                            if (f instanceof ColdAddressFragment) {
                                                ColdAddressFragment coldAddressFragment =
                                                        (ColdAddressFragment) f;
                                                coldAddressFragment.refresh();
                                            }
                                        }
                                    }
                                });
                            }
                        }.start();
                    }
                }).show();
            }
        }));
        return acitons;
    }
}
