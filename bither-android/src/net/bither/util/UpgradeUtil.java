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

import android.os.Handler;

import com.google.bitcoin.store.WalletProtobufSerializer;

import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.UpgradeAddressUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.BaseRunnable;
import net.bither.runnable.HandlerMessage;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class UpgradeUtil {

    // old watch only dir
    private static final String WALLET_WATCH_ONLY_OLD = "w";
    public static final int BITHERJ_VERSION_CODE = 9;
    public static final int UPGRADE_ADDRESS_TO_DB = 130;
    private static final String WALLET_SEQUENCE_WATCH_ONLY = "sequence_watch_only";
    private static final String WALLET_SEQUENCE_PRIVATE = "sequence_private";
    private static final String WALLET_ROM_CACHE = "wallet";
    private static final String WALLET_WATCH_ONLY = "watch";
    private static final String WALLET_HOT = "hot";
    private static final String WALLET_COLD = "cold";
    private static final String WALLET_ERROR = "error";

    private UpgradeUtil() {

    }

    public static boolean needUpgrade() {
        int verionCode = AppSharedPreference.getInstance().getVerionCode();
        return (verionCode < UPGRADE_ADDRESS_TO_DB && verionCode > 0) || getOldWatchOnlyCacheDir().exists();
    }

    public static void upgradeNewVerion(Handler handler) {
        BaseRunnable baseRunnable = new BaseRunnable() {
            @Override
            public void run() {
                obtainMessage(HandlerMessage.MSG_PREPARE);
                try {
                    if (getOldWatchOnlyCacheDir().exists()) {
                        upgradeV4();
                        upgradeToBitherj();
                    } else {
                        int verionCode = AppSharedPreference.getInstance().getVerionCode();
                        if (verionCode < BITHERJ_VERSION_CODE) {
                            upgradeToBitherj();
                        } else if (verionCode < UPGRADE_ADDRESS_TO_DB) {

                            UpgradeAddressUtil.upgradeAddress();
                        }
                    }
                    obtainMessage(HandlerMessage.MSG_SUCCESS);
                } catch (Exception e) {
                    e.printStackTrace();
                    obtainMessage(HandlerMessage.MSG_FAILURE);
                }

            }
        };
        baseRunnable.setHandler(handler);
        new Thread(baseRunnable).start();

    }

    //upgrde when version code <9
    private static void upgradeToBitherj() throws Exception {
        List<ECKey> ecKeyPrivates = initPrivateWallet(readPrivateAddressSequence());
        List<ECKey> ecKeysWatchOnly = initWatchOnlyWallet(readWatchOnlyAddressSequence());
        List<Address> privateAddressList = new ArrayList<Address>();
        List<Address> watchOnlyAddressList = new ArrayList<Address>();
        for (int i = 0; i < ecKeyPrivates.size(); i++) {
            ECKey ecKey = ecKeyPrivates.get(i);
            Address address = new Address(ecKey.toAddress(), ecKey.getPubKey(), PrivateKeyUtil.getEncryptedString(ecKey), false);
            privateAddressList.add(address);
        }
        if (privateAddressList.size() > 0) {
            KeyUtil.addAddressListByDesc(null, privateAddressList);
        }
        for (int i = 0; i < ecKeysWatchOnly.size(); i++) {
            ECKey ecKey = ecKeysWatchOnly.get(i);
            Address address = new Address(ecKey.toAddress(), ecKey.getPubKey(), null, false);
            watchOnlyAddressList.add(address);
        }
        if (watchOnlyAddressList.size() > 0) {
            KeyUtil.addAddressListByDesc(null, watchOnlyAddressList);
        }
    }

    private static void upgradeV4() {
        AppSharedPreference.getInstance().clear();
        File walletFile = UpgradeUtil.getOldWatchOnlyCacheDir();
        FileUtil.delFolder(walletFile.getAbsolutePath());
        File watchOnlyAddressSequenceFile = getWatchOnlyAddressSequenceFile();
        if (watchOnlyAddressSequenceFile.exists()) {
            watchOnlyAddressSequenceFile.delete();
        }
        //upgrade
//                File blockFile = FileUtil.getBlockChainFile();
//                if (blockFile.exists()) {
//                    blockFile.delete();
//                }
        File errorFolder = getWatchErrorDir();
        FileUtil.delFolder(errorFolder.getAbsolutePath());
    }

    public static File getOldWatchOnlyCacheDir() {
        File file = Utils.getWalletRomCache();
        file = new File(file, WALLET_WATCH_ONLY_OLD);
        return file;
    }

    private static List<ECKey> initPrivateWallet(
            List<String> sequence) throws Exception {
        List<ECKey> result = new ArrayList<ECKey>();
        File dir = getPrivateCacheDir();
        File[] fs = dir.listFiles();
        if (sequence != null) {
            fs = sortAddressFile(fs, sequence);
        }
        for (File walletFile : fs) {
            String name = walletFile.getName();
            if (sequence.contains(name)) {
                ECKey ecKey = loadECKey(walletFile);
                result.add(ecKey);
            }
        }
        return result;
    }

    private static List<ECKey> initWatchOnlyWallet(
            List<String> sequence) throws Exception {
        List<ECKey> result = new ArrayList<ECKey>();
        File dir = getWatchOnlyCacheDir();
        File[] fs = dir.listFiles();
        if (sequence != null) {
            fs = sortAddressFile(fs, sequence);
        }
        for (File walletFile : fs) {
            String name = walletFile.getName();
            if (sequence.contains(name)) {
                ECKey ecKey = loadECKey(walletFile);
                result.add(ecKey);
            }
        }
        return result;
    }

    public static File getPrivateCacheDir() {
        File file = Utils.getWalletRomCache();
        String dirName = WALLET_HOT;
        if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD) {
            dirName = WALLET_COLD;
        }
        file = new File(file, dirName);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }


    public static File getWatchOnlyCacheDir() {
        File file = Utils.getWalletRomCache();
        file = new File(file, WALLET_WATCH_ONLY);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }


    public static File getWatchErrorDir() {
        File file = Utils.getWalletRomCache();
        file = new File(file, WALLET_ERROR);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    private static File[] sortAddressFile(File[] fs, final List<String> sequence) {
        Arrays.sort(fs, new Comparator<File>() {
            public int compare(File f1, File f2) {
                long diff = sequence.indexOf(f1.getName())
                        - sequence.indexOf(f2.getName());
                if (diff > 0) {
                    return 1;
                } else if (diff == 0) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        return fs;
    }

    private static ECKey loadECKey(File walletFile) throws Exception {
        FileInputStream walletStream = null;
        walletStream = new FileInputStream(walletFile);
        ECKey ecKey = new
                WalletProtobufSerializer()
                .readWallet(walletStream);
        return ecKey;

    }

    public static File getWarmPrivateAddressSequenceFile() {
        File dir = Utils.getWalletRomCache();
        File file = new File(dir, WALLET_SEQUENCE_PRIVATE);
        return file;
    }

    public static File getWatchOnlyAddressSequenceFile() {
        File dir = Utils.getWalletRomCache();
        File file = new File(dir, WALLET_SEQUENCE_WATCH_ONLY);
        return file;
    }

    private static List<String> readPrivateAddressSequence() {
        File file = getWarmPrivateAddressSequenceFile();
        if (file.exists()) {
            ArrayList<String> addresses = (ArrayList<String>) FileUtil
                    .deserialize(file);
            return addresses;
        } else {
            return null;
        }
    }

    private static List<String> readWatchOnlyAddressSequence() {
        File file = getWatchOnlyAddressSequenceFile();
        if (file.exists()) {
            ArrayList<String> addresses = (ArrayList<String>) FileUtil
                    .deserialize(file);
            return addresses;
        } else {
            return null;
        }
    }
}
