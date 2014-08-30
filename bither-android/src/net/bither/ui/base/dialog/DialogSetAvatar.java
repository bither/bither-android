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
import android.content.DialogInterface;
import android.view.View;

import net.bither.R;

/**
 * Created by songchenwen on 14-5-23.
 */
public class DialogSetAvatar extends CenterDialog implements View.OnClickListener, DialogInterface.OnDismissListener {

    public static interface SetAvatarDelegate {
        public void avatarFromCamera();

        public void avatarFromGallery();
    }

    private int clickedView = 0;
    private SetAvatarDelegate delegate;

    public DialogSetAvatar(Context context, SetAvatarDelegate delegate) {
        super(context);
        this.delegate = delegate;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_set_avatar);
        initView();
    }

    private void initView() {
        findViewById(R.id.tv_camera).setOnClickListener(this);
        findViewById(R.id.tv_gallery).setOnClickListener(this);
        findViewById(R.id.tv_close).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        clickedView = v.getId();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if(delegate != null){
            switch (clickedView){
                case R.id.tv_camera:
                    delegate.avatarFromCamera();
                    break;
                case R.id.tv_gallery:
                    delegate.avatarFromGallery();
                    break;
                default:
                    break;
            }
        }
    }
}
