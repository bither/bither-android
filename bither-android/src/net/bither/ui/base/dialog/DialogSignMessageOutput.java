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

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;

import net.bither.R;
import net.bither.ui.base.DropdownMessage;
import net.bither.util.StringUtil;

/**
 * Created by songchenwen on 14/12/19.
 */
public class DialogSignMessageOutput extends CenterDialog implements View.OnClickListener,
        DialogInterface.OnDismissListener {
    private String output;
    private int clickedId;
    private Activity activity;

    public DialogSignMessageOutput(Activity context, String output) {
        super(context);
        setContentView(R.layout.dialog_sign_message_output);
        this.output = output;
        this.activity = context;
        findViewById(R.id.tv_copy).setOnClickListener(this);
        findViewById(R.id.tv_qr_code).setOnClickListener(this);
        findViewById(R.id.tv_close).setOnClickListener(this);
        setOnDismissListener(this);
    }

    @Override
    public void show() {
        clickedId = 0;
        super.show();
    }

    @Override
    public void onClick(View v) {
        clickedId = v.getId();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        switch (clickedId) {
            case R.id.tv_copy:
                StringUtil.copyString(output);
                DropdownMessage.showDropdownMessage(activity, R.string.sign_message_output_copied);
                break;
            case R.id.tv_qr_code:
                new DialogFancyQrCode(activity, output, false, true).show();
                break;
            default:
                break;
        }
    }
}
