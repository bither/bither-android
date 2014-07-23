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
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.model.AddressInfo;
import net.bither.model.BitherAddress;
import net.bither.util.ConfidenceUtil;
import net.bither.util.CurrencySymbolUtil;
import net.bither.util.GenericUtils;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;

import java.math.BigInteger;

public class AddressFragmentListItemView extends FrameLayout implements
        AddressInfoChangedObserver, MarketTickerChangedObserver {

    private FragmentActivity activity;
    private TextView tvAddress;
    private TextView tvBalance;
    private ImageView ivBalanceSymbol;
    private BtcToMoneyTextView tvBalanceMoney;
    private ImageView ivWatchOnlyType;
    public ImageView ivPrivateType;
    private TransactionImmutureSummeryListItemView vTransactionImmuture;
    private View llExtra;
    private TextView tvTransactionCount;
    private View llMonitorFailed;
    private BitherAddress address;

    public AddressFragmentListItemView(FragmentActivity activity) {
        super(activity);
        this.activity = activity;
        View v = LayoutInflater.from(activity).inflate(R.layout.list_item_address_fragment_warm,
                null);
        addView(v, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        initView();
    }

    private void initView() {
        tvAddress = (TextView) findViewById(R.id.tv_address);
        tvBalance = (TextView) findViewById(R.id.tv_balance);
        ivBalanceSymbol = (ImageView) findViewById(R.id.iv_balance_symbol);
        tvTransactionCount = (TextView) findViewById(R.id.tv_transaction_count);
        llExtra = findViewById(R.id.ll_extra);
        llMonitorFailed = findViewById(R.id.ll_monitor_failed);
        tvBalanceMoney = (BtcToMoneyTextView) findViewById(R.id.tv_balance_money);
        ivWatchOnlyType = (ImageView) findViewById(R.id.iv_type_watchonly);
        ivPrivateType = (ImageView) findViewById(R.id.iv_type_private);
        ivBalanceSymbol.setImageBitmap(CurrencySymbolUtil.getBtcSlimSymbol(tvBalance));
        findViewById(R.id.ibtn_address_full).setOnClickListener(addressFullClick);
        vTransactionImmuture = (TransactionImmutureSummeryListItemView) findViewById(R.id
                .v_transaction_immuture);
        vTransactionImmuture.setActivity(activity);
    }

    private AddressFragmentListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private AddressFragmentListItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAddress(BitherAddress address, int loaderPosition, boolean isPrivate) {
        this.address = address;

        if (address != null) {
            showAddressInfo();
        }
    }

    private void showAddressInfo() {
        if (address == null) {
            return;
        }
        tvAddress.setText(address.getShortAddress());
        tvBalanceMoney.setVisibility(View.VISIBLE);
        ivBalanceSymbol.setVisibility(View.VISIBLE);
        llExtra.setVisibility(View.VISIBLE);
        llMonitorFailed.setVisibility(View.GONE);
        tvTransactionCount.setVisibility(View.GONE);
        if (address.hasPrivateKey()) {
            ivWatchOnlyType.setVisibility(GONE);
            ivPrivateType.setVisibility(VISIBLE);
        } else {
            ivWatchOnlyType.setVisibility(VISIBLE);
            ivPrivateType.setVisibility(GONE);
        }
        if (this.address.getAddressInfo() != null && address.isReadyToShow() && !address.isError
                ()) {
            tvBalance.setText(GenericUtils.formatValueWithBold(this.address.getAddressInfo()
                    .getBalance()));
            tvBalanceMoney.setBigInteger(this.address.getAddressInfo().getBalance());
            tvTransactionCount.setText(Integer.toString(this.address.getAddressInfo().getTxCount
                    ()));
            Transaction lastTransaction = WalletUtils.getLastTx(this.address);
            if (lastTransaction != null && (lastTransaction.getConfidence().getConfidenceType()
                    == ConfidenceType.BUILDING || lastTransaction.getConfidence()
                    .getConfidenceType() == ConfidenceType.PENDING) && ConfidenceUtil
                    .getDepthInChain(lastTransaction.getConfidence()) < 6) {
                vTransactionImmuture.setVisibility(View.VISIBLE);
                vTransactionImmuture.setTransaction(lastTransaction, address);
            } else {
                vTransactionImmuture.setVisibility(View.GONE);
            }
            if (vTransactionImmuture.getVisibility() == View.GONE) {
                tvTransactionCount.setVisibility(View.VISIBLE);
            }
        } else {
            ivBalanceSymbol.setVisibility(View.GONE);
            tvBalance.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvBalanceMoney.setBigInteger(null);
            vTransactionImmuture.setVisibility(View.GONE);
            if (address.isError()) {
                llExtra.setVisibility(View.GONE);
                llMonitorFailed.setVisibility(View.VISIBLE);
            }
        }
    }

    public AddressInfo getAddressInfo() {
        return this.address.getAddressInfo();
    }

    @Override
    public void onMarketTickerChanged() {
        tvBalanceMoney.onMarketTickerChanged();
    }

    @Override
    public void onAddressInfoChanged(String address) {
        if (StringUtil.compareString(address, this.address.getAddress())) {
            showAddressInfo();
        }
    }

    private OnClickListener addressFullClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ArrayMap<String, BigInteger> map = new ArrayMap<String, BigInteger>();
            map.put(address.getAddress(), null);
            DialogAddressFull dialog = new DialogAddressFull(activity, map);
            dialog.show(v);
        }
    };

    public void onPause() {
        vTransactionImmuture.onPause();
    }

    public void onResume() {
        showAddressInfo();
        vTransactionImmuture.onResume();
    }
}
