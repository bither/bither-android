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

package net.bither.activity.hot;

import android.os.Bundle;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.fragment.cold.CheckFragment;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.BackClickListener;

public class CheckPrivateKeyActivity extends SwipeRightFragmentActivity {
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_check);
        findViewById(R.id.ibtn_back).setOnClickListener(new BackClickListener());
        if (getIntent().getExtras().getBoolean(BitherSetting.INTENT_REF
                .ADD_PRIVATE_KEY_SUGGEST_CHECK_TAG, false)) {
            CheckFragment checkFragment = (CheckFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.f_check);
            checkFragment.check();
        }
    }
}
