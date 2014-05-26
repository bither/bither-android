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
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ToggleButton;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.ui.base.ProgressDialog;


/**
 * The activity can crop specific region of interest from an image.
 */
public class CropImageGlActivity extends CropImageGlActivityBase {
    private static final int FinishAnimDuration = 300;

    private boolean touchable;
    private ToggleButton tbtnFilter;
    private View fsv;
    private View llBottom;
    private View llTop;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        touchable = true;
        tbtnFilter = (ToggleButton) findViewById(R.id.tbtn_filter);
        //fsv = findViewById(R.id.sv_filters);
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


    private void afterHideFilters(final long timeStamp, final int filterId) {
        TranslateAnimation topHide = new TranslateAnimation(0, 0, 0,
                -llTop.getHeight());
        TranslateAnimation bottomHide = new TranslateAnimation(0, 0, 0,
                llBottom.getHeight());
        topHide.setFillAfter(true);
        bottomHide.setFillAfter(true);
        bottomHide.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
//				Intent intent = new Intent(CropImageGlActivity.this,
//						PostShareActivity.class);
//				intent.putExtra(PiCommonSetting.INTENT_REF.PIC_PASS_VALUE_TAG,
//						timeStamp);
//				intent.putExtra(
//						PiCommonSetting.INTENT_REF.FILTER_ID_PASS_VALUE_TAG,
//						filterId);
//				startActivity(intent);
//				finish();
//				overridePendingTransition(0, 0);
            }
        });
        topHide.setDuration(FinishAnimDuration);
        bottomHide.setDuration(FinishAnimDuration);
        llTop.startAnimation(topHide);
        llBottom.startAnimation(bottomHide);
    }

    @Override
    protected Boolean toShowSaveAnimation() {
//		if (getPiImageType().ordinal() == PiImageType.AVATAR.ordinal()
//				|| StringUtil.compareString(getIntent().getAction(),
//						PiCommonSetting.INTENT_REF.ACTION_COMMENT_ADD_PHOTO)) {
//			return super.toShowSaveAnimation();
//		} else {
//			return false;
//		}
        return true;
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
