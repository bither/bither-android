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

package net.bither.ui.base;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.SendActivity;
import net.bither.activity.hot.AddressDetailActivity;
import net.bither.activity.hot.GenerateUnsignedTxActivity;
import net.bither.fragment.Refreshable;
import net.bither.model.BitherAddress;
import net.bither.preference.AppSharedPreference;
import net.bither.util.Qr;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import java.math.BigInteger;

public class AddressDetailHeader extends FrameLayout implements DialogFragmentFancyQrCodePager
        .QrCodeThemeChangeListener {
    private AddressDetailActivity activity;
    private BitherAddress address;
    private FrameLayout flAddress;
    private TextView tvAddress;
    private QrCodeImageView ivQr;
    private TextView tvNoTransactions;
    private LinearLayout llMonitorFailed;
    private Button btnCancelMonitor;
    private LinearLayout llMore;
    private Button btnSend;
    private ImageButton ibtnBalanceDetail;
    private BalanceBtcToMoneyButton btnBalance;
    private int addressPosition;

    public AddressDetailHeader(AddressDetailActivity activity) {
        super(activity);
        this.activity = activity;
        initView();
    }

    private void initView() {
        removeAllViews();
        addView(LayoutInflater.from(getContext()).inflate(R.layout.layout_address_detail_header,
                        null), LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        );
        flAddress = (FrameLayout) findViewById(R.id.fl_address);
        ivQr = (QrCodeImageView) findViewById(R.id.iv_qrcode);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        tvNoTransactions = (TextView) findViewById(R.id.tv_no_transactions);
        llMonitorFailed = (LinearLayout) findViewById(R.id.ll_monitor_failed);
        btnCancelMonitor = (Button) findViewById(R.id.btn_cancel_monitor);
        llMore = (LinearLayout) findViewById(R.id.ll_more);
        btnSend = (Button) findViewById(R.id.btn_send);
        ibtnBalanceDetail = (ImageButton) findViewById(R.id.ibtn_balance_detail);
        btnBalance = (BalanceBtcToMoneyButton) findViewById(R.id.btn_balance);
        flAddress.setOnClickListener(copyClick);
        ivQr.setOnClickListener(qrClick);
        btnCancelMonitor.setOnClickListener(cancelMonitorClick);
        btnSend.setOnClickListener(sendClick);
        ibtnBalanceDetail.setOnClickListener(balanceDetailClick);
    }

    private AddressDetailHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private AddressDetailHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void showAddress(final BitherAddress address, int addressPosition) {
        this.addressPosition = addressPosition;
        if (address.getAddress() == null) {
            return;
        }
        tvAddress.setText(WalletUtils.formatHash(address.getAddress(), 4, 12));
        if (this.address != address) {
            Qr.QrCodeTheme theme = AppSharedPreference.getInstance().getFancyQrCodeTheme();
            ivQr.setContent(address.getAddress(), theme.getFgColor(), theme.getBgColor());
        }
        if (!address.isError()) {
            if (address.getAddressInfo() != null) {
                llMore.setVisibility(View.VISIBLE);
                if (address.getAddressInfo().getTxCount() == 0) {
                    tvNoTransactions.setVisibility(View.VISIBLE);
                } else {
                    tvNoTransactions.setVisibility(View.GONE);
                }
                btnBalance.setBigInteger(address.getAddressInfo().getBalance());
                if (address.hasPrivateKey()) {
                    btnSend.setCompoundDrawables(null, null, null, null);
                } else {
                    Drawable d = getContext().getResources().getDrawable(R.drawable
                            .unsigned_transaction_button_icon);
                    int size = UIUtil.dip2pix(20);
                    int topOffset = UIUtil.dip2pix(0.5f);
                    d.setBounds(0, topOffset, size, size + topOffset);
                    btnSend.setCompoundDrawables(null, null, d, null);
                }
            } else {
                llMore.setVisibility(View.GONE);
            }
            llMonitorFailed.setVisibility(View.GONE);
        } else {
            llMore.setVisibility(View.GONE);
            tvNoTransactions.setVisibility(View.GONE);
            llMonitorFailed.setVisibility(View.VISIBLE);
        }
        this.address = address;
    }

    private OnClickListener copyClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (address != null) {
                String text = address.getAddress();
                StringUtil.copyString(text);
                DropdownMessage.showDropdownMessage(activity, R.string.copy_address_success);
            }
        }
    };
    private OnClickListener cancelMonitorClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (address != null) {
                WalletUtils.removeBitherAddress(address);
            }
            Fragment f = BitherApplication.hotActivity.getFragmentAtIndex(1);
            if (f instanceof Refreshable) {
                ((Refreshable) f).doRefresh();
            }
            activity.finish();
        }
    };
    private OnClickListener balanceDetailClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogBalanceDetail dialog = new DialogBalanceDetail(activity,
                    address.getAddressInfo());
            dialog.show(v);
        }
    };
    private OnClickListener qrClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogFragmentFancyQrCodePager.newInstance(address.getAddress())
                    .setQrCodeThemeChangeListener(AddressDetailHeader.this).show(activity
                    .getSupportFragmentManager(), DialogFragmentFancyQrCodePager.FragmentTag);
        }
    };

    private OnClickListener sendClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (address != null) {
                if (address.getAddressInfo().getBalance().compareTo(BigInteger.ZERO) <= 0) {
                    DropdownMessage.showDropdownMessage(activity,
                            R.string.address_detail_send_balance_zero);
                    return;
                }
                if (address.hasPrivateKey()) {
                    Intent intent = new Intent(activity, SendActivity.class);
                    intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG,
                            addressPosition);
                    activity.startActivityForResult(intent, BitherSetting.INTENT_REF
                            .SEND_REQUEST_CODE);
                } else {
                    Intent intent = new Intent(getContext(), GenerateUnsignedTxActivity.class);
                    intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG,
                            addressPosition);
                    activity.startActivityForResult(intent, BitherSetting.INTENT_REF
                            .SEND_REQUEST_CODE);
                }
            }
        }
    };

    @Override
    public void qrCodeThemeChangeTo(Qr.QrCodeTheme theme) {
        ivQr.setContent(address.getAddress(), theme.getFgColor(), theme.getBgColor());
    }
}
