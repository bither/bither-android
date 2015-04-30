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

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.Utils;
import net.bither.util.WalletUtils;

public class DialogPrivateKeyText extends CenterDialog implements View
        .OnClickListener, DialogInterface.OnDismissListener {
    private Activity activity;
    private SecureCharSequence mPrivateKeyText;
    private TextView tvPrivateKeyText;

    public DialogPrivateKeyText(Activity context, SecureCharSequence privateKeyText) {
        super(context);
        this.activity = context;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_address_with_show_private_key_text);
        tvPrivateKeyText = (TextView) findViewById(R.id.tv_view_show_private_key_text);
        mPrivateKeyText = Utils.formatHashFromCharSequence(privateKeyText, 4, 16);
        tvPrivateKeyText.setText(mPrivateKeyText);
        privateKeyText.wipe();
        findViewById(R.id.tv_close).setOnClickListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mPrivateKeyText.wipe();
    }



    @Override
    public void onClick(View v) {
        dismiss();
    }


}
