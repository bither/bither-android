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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Tx;

//import net.bither.bitherj.core.TransactionConfidence;

public class TransactionImmutureConfidenceIconView extends FrameLayout {

    private ImageView iv;
    private TextView tv;
    private Tx tx;

    public TransactionImmutureConfidenceIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public TransactionImmutureConfidenceIconView(Context context) {
        super(context);
        initView();
    }

    public TransactionImmutureConfidenceIconView(Context context, AttributeSet attrs,
                                                 int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout
                .layout_transaction_immuture_confidence_icon, this);
        iv = (ImageView) findViewById(R.id.iv_confidence);
        tv = (TextView) findViewById(R.id.tv_confidence);
        tv.setVisibility(View.GONE);
    }

    public void setTx(Tx tx) {
        this.tx = tx;
        showConfidence();
    }

    public Tx getTx() {
        return tx;
    }

    public void onPause() {
    }

    public void onResume() {
        showConfidence();
    }

    private void showConfidence() {
        if (tx == null) {
            return;
        }
        int depth = tx.getConfirmationCount();
        switch (depth) {
            case 0:
                iv.setImageResource(R.drawable.transaction_pending_icon);
                break;
            case 1:
                iv.setImageResource(R.drawable.transaction_building_icon_1);
                break;
            case 2:
                iv.setImageResource(R.drawable.transaction_building_icon_2);
                break;
            case 3:
                iv.setImageResource(R.drawable.transaction_building_icon_3);
                break;
            case 4:
                iv.setImageResource(R.drawable.transaction_building_icon_4);
                break;
            case 5:
                iv.setImageResource(R.drawable.transaction_building_icon_5);
                break;
            case 6:
                iv.setImageResource(R.drawable.transaction_building_icon_6);
                break;
            default:
                iv.setImageResource(R.drawable.transaction_building_icon_6);
                break;
        }
        if (depth >= 100) {
            iv.setImageResource(R.drawable.transaction_building_icon_100);
        }

        int textColor = getResources().getColor(R.color.white);
        if (depth > 0 && depth < 4) {
            textColor = getResources().getColor(R.color.orange);
        } else if (depth > 3) {
            textColor = getResources().getColor(R.color.confidence_blue);
        }
        if (depth < 10) {
            tv.setText(Integer.toString(depth));
        } else {
            tv.setText("9+");
        }
        tv.setTextColor(textColor);
    }

}
