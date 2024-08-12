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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.Tx;


public class TransactionImmutureSummeryListItemView extends FrameLayout {

    private FragmentActivity activity;
    private TransactionImmutureConfidenceIconView icon;
    private BtcToMoneyButton btnBtc;

    private Tx transaction;
    private Address address;

    public TransactionImmutureSummeryListItemView(FragmentActivity activity) {
        super(activity);
        removeAllViews();
        this.setActivity(activity);
        View v = LayoutInflater.from(activity).inflate(
                R.layout.list_item_transaction_immature_summery, null);
        addView(v, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        initView();
    }

    public TransactionImmutureSummeryListItemView(Context context,
                                                  AttributeSet attrs) {
        super(context, attrs);
        removeAllViews();
        View v = LayoutInflater.from(context).inflate(
                R.layout.list_item_transaction_immature_summery, null);
        addView(v, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        initView();
    }

    public TransactionImmutureSummeryListItemView(Context context,
                                                  AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        removeAllViews();
        View v = LayoutInflater.from(context).inflate(
                R.layout.list_item_transaction_immature_summery, null);
        addView(v, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        initView();
    }

    private void initView() {
        icon = (TransactionImmutureConfidenceIconView) findViewById(R.id.fl_confidence_icon);
        btnBtc = (BtcToMoneyButton) findViewById(R.id.btn_btc);
    }

    public void setTransaction(Tx transaction, Address address, long value) {
        this.transaction = transaction;
        this.address = address;
        showTransaction(value);
    }

    private void showTransaction(long value) {
        if (this.transaction == null || address == null) {
            return;
        }
        icon.setTx(transaction);
        btnBtc.setAmount(value);
    }

    public void onPause() {
        icon.onPause();
        btnBtc.onPause();
    }

    public void onResume() {
        icon.onResume();
        btnBtc.onResume();
    }


    public void onConfidenceBecomeMuture(Tx confidence) {
        setVisibility(View.INVISIBLE);
    }

    public void setActivity(FragmentActivity activity) {
        this.activity = activity;
    }

}
