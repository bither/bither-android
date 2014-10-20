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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.R;

/**
 * Created by songchenwen on 14-10-20.
 */
public class DialogRCheck extends ProgressDialog {
    private ProgressBar pb;
    private ImageView ivSuccess;
    private TextView tv;

    public DialogRCheck(Context context) {
        super(context, context.getString(R.string.please_wait), null);
        setContentView(R.layout.dialog_r_check);
        pb = (ProgressBar) findViewById(R.id.pb);
        ivSuccess = (ImageView) findViewById(R.id.iv_success);
        tv = (TextView) findViewById(R.id.tv_message);
        setWait();
    }

    public void setWait() {
        pb.setVisibility(View.VISIBLE);
        ivSuccess.setVisibility(View.GONE);
        tv.setText(R.string.please_wait);
    }

    public void setRChecking() {
        pb.setVisibility(View.VISIBLE);
        ivSuccess.setVisibility(View.GONE);
        tv.setText(R.string.rchecking);
    }

    public void setRCheckSuccess() {
        pb.setVisibility(View.GONE);
        ivSuccess.setVisibility(View.VISIBLE);
        tv.setText(R.string.rcheck_safe);
    }
}
