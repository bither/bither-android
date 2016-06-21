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

package net.bither.ui.base.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Layout;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import net.bither.R;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import java.util.List;

/**
 * Created by songchenwen on 16/6/13.
 */
public class DialogMonitorAddressesValidation extends CenterDialog implements DialogInterface
        .OnDismissListener, View.OnClickListener {
    private Runnable okRunnable;
    private int clickedId;

    public DialogMonitorAddressesValidation(Context context, List<String> addresses, Runnable
            okRunnable) {
        super(context);
        this.okRunnable = okRunnable;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_monitor_address_validation);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_ok).setOnClickListener(this);
        final LinearLayout llAddresses = (LinearLayout) findViewById(R.id.ll_addresses);
        for (int i = 0;
             i < addresses.size();
             i++) {
            TextView tv = new TextView(context);
            tv.setTextColor(getContext().getResources().getColor(R.color.white));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setText(WalletUtils.formatHash(addresses.get(i), 4, 16));
            llAddresses.addView(tv);
            if (i < addresses.size() - 1) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tv.getLayoutParams();
                lp.bottomMargin = UIUtil.dip2pix(4);
                tv.setLayoutParams(lp);
            }
        }
        final ScrollView sv = (ScrollView) llAddresses.getParent();
        llAddresses.measure(0, 0);
        ViewGroup.LayoutParams lp = sv.getLayoutParams();
        lp.height = Math.min(UIUtil.dip2pix(200), llAddresses.getMeasuredHeight());
        sv.setLayoutParams(lp);
    }

    @Override
    public void show() {
        super.show();
        clickedId = 0;
    }

    @Override
    public void onClick(View v) {
        clickedId = v.getId();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (clickedId == R.id.btn_ok && okRunnable != null) {
            okRunnable.run();
        }
    }
}
