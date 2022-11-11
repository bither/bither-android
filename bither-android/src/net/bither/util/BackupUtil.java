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

import static net.bither.bitherj.qrcode.QRCodeUtil.ADD_MODE_QR_CODE_FLAG;
import android.os.Handler;
import android.os.Looper;

import net.bither.BitherSetting;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.BaseRunnable;
import net.bither.runnable.HandlerMessage;

import java.io.File;
import java.util.Date;

// TODO : backup hot wallet's encrypted private keys & public keys in the rom
public class BackupUtil {

    public interface BackupListener {
        void backupSuccess();

        void backupError();
    }

    private static AppSharedPreference appSharedPreference = AppSharedPreference
            .getInstance();
    private static long ONE_WEEK_TIME = 7 * 24 * 60 * 60 * 1000;

    private BackupUtil() {

    }

    public static ECKey getEckeyFromBackup(String address, CharSequence password) {
        if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD) {
            return getEckeyFormBackupCold(address, password);
        } else {
            return getEckeyFormBackupHot(address, password);
        }
    }

    private static ECKey getEckeyFormBackupHot(String address, CharSequence password) {
        File file = FileUtil.getBackupKeyOfHot();
        String str = Utils.readFile(file);
        try {
            String base58Address = Base58.bas58ToHexWithAddress(address);
            if (str.contains(base58Address)) {
                String[] backupStrArray = str.split(PrivateKeyUtil.BACKUP_KEY_SPLIT_MUTILKEY_STRING);
                for (String backupStr : backupStrArray) {
                    if (backupStr.contains(base58Address)) {
                        String[] strArray = QRCodeUtil.splitString(backupStr);
                        if (strArray.length > 3) {
                            String keyString = backupStr.substring(strArray[0].length() + 1);
                            if (keyString.contains(ADD_MODE_QR_CODE_FLAG)) {
                                return PrivateKeyUtil.getECKeyFromSingleString(
                                        keyString.split(ADD_MODE_QR_CODE_FLAG)[0], password);
                            }
                        }
                    }
                }
            }
            return null;
        } catch (AddressFormatException e) {
            return null;
        }
    }

    private static ECKey getEckeyFormBackupCold(String address, CharSequence password) {
        if (!FileUtil.existSdCardMounted()) {
            return null;
        }
        try {
            File[] files = FileUtil.getBackupSdCardDir().listFiles();
            ECKey ecKey = null;
            if (files != null && files.length > 0) {
                ecKey = getEckeyFormBackupCold(files, address, password);
            }
            if (ecKey == null && BitherSetting.IS_ANDROID11_OR_HIGHER) {
                files = FileUtil.getBackupExternalFilesDir().listFiles();
                if (files != null && files.length > 0) {
                    return getEckeyFormBackupCold(files, address, password);
                }
            }
            return ecKey;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ECKey getEckeyFormBackupCold(File[] files, String address, CharSequence password) {
        try {
            files = FileUtil.orderByDateDesc(files);
            for (int i = files.length - 1; i >= 0; i--) {
                try {
                    File file = files[i];
                    String str = Utils.readFile(file);
                    String base58Address = Base58.bas58ToHexWithAddress(address);
                    if (str.contains(base58Address)) {
                        String[] backupStrArray = str.split(PrivateKeyUtil.BACKUP_KEY_SPLIT_MUTILKEY_STRING);
                        for (String backupStr : backupStrArray) {
                            if (backupStr.contains(base58Address)) {
                                String[] strArray = QRCodeUtil.splitString(backupStr);
                                if (strArray.length > 3) {
                                    String keyString = backupStr.substring(strArray[0].length() + 1);
                                    if (keyString.contains(ADD_MODE_QR_CODE_FLAG)) {
                                        return PrivateKeyUtil.getECKeyFromSingleString(
                                                keyString.split(ADD_MODE_QR_CODE_FLAG)[0], password);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void backupColdKey(boolean checkTime) {
        backupColdKey(checkTime, null);
    }

    public static void backupColdKey(boolean checkTime,
                                     BackupListener backupListener) {
        if (AppSharedPreference.getInstance().getAppMode() != BitherjSettings.AppMode.COLD) {
            if (backupListener != null) {
                backupListener.backupError();
            }
            return;
        }
        if (FileUtil.existSdCardMounted()) {
            boolean isBackup = false;
            if (checkTime) {
                Date lastBackupTime = appSharedPreference.getLastBackupkeyTime();
                if (lastBackupTime == null || ((lastBackupTime.getTime() + ONE_WEEK_TIME) < System.currentTimeMillis()) || FileUtil.getBackupFileListOfCold().size() == 0) {
                    isBackup = true;
                }
            } else {
                isBackup = true;
            }
            if (isBackup) {
                BackupPrivateKeyRunnable backupColdPrivateKeyRunnable = new
                        BackupPrivateKeyRunnable(
                        backupListener);
                new Thread(backupColdPrivateKeyRunnable).start();
            }
        } else {
            if (backupListener != null) {
                backupListener.backupError();
            }
        }
    }

    public static void backupHotKey() {
        if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.HOT) {
            BackupPrivateKeyRunnable backupColdPrivateKeyRunnable = new BackupPrivateKeyRunnable(
                    null);
            new Thread(backupColdPrivateKeyRunnable).start();
        }
    }

    public static String[] getBackupKeyStrList(File file) {
        String keyStrs = Utils.readFile(file);
        String[] result = null;
        if (!Utils.isEmpty(keyStrs)) {
            result = keyStrs.split(PrivateKeyUtil.BACKUP_KEY_SPLIT_MUTILKEY_STRING);
        }
        return result;
    }


    private static class BackupPrivateKeyRunnable extends BaseRunnable {

        private BackupListener mBackupListener;

        public BackupPrivateKeyRunnable(BackupListener backupListener) {
            this.mBackupListener = backupListener;
        }

        @Override
        public void run() {
            obtainMessage(HandlerMessage.MSG_PREPARE);
            final boolean result = backupPrivateKey();
            if (this.mBackupListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (result) {
                            mBackupListener.backupSuccess();
                        } else {
                            mBackupListener.backupError();
                        }
                    }
                });
            }
            obtainMessage(HandlerMessage.MSG_SUCCESS);
        }

        private boolean backupPrivateKey() {
            String backupString = PrivateKeyUtil.getBackupPrivateKeyStr();
            if (!Utils.isEmpty(backupString)) {
                try {
                    File file;
                    if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.HOT) {
                        file = FileUtil.getBackupKeyOfHot();
                    } else {
                        file = FileUtil.getBackupFileOfCold();
                    }
                    Utils.writeFile(backupString.getBytes(), file);
                    AppSharedPreference.getInstance().setLastBackupKeyTime(new Date(System.currentTimeMillis()));
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

    }

}
