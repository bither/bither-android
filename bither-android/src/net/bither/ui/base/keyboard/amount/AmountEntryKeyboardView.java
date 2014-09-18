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

package net.bither.ui.base.keyboard.amount;

import android.content.Context;
import android.util.AttributeSet;

import net.bither.R;
import net.bither.ui.base.keyboard.EntryKeyboardView;

/**
 * Created by songchenwen on 14-9-18.
 */
public class AmountEntryKeyboardView extends EntryKeyboardView {
    public AmountEntryKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AmountEntryKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected int getAlphaKeyboard() {
        return 0;
    }

    @Override
    protected int getNumericKeyboard() {
        return R.xml.amount_keyboard;
    }

    @Override
    protected int getDefaultKeyboardMode() {
        return EntryKeyboardView.KEYBOARD_MODE_NUMERIC;
    }
}
