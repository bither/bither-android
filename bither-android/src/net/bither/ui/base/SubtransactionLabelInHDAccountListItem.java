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

package net.bither.ui.base;

import android.app.Activity;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 15/7/2.
 */
public class SubtransactionLabelInHDAccountListItem extends FrameLayout {
    public static final int MessageHeight = UIUtil.dip2pix(30);

    private TextView tv;
    private FrameLayout parent;
    private Activity activity;

    public SubtransactionLabelInHDAccountListItem(Activity context) {
        super(context);
        activity = context;
        removeAllViews();
        parent = new FrameLayout(context);
        addView(parent, new LayoutParams(LayoutParams.MATCH_PARENT, MessageHeight));
        tv = new TextView(context);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        lp.bottomMargin = UIUtil.dip2pix(1);
        parent.addView(tv, lp);
    }

    public void setTextColor(int color) {
        tv.setTextColor(color);
    }

    public void setContent(boolean own) {
        tv.setText(own ? R.string.address_full_for_hd_label_own : R.string
                .address_full_for_hd_label_foreign);
    }
}
