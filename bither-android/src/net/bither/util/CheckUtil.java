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

package net.bither.util;

import android.content.Intent;
import android.provider.Settings;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.model.Check;
import net.bither.model.Check.CheckOperation;
import net.bither.model.Check.ICheckAction;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.runnable.CheckRunnable;
import net.bither.util.NetworkUtil.NetworkType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckUtil {
    private CheckUtil() {

    }

    public static ExecutorService runChecks(List<Check> checks, int threadCount) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (Check check : checks) {
            executor.execute(new CheckRunnable(check));
        }
        return executor;
    }

    public static Check initCheckOfWifi() {
        Check check = new Check(R.string.wifi_is_close,
                R.string.wifi_not_close, R.string.wifi_is_close_checking,
                new ICheckAction() {

                    @Override
                    public boolean check() {
                        NetworkType networkType = NetworkUtil.isConnectedType();
                        if (networkType == NetworkType.Wifi) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                });
        check.setCheckOperation(new CheckOperation() {
            @Override
            public void operate() {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                BitherApplication.mContext.startActivity(intent);
            }
        });
        return check;
    }

    public static Check initCheckOf3G() {
        Check check = new Check(R.string.threeg_is_close,
                R.string.threeg_not_close, R.string.threeg_is_close_checking,
                new ICheckAction() {

                    @Override
                    public boolean check() {
                        NetworkType networkType = NetworkUtil.isConnectedType();
                        if (networkType == NetworkType.Mobile) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                });
        check.setCheckOperation(new CheckOperation() {
            @Override
            public void operate() {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                BitherApplication.mContext.startActivity(intent);
            }
        });
        return check;
    }

    public static Check initCheckOfBluetooth() {
        Check check = new Check(R.string.bluetooth_is_close,
                R.string.bluetooth_not_close,
                R.string.bluetooth_is_close_checking, new ICheckAction() {
            @Override
            public boolean check() {
                if (NetworkUtil.BluetoothIsConnected()) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        check.setCheckOperation(new CheckOperation() {
            @Override
            public void operate() {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                BitherApplication.mContext.startActivity(intent);
            }
        });
        return check;
    }

    public static Check initCheckForPrivateKey(
            final Address address, final SecureCharSequence password) {
        String title = String.format(BitherApplication.mContext
                .getString(R.string.check_address_private_key_title), address
                .getShortAddress());
        Check check = new Check(title, new ICheckAction() {

            @Override
            public boolean check() {
                boolean result = new PasswordSeed(address).checkPassword(password);
                if (!result) {
                    try {
                        ECKey eckeyFromBackup = BackupUtil.getEckeyFromBackup(
                                address.getAddress(), password);
                        if (eckeyFromBackup != null) {
                            String encryptPrivateKey = PrivateKeyUtil.getEncryptedString(eckeyFromBackup);
                            eckeyFromBackup.clearPrivateKey();
                            if (!Utils.isEmpty(encryptPrivateKey)) {
                                address.setEncryptPrivKey(encryptPrivateKey);
                                address.updatePrivateKey();
                                result = true;
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        password.wipe();
                    }


                }
                return result;
            }
        });
        return check;
    }

    public static Check initCheckForRValue(final Address address) {
        String title = String.format(BitherApplication.mContext.getString(R.string
                .rcheck_address_title), address.getShortAddress());
        Check check = new Check(title, new ICheckAction() {

            @Override
            public boolean check() {
                TransactionsUtil.completeInputsForAddress(address);
                return address.checkRValues();
            }
        });
        return check;
    }
}
