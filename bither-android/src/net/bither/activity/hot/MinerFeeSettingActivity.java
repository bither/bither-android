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

import static net.bither.BitherSetting.INTENT_REF.MINER_FEE_BASE_KEY;
import static net.bither.BitherSetting.INTENT_REF.MINER_FEE_MODE_KEY;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.bither.R;

import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.api.BitherStatsDynamicFeeApi;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.BaseFragmentActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.ThreadUtil;

public class MinerFeeSettingActivity extends BaseFragmentActivity {

    public enum MinerFeeMode {
        Dynamic, Higher, High, Normal, Low, Custom;

        public int getDisplayNameRes() {
            switch (this) {
                case Higher:
                    return R.string.setting_name_transaction_fee_higher;
                case High:
                    return R.string.setting_name_transaction_fee_high;
                case Normal:
                    return R.string.setting_name_transaction_fee_normal;
                case Low:
                    return R.string.setting_name_transaction_fee_low;
                case Custom:
                    return R.string.setting_name_transaction_fee_custom;
                default:
                    return R.string.dynamic_miner_fee_title;
            }
        }

        public int getFeeBase() {
            BitherjSettings.TransactionFeeMode transactionFeeMode = getTransactionFeeMode();
            if (transactionFeeMode != null) {
                return transactionFeeMode.getMinFeeSatoshi();
            } else {
                return 0;
            }
        }

        static public MinerFeeMode getMinerFeeMode(BitherjSettings.TransactionFeeMode transactionFeeMode) {
            switch (transactionFeeMode) {
                case Higher:
                    return Higher;
                case High:
                    return High;
                case Normal:
                    return Normal;
                case Low:
                    return Low;
                default:
                    return null;
            }
        }

        private BitherjSettings.TransactionFeeMode getTransactionFeeMode() {
            switch (this) {
                case Higher:
                    return BitherjSettings.TransactionFeeMode.Higher;
                case High:
                    return BitherjSettings.TransactionFeeMode.High;
                case Normal:
                    return BitherjSettings.TransactionFeeMode.Normal;
                case Low:
                    return BitherjSettings.TransactionFeeMode.Low;
                default:
                    return null;
            }
        }

    }

    private ListView lv;
    private MinerFeeSettingActivity.MinerFeeMode curMinerFeeMode = MinerFeeMode.Dynamic;
    private long curMinerFeeBase;
    private DialogProgress dp;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_miner_fee_setting);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        dp = new DialogProgress(this, R.string.please_wait);
        dp.setCancelable(false);
        if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            if (extras.containsKey(MINER_FEE_MODE_KEY)) {
                curMinerFeeMode = (MinerFeeSettingActivity.MinerFeeMode) extras.get(MINER_FEE_MODE_KEY);
            }
            if (extras.containsKey(MINER_FEE_BASE_KEY)) {
                curMinerFeeBase = getIntent().getExtras().getLong(MINER_FEE_BASE_KEY);
            }
        }
        lv = (ListView) findViewById(R.id.lv);
        lv.setAdapter(adapter);
    }

    private void changeFinish() {
        final Intent result = getIntent();
        result.putExtra(MINER_FEE_MODE_KEY, curMinerFeeMode);
        result.putExtra(MINER_FEE_BASE_KEY, curMinerFeeBase);
        setResult(RESULT_OK, result);
        finish();
    }

    private void setCustom(final long custom) {
        dp.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Long dynamicFeeBase = null;
                try {
                    dynamicFeeBase = BitherStatsDynamicFeeApi.queryStatsDynamicFee();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                final long maxWarn = dynamicFeeBase != null ? (dynamicFeeBase / 500) : 600;
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp.isShowing()) {
                            dp.dismiss();
                        }
                        if (custom < maxWarn) {
                            curMinerFeeBase = custom  * 1000;
                            changeFinish();
                        } else {
                            DialogConfirmTask confirmTask = new DialogConfirmTask(MinerFeeSettingActivity.this, getString(R.string.setting_transaction_fee_custom_high), getString(R.string.setting_transaction_fee_custom_high_continue), getString(R.string.cancel), new Runnable() {
                                @Override
                                public void run() {
                                    curMinerFeeBase = custom * 1000;
                                    changeFinish();
                                }
                            });
                            confirmTask.setCancelable(false);
                            confirmTask.show();
                        }
                    }
                });
            }
        }).start();
    }

    private BaseAdapter adapter = new BaseAdapter() {

        private MinerFeeMode[] minerFeeModes = MinerFeeMode.values();
        private LayoutInflater inflater;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (inflater == null) {
                inflater = LayoutInflater.from(MinerFeeSettingActivity.this);
            }
            ViewHolder h;
            if (convertView != null && convertView.getTag() != null && convertView.getTag() instanceof ViewHolder) {
                h = (ViewHolder) convertView.getTag();
            } else {
                convertView = inflater.inflate(R.layout.list_item_miner_fee_setting, null);
                h = new ViewHolder(convertView);
                convertView.setTag(h);
            }
            final MinerFeeSettingActivity.MinerFeeMode minerFeeMode = getItem(position);
            final int feeBase = minerFeeMode.getFeeBase();
            if (feeBase > 0) {
                h.tvOptionName.setText(String.format("%s %d%s", getString(minerFeeMode.getDisplayNameRes()), feeBase / 1000, getString(R.string.send_confirm_fee_rate_symbol)));
            } else {
                h.tvOptionName.setText(minerFeeMode.getDisplayNameRes());
            }

            if (minerFeeMode.equals(curMinerFeeMode)) {
                h.ivCheck.setVisibility(View.VISIBLE);
                boolean isCustom = minerFeeMode == MinerFeeMode.Custom;
                h.llCustom.setVisibility(isCustom ? View.VISIBLE : View.GONE);
                h.vLine.setVisibility(isCustom ? View.GONE : View.VISIBLE);
                if (isCustom) {
                    if (curMinerFeeBase > 0) {
                        h.etCustom.setText(String.valueOf(curMinerFeeBase / 1000));
                    }
                    final ViewHolder fH = h;
                    h.btnConfirm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String customStr = fH.etCustom.getText().toString();
                            if (Utils.isEmpty(customStr)) {
                                DropdownMessage.showDropdownMessage(MinerFeeSettingActivity.this, R.string.setting_transaction_fee_custom_empty);
                                return;
                            }
                            try {
                                long custom = Long.parseLong(customStr);
                                if (custom > 0) {
                                    setCustom(custom);
                                } else {
                                    DropdownMessage.showDropdownMessage(MinerFeeSettingActivity.this, R.string.setting_transaction_fee_custom_empty);
                                }
                            } catch (Exception exception) {
                                DropdownMessage.showDropdownMessage(MinerFeeSettingActivity.this, R.string.setting_transaction_fee_custom_empty);
                            }
                        }
                    });
                }
            } else {
                h.ivCheck.setVisibility(View.INVISIBLE);
                h.llCustom.setVisibility(View.GONE);
                h.vLine.setVisibility(View.VISIBLE);
            }
            h.llOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (minerFeeMode == curMinerFeeMode) {
                        return;
                    }
                    if (minerFeeMode != MinerFeeMode.Custom) {
                        curMinerFeeMode = minerFeeMode;
                        curMinerFeeBase = feeBase;
                        AppSharedPreference.getInstance().setIsUseDynamicMinerFee(minerFeeMode == MinerFeeMode.Dynamic);
                        AppSharedPreference.getInstance().setTransactionFeeMode(minerFeeMode.getTransactionFeeMode());
                        AbstractApp.notificationService.sendMinerFeeChange();
                        changeFinish();
                    } else {
                        DialogConfirmTask confirmTask = new DialogConfirmTask(MinerFeeSettingActivity.this, getString(R.string.setting_transaction_fee_custom_warn), new Runnable() {
                            @Override
                            public void run() {
                                curMinerFeeMode = minerFeeMode;
                                curMinerFeeBase = 0;
                                ThreadUtil.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        });
                        confirmTask.setCancelable(false);
                        confirmTask.show();
                    }
                }
            });
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public MinerFeeSettingActivity.MinerFeeMode getItem(int position) {
            return minerFeeModes[position];
        }

        @Override
        public int getCount() {
            return minerFeeModes.length;
        }

        class ViewHolder {
            LinearLayout llOption;
            TextView tvOptionName;
            ImageView ivCheck;
            LinearLayout llCustom;
            EditText etCustom;
            Button btnConfirm;
            View vLine;

            public ViewHolder(View v) {
                llOption = v.findViewById(R.id.ll_option);
                tvOptionName = (TextView) v.findViewById(R.id.tv_option_name);
                ivCheck = (ImageView) v.findViewById(R.id.iv_check);
                llCustom = v.findViewById(R.id.ll_custom);
                etCustom = v.findViewById(R.id.et_custom);
                btnConfirm = v.findViewById(R.id.btn_confirm);
                vLine = v.findViewById(R.id.v_line);
            }
        }
    };

}
