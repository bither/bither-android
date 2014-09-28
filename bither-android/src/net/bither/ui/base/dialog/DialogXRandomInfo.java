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
import android.widget.TextView;

import net.bither.R;
import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 14-9-19.
 */
public class DialogXRandomInfo extends CenterDialog implements View.OnClickListener {
    public DialogXRandomInfo(Context context) {
        this(context, false);
    }

    public DialogXRandomInfo(Context context, boolean guide) {
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
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    public static final View.OnClickListener InfoClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            new DialogXRandomInfo(v.getContext()).show();
        }
    };

    public static final View.OnClickListener GuideClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            new DialogXRandomInfo(v.getContext(), true).show();
        }
    };
}
