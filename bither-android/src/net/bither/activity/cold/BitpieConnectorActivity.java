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

package net.bither.activity.cold;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import net.bither.R;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.IBackClickListener;

//
// create by yiwenlong(wlong.yi@gmail.com) 2019/01/17
//

public class BitpieConnectorActivity extends SwipeRightFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cold_bitpie_connector);
        initViews();
    }

    private void initViews() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        findViewById(R.id.btn_sign_tx).setOnClickListener(signTxListener);
        findViewById(R.id.btn_sign_msg).setOnClickListener(signMessageListener);
        findViewById(R.id.btn_sign_hash).setOnClickListener(signHashListener);
    }

    private View.OnClickListener signTxListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(getApplicationContext(), BitpieSignTxActivity.class);
            startActivity(i);
        }
    };

    private View.OnClickListener signMessageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(getApplicationContext(), BitpieSignMessageActivity.class);
            startActivity(i);
        }
    };

    private View.OnClickListener signHashListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };
}
