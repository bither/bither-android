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

package net.bither;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.bitherj.utils.Utils;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.IBackClickListener;

/**
 * Created by songchenwen on 14/12/16.
 */
public class SignMessageActivity extends SwipeRightFragmentActivity {

    private EditText etInput;
    private TextView tvOutput;
    private FrameLayout flOutput;
    private Button btnSign;
    private ProgressBar pbSign;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_sign_message);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        etInput = (EditText) findViewById(R.id.et_input);
        tvOutput = (TextView) findViewById(R.id.tv_output);
        flOutput = (FrameLayout) findViewById(R.id.fl_output);
        btnSign = (Button) findViewById(R.id.btn_sign);
        pbSign = (ProgressBar) findViewById(R.id.pb_sign);
        btnSign.setOnClickListener(signClick);
        flOutput.setOnClickListener(outputClick);
    }

    private View.OnClickListener signClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            final String input = etInput.getText().toString().trim();
            if (Utils.isEmpty(input)) {
                return;
            }
            pbSign.setVisibility(View.VISIBLE);
            btnSign.setVisibility(View.INVISIBLE);
            new Thread() {
                @Override
                public void run() {
                    tvOutput.post(new Runnable() {
                        @Override
                        public void run() {
                            tvOutput.setText(input);
                            pbSign.setVisibility(View.INVISIBLE);
                            btnSign.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }.start();
        }
    };

    private View.OnClickListener outputClick = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            String output = tvOutput.getText().toString().trim();
            if(Utils.isEmpty(output)){
                return;
            }

        }
    };

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }
}
