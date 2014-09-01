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

import android.graphics.Paint;
import android.text.TextPaint;
import android.text.style.LineHeightSpan;

public class RelativeLineHeightSpan implements LineHeightSpan.WithDensity {
    private float rate;

    public RelativeLineHeightSpan(float rate) {
        this.rate = rate;
    }

    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v,
                             Paint.FontMetricsInt fm) {
        // Should not get called, at least not by StaticLayout.
        chooseHeight(text, start, end, spanstartv, v, fm, null);
    }

    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v,
                             Paint.FontMetricsInt fm, TextPaint paint) {
        int size = (int) ((fm.bottom - fm.top) * rate);
        fm.top = fm.bottom - size;
        fm.ascent = fm.ascent - size;
    }
}
