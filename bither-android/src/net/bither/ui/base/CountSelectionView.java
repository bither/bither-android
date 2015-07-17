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

package net.bither.ui.base;

import android.content.Context;
import android.util.AttributeSet;

import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.AbstractWheelTextAdapter;

/**
 * Created by songchenwen on 15/6/12.
 */
public class CountSelectionView extends WheelView {
    public static final int DefaultMax = 10;

    private int max;
    private int min;

    public CountSelectionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CountSelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CountSelectionView(Context context) {
        super(context);
        init();
    }

    private void init() {
        min = 0;
        max = DefaultMax;
        setViewAdapter(countAdapter);
    }

    public CountSelectionView setMax(int max) {
        this.max = max;
        invalidateWheel(true);
        return this;
    }

    public CountSelectionView setMin(int min) {
        this.min = min;
        invalidateWheel(true);
        return this;
    }

    public int max(){
        return max;
    }

    public int min(){
        return min;
    }

    public CountSelectionView setSelectedCount(int count) {
        setCurrentItem(count - min);
        return this;
    }

    public CountSelectionView setSelectedCountAnimated(int count) {
        setCurrentItem(count - min, true);
        return this;
    }

    public int selectedCount() {
        return countAtIndex(getCurrentItem());
    }

    public int countAtIndex(int index){
        return index + min;
    }

    private AbstractWheelTextAdapter countAdapter = new AbstractWheelTextAdapter(getContext()) {
        @Override
        public int getItemsCount() {
            return max - min + 1;
        }

        @Override
        protected CharSequence getItemText(int index) {
            return textForCount(index + min);
        }
    };

    protected CharSequence textForCount(int count) {
        return String.valueOf(count);
    }
}
