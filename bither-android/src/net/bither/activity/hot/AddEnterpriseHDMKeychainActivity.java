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

import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.WheelView;

/**
 * Created by songchenwen on 15/6/9.
 */
public class AddEnterpriseHDMKeychainActivity extends SwipeRightFragmentActivity implements
        OnWheelChangedListener {
    private static final int AddPubCode = 1452;

    private LinearLayout llAdd;
    private LinearLayout llCollect;
    private LinearLayout llInit;

    private CountSelectionView csvThreshold;
    private CountSelectionView csvPubCount;
    private CountSelectionView csvAddressCount;

    private ImageButton ibtnAddPub;
    private Button btnCollectFinish;

    private LinearLayout llPubs;

    private ArrayList<byte[]> pubs = new ArrayList<byte[]>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_enterprise_hdm_keychain);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        llAdd = (LinearLayout) findViewById(R.id.ll_add);
        llCollect = (LinearLayout) findViewById(R.id.ll_collect);
        llInit = (LinearLayout) findViewById(R.id.ll_init);
        csvThreshold = (CountSelectionView) findViewById(R.id.csv_threshold);
        csvPubCount = (CountSelectionView) findViewById(R.id.csv_pub_count);
        csvAddressCount = (CountSelectionView) findViewById(R.id.csv_address_count);
        ibtnAddPub = (ImageButton) findViewById(R.id.ibtn_add_pub);
        btnCollectFinish = (Button) findViewById(R.id.btn_collect_finish);
        llPubs = (LinearLayout) findViewById(R.id.ll_pubs);

        llAdd.setVisibility(View.GONE);
        llCollect.setVisibility(View.GONE);
        llInit.setVisibility(View.VISIBLE);

        findViewById(R.id.btn_collect).setOnClickListener(collectClick);
        btnCollectFinish.setOnClickListener(collectFinishClick);
        ibtnAddPub.setOnClickListener(addPubClick);
        findViewById(R.id.btn_generate).setOnClickListener(addClick);

        csvPubCount.setMax(EnterpriseHDMKeychain.MaxPubCount).setMin(1).setSelectedCount(3);
        csvThreshold.setMax(csvPubCount.selectedCount()).setMin(1).setSelectedCount(2);
        csvAddressCount.setMax(50).setMin(1).setSelectedCount(1);
        csvPubCount.addChangingListener(this);
    }

    private View.OnClickListener collectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            llInit.setVisibility(View.GONE);
            llCollect.setVisibility(View.VISIBLE);
            llAdd.setVisibility(View.GONE);
            ibtnAddPub.setVisibility(View.VISIBLE);
            btnCollectFinish.setEnabled(false);
            pubs.clear();
            llPubs.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(AddEnterpriseHDMKeychainActivity.this);
            for (int i = 0;
                 i < pubCount();
                 i++) {
                inflater.inflate(R.layout.list_item_enterprise_hdm_collector, llPubs, true);
            }
        }
    };

    private View.OnClickListener addPubClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            startActivityForResult(new Intent(AddEnterpriseHDMKeychainActivity.this, ScanActivity
                    .class), AddPubCode);
        }
    };

    private View.OnClickListener collectFinishClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (pubs.size() == pubCount()) {
                llInit.setVisibility(View.GONE);
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
                    EnterpriseHDMKeychain k = null;
                    try {
                        k = new EnterpriseHDMKeychain(threshold(), addressCount(), pubs);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    final EnterpriseHDMKeychain keychain = k;
                    if (keychain != null) {
                        AddressManager.getInstance().setEnterpriseHDMKeychain(keychain);
                    }
                    if (service != null) {
                        service.startAndRegister();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            d.dismiss();
                            if (keychain == null) {
                                DropdownMessage.showDropdownMessage
                                        (AddEnterpriseHDMKeychainActivity.this, R.string
                                                .enterprise_hdm_keychain_add_fail, new Runnable() {
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

    @Override
    public void onChanged(WheelView wheel, int oldValue, int newValue) {
        if (wheel == csvPubCount) {
            int oldThreshold = threshold();
            int newMaxThreshold = csvPubCount.countAtIndex(newValue);
            csvThreshold.setMax(newMaxThreshold);
            if (newMaxThreshold < oldThreshold) {
                csvThreshold.setSelectedCountAnimated(newMaxThreshold);
            }
        }
    }

    private int threshold() {
        return csvThreshold.selectedCount();
    }

    private int pubCount() {
        return csvPubCount.selectedCount();
    }

    private int addressCount() {
        return csvAddressCount.selectedCount();
    }
}
