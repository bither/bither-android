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

package net.bither.ui.base.dialog;

import android.content.Context;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.util.UIUtil;
import net.bither.util.UnitUtilWrapper;

public class DialogBalanceDetail extends DialogWithArrow {
    private TextView tvTransactionCount;
    private TextView tvReceived;
    private TextView tvSent;

    public DialogBalanceDetail(Context context, Info info) {
        super(context);
        setContentView(R.layout.dialog_balance_detail);
        tvTransactionCount = (TextView) findViewById(R.id.tv_transaction_count);
        tvReceived = (TextView) findViewById(R.id.tv_received);
        tvSent = (TextView) findViewById(R.id.tv_sent);
        tvTransactionCount.setText(Integer.toString(info.txCount));
        tvReceived.setText(UnitUtilWrapper.formatValueWithBold(info.totalReceived));
        tvSent.setText(UnitUtilWrapper.formatValueWithBold(info.totalSent));
    }

    @Override
    public int getSuggestHeight() {
        return UIUtil.dip2pix(200);
    }


    public static final class Info{
        public int txCount;
        public long totalReceived;
        public long totalSent;

        public Info(Address address){
            txCount = address.txCount();
            totalReceived = address.totalReceive();
            totalSent = address.getBalance() - totalReceived;
        }
    }
}
