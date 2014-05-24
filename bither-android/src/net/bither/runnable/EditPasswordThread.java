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

import net.bither.util.ThreadUtil;
import net.bither.util.WalletUtils;

/**
 * Created by songchenwen on 14-5-24.
 */
public class EditPasswordThread extends Thread {
    private String oldPassword;
    private String newPassword;
    private EditPasswordListener listener;

    public EditPasswordThread(String oldPassword, String newPassword,
                              EditPasswordListener listener) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        this.listener = listener;
    }

    @Override
    public void run() {
        final boolean result = WalletUtils.editPassword(oldPassword, newPassword);
        if (listener != null) {
            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (result) {
                        listener.onSuccess();
                    } else {
                        listener.onFailed();
                    }
                }
            });
        }
    }

    public static interface EditPasswordListener {
        public void onSuccess();

        public void onFailed();
    }
}
