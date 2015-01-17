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
import android.text.SpannableString;
import android.widget.TextView;

import net.bither.R;

public class DialogUpgrade extends CenterDialog {
    private TextView tvMsg;

    public DialogUpgrade(Context context) {
        super(context);
        setContentView(R.layout.dialog_upgrade);
        tvMsg = (TextView) findViewById(R.id.tv_message);


    }

    public void setMessage(SpannableString msg) {
        tvMsg.setText(msg);
    }
}
