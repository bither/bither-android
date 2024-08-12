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

import android.content.Context;

import net.bither.bitherj.BitherjSettings;
import net.bither.preference.AppSharedPreference;
import net.bither.service.BlockchainService;
import net.bither.ui.base.dialog.DialogProgress;

public abstract class ThreadNeedService extends Thread {
    protected DialogProgress dp;
    private boolean needService = AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.HOT;

    public ThreadNeedService(DialogProgress dp, Context context) {
        this.dp = dp;
    }

    abstract public void runWithService(BlockchainService service);

    @Override
    public void run() {
        if (needService) {
            runWithService(BlockchainService.getInstance());
        } else {
            runWithService(null);
        }
        if (dp != null) {
            dp.setThread(null);
        }
    }

    @Override
    public synchronized void start() {
        if (dp != null && !dp.isShowing()) {
            dp.show();
            dp.setCancelable(false);
        }
        if (dp != null) {
            dp.setThread(this);
        }
        super.start();
    }

}
