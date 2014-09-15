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

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.model.PasswordSeed;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.util.KeyUtil;
import net.bither.util.SecureCharSequence;

import java.util.ArrayList;
import java.util.List;

public class ImportPrivateKeyWithColdThread extends Thread {

    private String content;
    private SecureCharSequence password;
    private DialogProgress dp;
    private Activity activity;
    private Runnable importSuccessRunnable;


    public ImportPrivateKeyWithColdThread(Activity activity, DialogProgress dp, String content, SecureCharSequence password, Runnable runnable) {
        this.content = content;
        this.password = password;
        this.activity = activity;
        this.dp = dp;
        this.importSuccessRunnable = runnable;
    }

    @Override
    public void run() {
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

        Address address = new Address(key.toAddress(), key.getPubKey(), content);
        AddressManager addressManager = AddressManager.getInstance();
        if (addressManager.getWatchOnlyAddresses().contains(address)) {
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
        } else if (addressManager.getPrivKeyAddresses().contains(address)) {
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
            List<Address> addressList = new ArrayList<Address>();
            addressList.add(address);
            KeyUtil.addAddressList(null, addressList);
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dp != null && dp.isShowing()) {
                    dp.setThread(null);
                    dp.dismiss();
                }
                importSuccessRunnable.run();
            }
        });
    }
}
