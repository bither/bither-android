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
public class RawDataBinaryView extends FrameLayout {
    private int restrictedWidth;
    private int restrictedHeight;
    private int column;
    private int row;

    private ArrayList<Boolean> data;

    public RawDataBinaryView(Context context) {
        super(context);
    }

    public RawDataBinaryView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RawDataBinaryView(Context context, AttributeSet attrs, int defStyle) {
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
                View v = inflater.inflate(R.layout.layout_raw_data_item, null);
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
        data = new ArrayList<Boolean>(column * row);
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
        int byteCount = dataLength() / 8;
        byte[] bytes = new byte[byteCount];
        for (int i = 0;
             i < byteCount;
             i++) {
            bytes[i] = getByteFromData(i * 8, false, false);
        }
        return bytes;
    }

    public void clearData(){
        Collections.fill(data, Boolean.FALSE);
    }

    public void removeAllData(){
        int size = data.size();
        data.clear();
        for(int i = 0; i < size; i++){
            final ImageView iv = (ImageView) ((FrameLayout) getChildAt(i)).getChildAt(0);
            if(iv.getVisibility() == View.VISIBLE){
                ScaleAnimation anim = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF,
                        0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                anim.setDuration(300);
                anim.setFillAfter(true);
                iv.startAnimation(anim);
                iv.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        iv.setVisibility(View.INVISIBLE);
                    }
                }, 300);
            }
        }
    }

    public void deleteLast(){
        int size = data.size();
        if(size <= 0){
            return;
        }
        data.remove(size - 1);
        final ImageView iv = (ImageView) ((FrameLayout) getChildAt(size - 1)).getChildAt(0);
        if(iv.getVisibility() == View.VISIBLE){
            ScaleAnimation anim = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(300);
            anim.setFillAfter(true);
            iv.startAnimation(anim);
            iv.postDelayed(new Runnable() {
                @Override
                public void run() {
                    iv.setVisibility(View.INVISIBLE);
                }
            }, 300);
        }
    }

    public BigInteger testNextZeroValue() {
        int byteCount = dataLength() / 8;
        byte[] bytes = new byte[byteCount];
        for (int i = 0;
             i < byteCount;
             i++) {
            bytes[i] = getByteFromData(i * 8, true, false);
        }
        return new BigInteger(1, bytes);
    }

    public BigInteger testNextOneValue() {
        int byteCount = dataLength() / 8;
        byte[] bytes = new byte[byteCount];
        for (int i = 0;
             i < byteCount;
             i++) {
            bytes[i] = getByteFromData(i * 8, false, true);
        }
        return new BigInteger(1, bytes);
    }

    private byte getByteFromData(int start, boolean fill, boolean lastValue) {
        StringBuilder builder = new StringBuilder();
        for (int i = start;
             i < start + 8;
             i++) {
            Boolean value = fill;
            if (i < data.size()) {
                value = data.get(i);
            } else if (i == data.size()) {
                value = lastValue;
            }
            builder.append(value ? '1' : '0');
        }
        return (byte) Integer.parseInt(builder.toString(), 2);
    }

    public void addData(boolean d) {
        if (data.size() < dataLength()) {
            ImageView iv = (ImageView) ((FrameLayout) getChildAt(data.size())).getChildAt(0);
            data.add(Boolean.valueOf(d));
            iv.setVisibility(View.INVISIBLE);
            if (d) {
                iv.setBackgroundResource(R.color.raw_private_key_one);
            } else {
                iv.setBackgroundResource(R.color.raw_private_key_zero);
            }
            ScaleAnimation anim = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(300);
            iv.startAnimation(anim);
            iv.setVisibility(View.VISIBLE);
        }
    }
}
