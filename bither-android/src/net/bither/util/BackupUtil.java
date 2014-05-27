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

import java.io.File;
import java.util.Date;

import net.bither.BitherSetting.AppMode;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.BackupPrivateKeyRunnable;

import com.google.bitcoin.core.ECKey;

// TODO : backup hot wallet's encrypted private keys & public keys in the rom
public class BackupUtil {

    public interface BackupListener {
        void backupSuccess();

        void backupError();
    }

    private static AppSharedPreference appSharedPreference = AppSharedPreference
            .getInstance();
    private static long ONE_WEEK_TIME = 7 * 24 * 60 * 60 * 1000;
    public static String BACKUP_KEY_SPLIT_MUTILKEY_STRING = "\n";

    private BackupUtil() {

    }

    ;

    public static ECKey getEckeyFromBackup(String address, String password) {
        if (AppSharedPreference.getInstance().getAppMode() == AppMode.COLD) {
            return getEckeyFormBackupCold(address, password);
        } else {
            return getEckeyFormBackupHot(address, password);
        }

    }

    private static ECKey getEckeyFormBackupHot(String address, String password) {
        File file = FileUtil.getBackupKeyOfHot();
        String str = FileUtil.readFile(file);
        if (str.contains(address)) {
            String[] backupStrArray = str.split(BACKUP_KEY_SPLIT_MUTILKEY_STRING);
            for (String backupStr : backupStrArray) {
                if (backupStr.contains(address)) {
                    String[] strArray = backupStr
                            .split(StringUtil.QR_CODE_SPLIT);
                    if (strArray.length > 3) {
                        String keyString = backupStr.substring(strArray[0]
                                .length() + 1);
                        return PrivateKeyUtil.getECKeyFromSingleString(
                                keyString, password);
                    }
                }
            }
        }
        return null;
    }

    private static ECKey getEckeyFormBackupCold(String address, String password) {
        if (!FileUtil.existSdCardMounted()) {
            return null;
        }
        try {
            File[] files = FileUtil.getBackupSdCardDir().listFiles();
            if (files == null) {
                return null;
            }
            files = FileUtil.orderByDateDesc(files);
            for (int i = files.length - 1;
                 i >= 0;
                 i++) {
                File file = files[i];
                String str = FileUtil.readFile(file);
                if (str.contains(address)) {
                    String[] backupStrArray = str.split(BACKUP_KEY_SPLIT_MUTILKEY_STRING);
                    for (String backupStr : backupStrArray) {
                        if (backupStr.contains(address)) {
                            String[] strArray = backupStr
                                    .split(StringUtil.QR_CODE_SPLIT);
                            if (strArray.length > 3) {
                                String keyString = backupStr
                                        .substring(strArray[0].length() + 1);
                                return PrivateKeyUtil.getECKeyFromSingleString(
                                        keyString, password);
                            }
                        }
                    }
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
        if (appSharedPreference.getAppMode() == AppMode.COLD
                && FileUtil.existSdCardMounted()) {
            boolean isBackup = false;
            if (checkTime) {
                Date lastBackupTime = appSharedPreference
                        .getLastBackupkeyTime();
                if (lastBackupTime == null
                        || ((lastBackupTime.getTime() + ONE_WEEK_TIME) < System
                        .currentTimeMillis())) {
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
        }
    }

    public static void backupHotKey() {
        if (AppSharedPreference.getInstance().getAppMode() == AppMode.HOT) {
            BackupPrivateKeyRunnable backupColdPrivateKeyRunnable = new BackupPrivateKeyRunnable(
                    null);
            new Thread(backupColdPrivateKeyRunnable).start();
        }
    }

    public static String[] getBackupKeyStrList(File file) {
        String keyStrs = FileUtil.readFile(file);
        String[] result = null;
        if (!StringUtil.isEmpty(keyStrs)) {
            result = keyStrs.split(BACKUP_KEY_SPLIT_MUTILKEY_STRING);
        }
        return result;
    }

}
