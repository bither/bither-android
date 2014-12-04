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
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.crypto.DumpedPrivateKey;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.Utils;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import java.math.BigInteger;

/**
 * Created by songchenwen on 14/12/4.
 */
public class RawPrivateKeyActivity extends SwipeRightActivity {
    private BigInteger min;
    private BigInteger max;
    private RawDataView vData;
    private Button btnZero;
    private Button btnOne;
    private Button btnAdd;
    private TextView tvPrivateKey;
    private TextView tvAddress;
    private LinearLayout llShow;
    private LinearLayout llInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_add_raw_private_key);
        initView();
        initBoundaries();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        vData = (RawDataView) findViewById(R.id.v_data);
        btnZero = (Button) findViewById(R.id.btn_zero);
        btnZero.setOnClickListener(addDataClick);
        btnOne = (Button) findViewById(R.id.btn_one);
        btnOne.setOnClickListener(addDataClick);
        llShow = (LinearLayout) findViewById(R.id.ll_show);
        llInput = (LinearLayout) findViewById(R.id.ll_input);
        tvPrivateKey = (TextView) findViewById(R.id.tv_private_key);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        btnAdd = (Button) findViewById(R.id.btn_add);
        vData.setRestrictedSize(getResources().getDisplayMetrics().widthPixels - UIUtil.dip2pix
                (16), (int) (getResources().getDisplayMetrics().heightPixels * 0.47f));
        vData.setDataSize(16, 16);
        llShow.setVisibility(View.GONE);
        llInput.setVisibility(View.VISIBLE);
    }

    private void initBoundaries() {
        min = BigInteger.ZERO;
        max = ECKey.CURVE.getN();
    }

    private void handleData() {
        BigInteger data = vData.getData();
        if (data == null) {
            return;
        }
        ECKey key = new ECKey(data);
        String address = Utils.toAddress(key.getPubKeyHash());
        SecureCharSequence privateKey = new DumpedPrivateKey(data.toByteArray(),
                true).toSecureCharSequence();
        tvPrivateKey.setText(WalletUtils.formatHashFromCharSequence(privateKey, 4, 16));
        tvAddress.setText(WalletUtils.formatHash(address, 4, 12));
        llInput.setVisibility(View.GONE);
        llShow.setVisibility(View.VISIBLE);
    }

    private View.OnClickListener addDataClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            btnZero.setVisibility(View.VISIBLE);
            btnOne.setVisibility(View.VISIBLE);
            if (vData.dataLength() > 0 && vData.filledDataLength() < vData.dataLength()) {
                vData.addData(v == btnOne);
                if (vData.filledDataLength() == vData.dataLength()) {
                    handleData();
                    return;
                }
                if (vData.testNextOneValue().compareTo(max) >= 0) {
                    btnOne.setVisibility(View.GONE);
                }
                if (vData.testNextZeroValue().compareTo(min) <= 0) {
                    btnZero.setVisibility(View.GONE);
                }
            }
        }
    };

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }
}
