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

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.model.BitherAddress;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;

/**
 * Created by nn on 14-7-22.
 */
public class DialogPrivateKeyText extends CenterDialog implements View
        .OnClickListener, DialogInterface.OnDismissListener {
    private Activity activity;
    private String mPrivateKeyText;
    private TextView tvPrivateKeyText;

    public DialogPrivateKeyText(Activity context, String privateKeyText) {
        super(context);
        this.mPrivateKeyText = privateKeyText;
        this.activity = context;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_address_with_show_private_key_text);
        tvPrivateKeyText = (TextView) findViewById(R.id.tv_view_show_private_key_text);
        tvPrivateKeyText.setText(WalletUtils.formatHash(this.mPrivateKeyText, 4, 16));
        findViewById(R.id.tv_copy).setOnClickListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
    }

    @Override
    public void onClick(View v) {
        dismiss();
        StringUtil.copyString(this.mPrivateKeyText);
        DropdownMessage.showDropdownMessage(activity, R.string.copy_private_key_success);
    }


}
