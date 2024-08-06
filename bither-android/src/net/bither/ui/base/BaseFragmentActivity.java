/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.ui.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import net.bither.pin.PinCodeUtil;

/**
 * Created by songchenwen on 14-11-10.
 */
public class BaseFragmentActivity extends FragmentActivity {

    protected boolean shouldPresentPinCode() {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldPresentPinCode()) {
            PinCodeUtil.resumeCheck(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinCodeUtil.checkBackground();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                }
                PinCodeUtil.checkBackgroundWithoutLockDelay();
            }
        }.start();
    }

    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            return super.registerReceiver(receiver, filter);
        }
    }

}
