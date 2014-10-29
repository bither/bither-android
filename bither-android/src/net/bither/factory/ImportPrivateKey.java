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

package net.bither.factory;

import android.app.Activity;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.cold.ColdAdvanceActivity;
import net.bither.activity.hot.HotAdvanceActivity;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.crypto.DumpedPrivateKey;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.QRCodeUtil;
import net.bither.model.PasswordSeed;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.util.KeyUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.TransactionsUtil;

import java.util.ArrayList;
import java.util.List;

public class ImportPrivateKey {
    public enum ImportPrivateKeyType {
        Text, BitherQrcode, Bip38
    }

    private String content;
    private SecureCharSequence password;
    private DialogProgress dp;
    private Activity activity;
    private ImportPrivateKeyType importPrivateKeyType;

    public ImportPrivateKey(Activity activity, ImportPrivateKeyType importPrivateKeyType
            , DialogProgress dp, String content, SecureCharSequence password) {
        this.content = content;
        this.password = password;
        this.activity = activity;
        this.dp = dp;
        this.importPrivateKeyType = importPrivateKeyType;
    }

    public void importPrivateKey() {
        ThreadNeedService threadNeedService = new ThreadNeedService(dp, activity) {
            @Override
            public void runWithService(BlockchainService service) {
                try {
                    ECKey ecKey = getEckey();
                    if (ecKey == null) {
                        password.wipe();
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dp != null && dp.isShowing()) {
                                    dp.setThread(null);
                                    dp.dismiss();
                                }
                                if (importPrivateKeyType == ImportPrivateKeyType.BitherQrcode) {
                                    DropdownMessage.showDropdownMessage(activity, R.string.password_wrong);
                                } else {
                                    DropdownMessage.showDropdownMessage(activity, R.string.import_private_key_qr_code_failed);
                                }
                            }
                        });
                        return;
                    }
                    List<String> addressList = new ArrayList<String>();
                    addressList.add(ecKey.toAddress());
                    if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.HOT) {
                        checkAddress(service, ecKey, addressList);
                    } else {
                        addECKey(service, ecKey);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    password.wipe();
                    ThreadUtil.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp != null && dp.isShowing()) {
                                dp.setThread(null);
                                dp.dismiss();
                            }
                            DropdownMessage.showDropdownMessage(activity, R.string.import_private_key_qr_code_failed);
                        }
                    });
                }
            }
        };
        threadNeedService.start();
    }

    private void checkAddress(BlockchainService service, ECKey ecKey, List<String> addressList) {
        try {
            BitherSetting.AddressType addressType = TransactionsUtil.checkAddress(addressList);
            handlerResult(service, ecKey, addressType);
        } catch (Exception e) {
            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (dp != null && dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(activity, R.string.network_or_connection_error);
                }
            });
        }

    }

    private void addECKey(BlockchainService blockchainService, ECKey ecKey) {
        String encryptedPrivateString;
        if (importPrivateKeyType == ImportPrivateKeyType.BitherQrcode) {
            encryptedPrivateString = QRCodeUtil.getNewVersionEncryptPrivKey(content);
        } else {
            ecKey = PrivateKeyUtil.encrypt(ecKey, password);
            encryptedPrivateString = PrivateKeyUtil.getPrivateKeyString(ecKey);
        }
        Address address = new Address(ecKey.toAddress(), ecKey.getPubKey(), encryptedPrivateString
                , ecKey.isFromXRandom());
        if (AddressManager.getInstance().getWatchOnlyAddresses().contains(address)) {
            password.wipe();
            ThreadUtil.runOnMainThread(new Runnable() {
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
            ThreadUtil.runOnMainThread(new Runnable() {
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
            if (importPrivateKeyType == ImportPrivateKeyType.BitherQrcode) {
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
            } else {
                password.wipe();
            }
            List<Address> addressList = new ArrayList<Address>();
            addressList.add(address);
            KeyUtil.addAddressListByDesc(blockchainService, addressList);
        }

        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (dp != null && dp.isShowing()) {
                    dp.setThread(null);
                    dp.dismiss();
                }
                if (activity instanceof HotAdvanceActivity) {
                    ((HotAdvanceActivity) activity).showImportSuccess();
                }
                if (activity instanceof ColdAdvanceActivity) {
                    ((ColdAdvanceActivity) activity).showImportSuccess();
                }
            }
        });

    }


    private void handlerResult(BlockchainService blockchainService, ECKey ecKey
            , BitherSetting.AddressType addressType) {
        switch (addressType) {
            case Normal:
                addECKey(blockchainService, ecKey);
                break;
            case SpecialAddress:
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(activity,
                                R.string.import_private_key_failed_special_address);
                    }
                });

                break;
            case TxTooMuch:
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(activity,
                                R.string.import_private_key_failed_tx_too_mush);
                    }
                });
                break;
        }
    }


    private ECKey getEckey() {
        ECKey ecKey = null;
        try {
            switch (this.importPrivateKeyType) {
                case Text:
                    ecKey = new DumpedPrivateKey(this.content).getKey();
                    break;
                case BitherQrcode:
                    ecKey = PrivateKeyUtil.getECKeyFromSingleString(content, password);
                    break;
                case Bip38:
                    ecKey = new DumpedPrivateKey(content).getKey();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ecKey;
    }

}
