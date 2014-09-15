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

package net.bither.runnable;

import android.app.Activity;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.model.PasswordSeed;
import net.bither.preference.AppSharedPreference;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.util.KeyUtil;
import net.bither.util.SecureCharSequence;
import net.bither.util.TransactionsUtil;

import java.util.ArrayList;
import java.util.List;

public class ImportPrivateKeyWithHotThread extends ThreadNeedService {
    private String content;
    private SecureCharSequence password;
    private DialogProgress dp;
    private Activity activity;
    private Runnable importSuccessRunnable;

    public ImportPrivateKeyWithHotThread(Activity activity, DialogProgress dp, String content, SecureCharSequence password, Runnable runnable) {
        super(dp, activity);
        this.activity = activity;
        this.dp = dp;
        this.content = content;
        this.password = password;
        this.importSuccessRunnable = runnable;
    }

    @Override
    public void runWithService(BlockchainService service) {

        ECKey key = PrivateKeyUtil.getECKeyFromSingleString(content, password);
        if (key == null) {
            password.wipe();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dp != null && dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(activity,
                            R.string.password_wrong);
                }
            });
            return;
        }
        Address address = new Address(key.toAddress(), key.getPubKey(), PrivateKeyUtil.getPrivateKeyString(key));
        if (AddressManager.getInstance().getWatchOnlyAddresses().contains(address)) {
            password.wipe();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dp != null && dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(activity,
                            R.string.import_private_key_qr_code_failed_monitored);
                }
            });
            return;
        } else if (AddressManager.getInstance().getPrivKeyAddresses().contains(address)) {
            password.wipe();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dp != null && dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(activity,
                            R.string.import_private_key_qr_code_failed_duplicate);
                }
            });
            return;
        } else {
            PasswordSeed passwordSeed = AppSharedPreference.getInstance().getPasswordSeed();
            if (passwordSeed != null && !passwordSeed.checkPassword(password)) {
                password.wipe();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(activity,
                                R.string.import_private_key_qr_code_failed_different_password);
                    }
                });
                return;
            }
            password.wipe();

            try {
                List<String> addressList = new ArrayList<String>();
                addressList.add(key.toAddress());
                BitherSetting.AddressType addressType = TransactionsUtil.checkAddress(addressList);
                switch (addressType) {
                    case Normal:
                        List<Address> wallets = new
                                ArrayList<Address>();
                        wallets.add(address);
                        KeyUtil.addAddressList(service, wallets);
                        activity.runOnUiThread(importSuccessRunnable);
                        break;
                    case SpecialAddress:
                        DropdownMessage.showDropdownMessage(activity, R.string.import_private_key_failed_special_address);
                        break;
                    case TxTooMuch:
                        DropdownMessage.showDropdownMessage(activity, R.string.import_private_key_failed_tx_too_mush);
                        break;
                }
            } catch (Exception e) {
                DropdownMessage.showDropdownMessage(activity, R.string.network_or_connection_error);
                e.printStackTrace();
            }
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dp != null && dp.isShowing()) {
                    dp.setThread(null);
                    dp.dismiss();
                }
            }
        });
    }
}
