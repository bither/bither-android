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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.api.http.Http400Exception;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.HDMBId;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.ScanActivity;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.ExceptionUtil;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.AbstractWheelTextAdapter;

/**
 * Created by songchenwen on 15/1/12.
 */
public class AddHDMAddressActivity extends FragmentActivity implements DialogPassword
        .PasswordGetter.PasswordGetterDelegate {
    private static final int ColdPubRequestCode = 1609;

    private WheelView wvCount;
    private DialogProgress dp;
    private HDMKeychain keychain;
    private DialogPassword.PasswordGetter passwordGetter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_hdm_address);
        keychain = AddressManager.getInstance().getHdmKeychain();
        if (keychain == null) {
            finish();
            return;
        }
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_cancel).setOnClickListener(new IBackClickListener());
        findViewById(R.id.btn_add).setOnClickListener(addClick);
        wvCount = (WheelView) findViewById(R.id.wv_count);
        wvCount.setViewAdapter(new CountAdapter(this));
        wvCount.setCurrentItem(0);
        dp = new DialogProgress(this, R.string.please_wait);
        dp.setCancelable(false);
        passwordGetter = new DialogPassword.PasswordGetter(this, this);
    }

    private View.OnClickListener addClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            int count = wvCount.getCurrentItem() + 1;
            if (keychain.uncompletedAddressCount() < count) {
                new DialogConfirmTask(v.getContext(),
                        getString(R.string.hdm_address_add_need_cold_pub), new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startActivityForResult(new Intent(AddHDMAddressActivity.this,
                                        ScanActivity.class), ColdPubRequestCode);
                            }
                        });
                    }
                }).show();
                return;
            }
            performAdd();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (ColdPubRequestCode == requestCode && resultCode == RESULT_OK) {
            final String result = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            try {
                final byte[] pub = Utils.hexStringToByteArray(result);
                final int count = (wvCount.getCurrentItem() + 1) - keychain
                        .uncompletedAddressCount();
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            keychain.prepareAddresses(count, passwordGetter.getPassword(), pub);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    performAdd();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (e instanceof HDMKeychain.HDMColdPubNotSameException) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DropdownMessage.showDropdownMessage(AddHDMAddressActivity
                                                .this, R.string.hdm_address_add_cold_pub_not_match);
                                    }
                                });
                            }
                        }
                    }
                }.start();
            } catch (Exception e) {
                e.printStackTrace();
                DropdownMessage.showDropdownMessage(this, R.string.hdm_address_add_need_cold_pub);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void performAdd() {
        final int count = wvCount.getCurrentItem() + 1;
        final DialogProgress dd = dp;
        new ThreadNeedService(null, this) {
            @Override
            public void runWithService(final BlockchainService service) {
                if (service != null) {
                    service.stopAndUnregister();
                }
                final List<HDMAddress> as = keychain.completeAddresses(count,
                        passwordGetter.getPassword(), new HDMKeychain.HDMFetchRemotePublicKeys() {
                            @Override
                            public void completeRemotePublicKeys(CharSequence password,
                                                                 List<HDMAddress.Pubs>
                                                                         partialPubs) {
                                try {
                                    HDMBId hdmBid = HDMBId.getHDMBidFromDb();
                                    HDMKeychain.getRemotePublicKeys(hdmBid, password, partialPubs);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    int msg = R.string.network_or_connection_error;
                                    if (e instanceof Http400Exception) {
                                        msg = ExceptionUtil.getHDMHttpExceptionMessage((
                                                (Http400Exception) e).getErrorCode());
                                    }
                                    final int m = msg;
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (dd.isShowing()) {
                                                dd.dismiss();
                                            }
                                            DropdownMessage.showDropdownMessage(AddHDMAddressActivity.this, m);
                                        }
                                    });
                                }
                            }
                        });
                if (service != null) {
                    service.startAndRegister();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dd.isShowing()) {
                            dd.dismiss();
                        }
                        if (as.size() == 0) {
                            return;
                        }
                        ArrayList<String> s = new ArrayList<String>();
                        for (HDMAddress a : as) {
                            s.add(a.getAddress());
                        }
                        Intent intent = new Intent();
                        intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG,
                                s);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                });
            }
        }.start();
    }

    private class CountAdapter extends AbstractWheelTextAdapter {

        protected CountAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemsCount() {
            int max = BitherSetting.HDM_ADDRESS_PER_SEED_COUNT_LIMIT - AddressManager.getInstance
                    ().getHdmKeychain().getAllCompletedAddresses().size();
            max = Math.min(max, 100);
            return max;
        }

        @Override
        protected CharSequence getItemText(int index) {
            return String.valueOf(index + 1);
        }
    }

    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }

    @Override
    public void beforePasswordDialogShow() {
        if (dp.isShowing()) {
            dp.dismiss();
        }
    }

    @Override
    public void afterPasswordDialogDismiss() {
        if (!dp.isShowing()) {
            dp.show();
        }
    }
}
