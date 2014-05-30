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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import net.bither.BitherSetting.AppMode;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.model.PasswordSeed;
import net.bither.preference.AppSharedPreference;
import net.bither.util.BackupUtil;
import net.bither.util.FileUtil;
import net.bither.util.PrivateKeyUtil;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;
import net.bither.util.BackupUtil.BackupListener;

import android.os.Handler;
import android.os.Looper;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.EncryptedPrivateKey;

public class BackupPrivateKeyRunnable extends BaseRunnable {

    private BackupListener mBackupListener;

    public BackupPrivateKeyRunnable(BackupListener backupListener) {
        this.mBackupListener = backupListener;

    }

    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);
        backupPrivateKey();
        if (this.mBackupListener != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {

                @Override
                public void run() {
                    mBackupListener.backupSuccess();

                }
            });
        }
        obtainMessage(HandlerMessage.MSG_SUCCESS);

    }

    private void backupPrivateKey() {
        File file = FileUtil.getBackupFileOfCold();
        if (AppSharedPreference.getInstance().getAppMode() == AppMode.HOT) {
            file = FileUtil.getBackupKeyOfHot();
        }
        String backupString = "";
        List<BitherAddressWithPrivateKey> addressWithPrivateKeys = WalletUtils
                .getPrivateAddressList();
        if (addressWithPrivateKeys == null) {
            return;
        }
        for (BitherAddressWithPrivateKey bitherAddressWithPrivateKey : addressWithPrivateKeys) {
            if (bitherAddressWithPrivateKey.getKeys() != null
                    && bitherAddressWithPrivateKey.getKeys().size() > 0) {
                PasswordSeed passwordSeed = new PasswordSeed(bitherAddressWithPrivateKey);
                backupString = backupString
                        + passwordSeed.toString()
                        + BackupUtil.BACKUP_KEY_SPLIT_MUTILKEY_STRING;

            }
        }
        if (!StringUtil.isEmpty(backupString)) {

            try {
                FileUtil.writeFile(backupString.getBytes(), file);
                AppSharedPreference.getInstance().setLastBackupKeyTime(
                        new Date(System.currentTimeMillis()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
