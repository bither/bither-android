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

import net.bither.R;
import net.bither.preference.AppSharedPreference;

public class DialogFirstRunWarning extends CenterDialog implements View.OnClickListener {
    public DialogFirstRunWarning(Context context) {
        super(context);
        setContentView(R.layout.dialog_first_run_warning);
        setCanceledOnTouchOutside(false);
        findViewById(R.id.btn_ok).setOnClickListener(this);
    }

    @Override
    public void show() {
        if (!AppSharedPreference.getInstance().getFirstRunDialogShown()) {
            AppSharedPreference.getInstance().setFirstRunDialogShown(true);
            super.show();
        }
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    public static final void show(Context context) {
        if (!AppSharedPreference.getInstance().getFirstRunDialogShown()) {
            new DialogFirstRunWarning(context).show();
        }
    }
}
