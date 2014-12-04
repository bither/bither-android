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

package net.bither.rawprivatekey;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import net.bither.R;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 14/12/4.
 */
public class RawPrivateKeyActivity extends SwipeRightActivity {
    private RawDataView vData;
    private Button btnZero;
    private Button btnOne;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_add_raw_private_key);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        vData = (RawDataView) findViewById(R.id.v_data);
        btnZero = (Button) findViewById(R.id.btn_zero);
        btnZero.setOnClickListener(addDataClick);
        btnOne = (Button) findViewById(R.id.btn_one);
        btnOne.setOnClickListener(addDataClick);
        vData.setRestrictedSize(getResources().getDisplayMetrics().widthPixels - UIUtil.dip2pix
                (16), (int)(getResources().getDisplayMetrics().heightPixels * 0.47f));
        vData.setDataSize(16, 16);
    }

    private View.OnClickListener addDataClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (vData.dataLength() > 0) {
                vData.addData(v == btnOne);
            }
        }
    };

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }
}
