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

package net.bither.rawprivatekey;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import net.bither.R;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by songchenwen on 14/12/4.
 */
public class RawDataDiceView extends FrameLayout {
    public static enum Dice {
        One(0), Two(1), Three(2), Four(3), Five(4), Six(5);

        private int value;

        Dice(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private int restrictedWidth;
    private int restrictedHeight;
    private int column;
    private int row;

    private ArrayList<Dice> data;

    public RawDataDiceView(Context context) {
        super(context);
    }

    public RawDataDiceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RawDataDiceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setRestrictedSize(int width, int height) {
        restrictedHeight = height;
        restrictedWidth = width;
        organizeView();
    }

    private void organizeView() {
        if (restrictedWidth <= 0 || restrictedHeight <= 0 || column <= 0 || row <= 0) {
            return;
        }
        removeAllViews();
        setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable
                .border_bottom_right));
        configureSize();
        double width = (double) (getLayoutParams().width - getPaddingRight() - getPaddingLeft())
                / (double) column;
        double height = (double) (getLayoutParams().height - getPaddingBottom() - getPaddingTop()
        ) / (double) row;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        LayoutParams lp;
        for (int y = 0;
             y < row;
             y++) {
            for (int x = 0;
                 x < column;
                 x++) {
                lp = new LayoutParams((int) width, (int) height, Gravity.LEFT | Gravity.TOP);
                lp.topMargin = (int) ((double) y * height);
                lp.leftMargin = (int) ((double) x * width);
                FrameLayout v = (FrameLayout) inflater.inflate(R.layout
                        .layout_raw_data_item_dice, null);
                v.setLayoutParams(lp);
                addView(v);
            }
        }
    }

    private void configureSize() {
        int width = restrictedWidth - getPaddingRight() - getPaddingLeft();
        int height = restrictedHeight - getPaddingBottom() - getPaddingTop();
        width = width - width % column;
        height = height - height % row;
        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.width = width + getPaddingRight() + getPaddingLeft();
        lp.height = height + getPaddingBottom() + getPaddingTop();
        setLayoutParams(lp);
    }

    public void setDataSize(int column, int row) {
        this.column = column;
        this.row = row;
        data = new ArrayList<Dice>(column * row);
        organizeView();
    }

    public int dataLength() {
        return column * row;
    }

    public int filledDataLength() {
        if (data == null) {
            return 0;
        }
        return data.size();
    }

    public byte[] getData() {
        if (filledDataLength() < dataLength()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0;
             i < data.size();
             i++) {
            builder.append(String.valueOf(data.get(i).getValue()));
        }
        return new BigInteger(builder.toString(), 6).toByteArray();
    }

    public void clearData() {
        Collections.fill(data, Dice.One);
    }

    public void removeAllData() {
        int size = data.size();
        data.clear();
        for (int i = 0;
             i < size;
             i++) {
            final ImageView iv = (ImageView) ((FrameLayout) getChildAt(i)).getChildAt(0);
            if (iv.getVisibility() == View.VISIBLE) {
                ScaleAnimation anim = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF,
                        0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                anim.setDuration(300);
                anim.setFillAfter(true);
                iv.startAnimation(anim);
                if (iv.getTag() != null && iv.getTag() instanceof HideIvRunnable) {
                    iv.removeCallbacks((Runnable) iv.getTag());
                }
                HideIvRunnable r = new HideIvRunnable(iv);
                iv.setTag(r);
                iv.postDelayed(r, 300);
            }
        }
    }

    public void deleteLast() {
        int size = data.size();
        if (size <= 0) {
            return;
        }
        data.remove(size - 1);
        final ImageView iv = (ImageView) ((FrameLayout) getChildAt(size - 1)).getChildAt(0);
        if (iv.getVisibility() == View.VISIBLE) {
            ScaleAnimation anim = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(300);
            anim.setFillAfter(true);
            iv.startAnimation(anim);
            if (iv.getTag() != null && iv.getTag() instanceof HideIvRunnable) {
                iv.removeCallbacks((Runnable) iv.getTag());
            }
            HideIvRunnable r = new HideIvRunnable(iv);
            iv.setTag(r);
            iv.postDelayed(r, 300);
        }
    }

    private class HideIvRunnable implements Runnable {
        private ImageView iv;

        HideIvRunnable(ImageView iv) {
            this.iv = iv;
        }

        @Override
        public void run() {
            iv.setVisibility(View.INVISIBLE);
        }
    }

    public void addData(Dice d) {
        if (data.size() < dataLength()) {
            ImageView iv = (ImageView) ((FrameLayout) getChildAt(data.size())).getChildAt(0);
            if (iv.getTag() != null && iv.getTag() instanceof HideIvRunnable) {
                iv.removeCallbacks((Runnable) iv.getTag());
            }
            data.add(d);
            iv.setVisibility(View.INVISIBLE);
            iv.setImageResource(getDrawable(d));
            ScaleAnimation anim = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(300);
            iv.startAnimation(anim);
            iv.setVisibility(View.VISIBLE);
        }
    }

    private int getDrawable(Dice d) {
        switch (d) {
            case One:
                return R.drawable.dice_large_1;
            case Two:
                return R.drawable.dice_large_2;
            case Three:
                return R.drawable.dice_large_3;
            case Four:
                return R.drawable.dice_large_4;
            case Five:
                return R.drawable.dice_large_5;
            case Six:
                return R.drawable.dice_large_6;
            default:
                return R.drawable.dice_large_1;
        }
    }
}
