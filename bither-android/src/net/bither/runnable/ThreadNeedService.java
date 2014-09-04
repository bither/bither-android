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


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import net.bither.bitherj.core.BitherjSettings;
import net.bither.preference.AppSharedPreference;
import net.bither.service.BlockchainService;
import net.bither.service.LocalBinder;
import net.bither.ui.base.dialog.DialogProgress;

public abstract class ThreadNeedService extends Thread {
    protected DialogProgress dp;
    private BlockchainService service;
    private Context context;
    private boolean connected = false;
    private boolean needService = AppSharedPreference.getInstance()
            .getAppMode() == BitherjSettings.AppMode.HOT;

    public ThreadNeedService(DialogProgress dp, Context context) {
        this.dp = dp;
        this.context = context;
    }

    abstract public void runWithService(BlockchainService service);

    @Override
    public void run() {
        if (needService) {
            if (service != null) {
                runWithService(service);
            }
        } else {
            runWithService(null);
        }
        if (connected) {
            context.unbindService(connection);
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
        if (needService) {
            connected = context.bindService(new Intent(context,
                            BlockchainService.class), connection,
                    Context.BIND_AUTO_CREATE);
        } else {
            if (dp != null) {
                dp.setThread(this);
            }
            super.start();
        }
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ThreadNeedService.this.service = ((LocalBinder) binder)
                    .getService();
            if (dp != null) {
                dp.setThread(ThreadNeedService.this);
            }
            ThreadNeedService.super.start();
        }
    };
}
