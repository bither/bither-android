
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

import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.preference.AppSharedPreference;
import net.bither.service.BlockchainService;
import net.bither.xrandom.IUEntropy;
import net.bither.xrandom.XRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class KeyUtil {

    private KeyUtil() {

    }

    public static List<Address> addPrivateKeyByRandomWithPassphras(BlockchainService service, IUEntropy iuEntropy, CharSequence password, int count) {
        if (service != null) {
            service.stopAndUnregister();
        }
        List<Address> addressList = new ArrayList<Address>();
        for (int i = 0; i < count; i++) {
            XRandom xRandom = new XRandom(iuEntropy);
            ECKey ecKey = ECKey.generateECKey(xRandom);
            ecKey = PrivateKeyUtil.encrypt(ecKey, password);
            Address address = new Address(ecKey.toAddress(),
                    ecKey.getPubKey(), PrivateKeyUtil.getEncryptedString(ecKey), true, ecKey.isFromXRandom());
            ecKey.clearPrivateKey();
            addressList.add(address);
            AddressManager.getInstance().addAddress(address);

        }
        if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD) {
            BackupUtil.backupColdKey(false);
        } else {
            BackupUtil.backupHotKey();
        }
        if (service != null) {
            service.startAndRegister();
        }
        return addressList;

    }

    public static void addAddressListByDesc(BlockchainService service, List<Address> addressList) {
        if (service != null) {
            service.stopAndUnregister();
        }
        boolean hasPrivateKey = false;
        AddressManager addressManager = AddressManager.getInstance();
        //need reverse addressList
        Collections.reverse(addressList);
        for (Address address : addressList) {
            if (address.hasPrivKey() && !hasPrivateKey) {
                hasPrivateKey = true;
            }
            if (!addressManager.getPrivKeyAddresses().contains(address) &&
                    !addressManager.getWatchOnlyAddresses().contains(address)) {
                addressManager.addAddress(address);

            }
        }
        if (hasPrivateKey) {
            if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD) {
                BackupUtil.backupColdKey(false);
            } else {
                BackupUtil.backupHotKey();
            }
        }
        if (service != null) {
            service.startAndRegister();
        }

    }

    public static void setHDKeyChain(HDMKeychain keyChain) {
        AddressManager.getInstance().setHDMKeychain(keyChain);
        if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD) {
            BackupUtil.backupColdKey(false);
        } else {
            BackupUtil.backupHotKey();
        }

    }

    public static void setHDAccount(HDAccount hdAccount) {

        AddressManager.getInstance().setHdAccountHot(hdAccount);
        if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD) {
            BackupUtil.backupColdKey(false);
        } else {
            BackupUtil.backupHotKey();
        }


    }

    public static void stopMonitor(BlockchainService service, Address address) {
        if (service != null) {
            service.stopAndUnregister();
        }
        AddressManager.getInstance().stopMonitor(address);
        address.notificatTx(null, Tx.TxNotificationType.txFromApi);
        if (service != null) {
            service.startAndRegister();
        }

    }

}
