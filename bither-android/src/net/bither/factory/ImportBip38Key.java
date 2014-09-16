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

import net.bither.R;
import net.bither.bitherj.crypto.bip38.Bip38;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.SecureCharSequence;
import net.bither.util.ThreadUtil;

public class ImportBip38Key implements IDialogPasswordListener {

    private String content;
    private SecureCharSequence password;
    private DialogProgress dp;
    private Activity activity;
    private String result;

    public ImportBip38Key(Activity activity, DialogProgress dp, SecureCharSequence password, String content) {
        this.activity = activity;
        this.dp = dp;
        this.password = password;
        this.content = content;

    }

    public void importBip38() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    result =
                            Bip38.decrypt(content, password.toString());
                    if (result == null) {
                        password.wipe();
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dp != null && dp.isShowing()) {
                                    dp.setThread(null);
                                    dp.dismiss();
                                }
                                DropdownMessage.showDropdownMessage(activity, R.string.password_wrong);
                            }
                        });

                    } else {
                        DialogPassword dialogPassword = new DialogPassword(activity,
                                ImportBip38Key.this);
                        dialogPassword.show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
        ).start();
    }

    @Override
    public void onPasswordEntered(SecureCharSequence password) {
        ImportPrivateKey importPrivateKey = new ImportPrivateKey(activity,
                ImportPrivateKey.ImportPrivateKeyType.BitherQrcode, dp, content, password);
        importPrivateKey.importPrivateKey();

    }
}
