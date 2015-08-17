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

package net.bither.activity.hot;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.EnterpriseHDMKeychain;
import net.bither.bitherj.crypto.hd.HDKeyDerivation;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.ScanActivity;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.CountSelectionView;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by songchenwen on 15/8/17.
 */
public class EnterpriseHDMKeychainAddNewAddressActivity extends SwipeRightFragmentActivity {

    private static final int AddPubCode = 1452;

    private LinearLayout llAdd;
    private LinearLayout llCollect;

    private CountSelectionView csvAddressCount;

    private ImageButton ibtnAddPub;
    private Button btnCollectFinish;

    private LinearLayout llPubs;

    private EnterpriseHDMKeychain keychain = AddressManager.getInstance()
            .getEnterpriseHDMKeychain();

    private ArrayList<byte[]> pubs = new ArrayList<byte[]>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enterprise_hdm_keychain_add_new_address);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        llAdd = (LinearLayout) findViewById(R.id.ll_add);
        llCollect = (LinearLayout) findViewById(R.id.ll_collect);
        csvAddressCount = (CountSelectionView) findViewById(R.id.csv_address_count);
        ibtnAddPub = (ImageButton) findViewById(R.id.ibtn_add_pub);
        btnCollectFinish = (Button) findViewById(R.id.btn_collect_finish);
        llPubs = (LinearLayout) findViewById(R.id.ll_pubs);

        llAdd.setVisibility(View.GONE);
        llCollect.setVisibility(View.VISIBLE);

        btnCollectFinish.setOnClickListener(collectFinishClick);
        ibtnAddPub.setOnClickListener(addPubClick);
        findViewById(R.id.btn_generate).setOnClickListener(addClick);

        csvAddressCount.setMax(50).setMin(1).setSelectedCount(1);

        ibtnAddPub.setVisibility(View.VISIBLE);
        btnCollectFinish.setEnabled(false);
        pubs.clear();
        llPubs.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(EnterpriseHDMKeychainAddNewAddressActivity
                .this);
        for (int i = 0;
             i < pubCount();
             i++) {
            inflater.inflate(R.layout.list_item_enterprise_hdm_collector, llPubs, true);
        }
    }

    private View.OnClickListener addPubClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            startActivityForResult(new Intent(EnterpriseHDMKeychainAddNewAddressActivity.this,
                    ScanActivity.class), AddPubCode);
        }
    };

    private View.OnClickListener collectFinishClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (pubs.size() == pubCount()) {
                llCollect.setVisibility(View.GONE);
                llAdd.setVisibility(View.VISIBLE);
            }
        }
    };

    private View.OnClickListener addClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            v.setEnabled(false);
            final DialogProgress d = new DialogProgress(v.getContext(), R.string.please_wait);
            d.show();
            new ThreadNeedService(null, v.getContext()) {

                @Override
                public void runWithService(BlockchainService service) {
                    if (service != null) {
                        service.stopAndUnregister();
                    }
                    int c = 0;
                    try {
                        c = keychain.prepareAddresses(addressCount(), pubs);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    final int count = c;
                    if (service != null) {
                        service.startAndRegister();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            d.dismiss();
                            if (count <= 0) {
                                DropdownMessage.showDropdownMessage
                                        (EnterpriseHDMKeychainAddNewAddressActivity.this, R
                                                .string.enterprise_hdm_keychain_add_fail, new
                                                Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                    }
                                });
                            } else {
                                setResult(RESULT_OK);
                                finish();
                            }
                        }
                    });
                }
            }.start();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (AddPubCode == requestCode) {
            if (resultCode == RESULT_OK) {
                String result = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                if (Utils.isEmpty(result)) {
                    return;
                }
                LogUtil.d("AddPub", "pub : " + result);
                byte[] pub = null;
                try {
                    if (result.length() == 130) {
                        pub = Utils.hexStringToByteArray(result);
                        if (HDKeyDerivation.createMasterPubKeyFromExtendedBytes(Arrays.copyOf
                                (pub, pub.length)) == null) {
                            pub = null;
                        }
                    }
                } catch (Exception e) {
                    pub = null;
                    e.printStackTrace();
                }
                if (pub == null) {
                    DropdownMessage.showDropdownMessage(this, R.string
                            .enterprise_hdm_keychain_collect_pub_error);
                    return;
                }
                pubs.add(pub);
                for (int i = 0;
                     i < pubs.size() && i < llPubs.getChildCount();
                     i++) {
                    View v = llPubs.getChildAt(i);
                    v.setBackgroundColor(getResources().getColor(R.color.blue_dark));
                }
                if (pubs.size() < pubCount()) {
                    ibtnAddPub.setVisibility(View.VISIBLE);
                    btnCollectFinish.setEnabled(false);
                } else {
                    ibtnAddPub.setVisibility(View.GONE);
                    btnCollectFinish.setEnabled(true);
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private int threshold() {
        return keychain.threshold();
    }

    private int pubCount() {
        return keychain.pubCount();
    }

    private int addressCount() {
        return csvAddressCount.selectedCount();
    }
}
