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

package net.bither.image.glcrop;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.ui.base.dialog.ProgressDialog;


/**
 * The activity can crop specific region of interest from an image.
 */
public class CropImageGlActivity extends CropImageGlActivityBase {
    private static final int FinishAnimDuration = 300;
    private boolean touchable;
    private View llBottom;
    private View llTop;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        touchable = true;
        llBottom = findViewById(R.id.ll_bottom);
        llTop = findViewById(R.id.rl_title_bar);
    }

    @Override
    protected void handleSaveSuccess(String photoName) {

        Intent resultIntent = new Intent();
        resultIntent.putExtra(
                BitherSetting.INTENT_REF.PIC_PASS_VALUE_TAG, photoName);

        setResult(RESULT_OK, resultIntent);
        finish();
        overridePendingTransition(R.anim.camera_to_share_in,
                R.anim.slide_out_bottom);

    }

    @Override
    protected Dialog getProgressDialog(String msg) {
        return new ProgressDialog(this, msg, null);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!touchable) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }
}
