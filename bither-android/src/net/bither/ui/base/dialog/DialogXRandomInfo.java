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
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;

import net.bither.R;
import net.bither.preference.AppSharedPreference;
import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 14-9-19.
 */
public class DialogXRandomInfo extends CenterDialog implements View.OnClickListener {
    private CheckedTextView cbxAutoShowNegative;

    public DialogXRandomInfo(Context context) {
        this(context, false);
    }


    public DialogXRandomInfo(Context context, boolean guide) {
        this(context, guide, false);
    }

    public DialogXRandomInfo(Context context, boolean guide, boolean auto) {
        super(context);
        setContentView(R.layout.dialog_xrandom_info);
        findViewById(R.id.btn_ok).setOnClickListener(this);
        TextView tv = (TextView) findViewById(R.id.tv);
        if (guide) {
            tv.setText(context.getString(R.string.xrandom_info_detail) + context.getString(R
                    .string.xrandom_info_guide));
        } else {
            tv.setText(context.getString(R.string.xrandom_info_detail));
        }
        tv.setMaxWidth(UIUtil.getScreenWidth() - UIUtil.dip2pix(80));
        cbxAutoShowNegative = (CheckedTextView) findViewById(R.id.cbx_auto_show_negative);
        if (auto) {
            cbxAutoShowNegative.setVisibility(View.VISIBLE);
            cbxAutoShowNegative.setOnClickListener(this);
        } else {
            cbxAutoShowNegative.setVisibility(View.GONE);
        }
    }

    @Override
    public void dismiss() {
        if (cbxAutoShowNegative.getVisibility() == View.VISIBLE) {
            AppSharedPreference.getInstance().setAutoShowXRandomInstruction(!cbxAutoShowNegative
                    .isChecked());
        }
        super.dismiss();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_ok) {
            dismiss();
        } else if (v == cbxAutoShowNegative) {
            cbxAutoShowNegative.setChecked(!cbxAutoShowNegative.isChecked());
        }
    }

    public static final View.OnLongClickListener InfoLongClick = new View.OnLongClickListener(){

        @Override
        public boolean onLongClick(View v) {
            new DialogXRandomInfo(v.getContext()).show();
            return true;
        }
    };

    public static final View.OnClickListener GuideClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            new DialogXRandomInfo(v.getContext(), true).show();
        }
    };
}
