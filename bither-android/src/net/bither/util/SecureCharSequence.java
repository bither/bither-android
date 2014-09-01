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

package net.bither.util;

import android.widget.TextView;

import java.util.Arrays;

public class SecureCharSequence implements CharSequence {
    private char[] chars;

    public SecureCharSequence(CharSequence charSequence) {
        this(charSequence, 0, charSequence.length());
    }

    public SecureCharSequence(TextView tv) {
        this(tv.getText());
    }

    private SecureCharSequence(CharSequence charSequence, int start, int end) {
        // pulled from http://stackoverflow.com/a/15844273
        wipe();
        int length = end - start;
        chars = new char[length];
        for (int i = start;
             i < end;
             i++) {
            chars[i - start] = charSequence.charAt(i);
        }
    }

    public void wipe() {
        if (chars != null) {
            Arrays.fill(chars, ' ');
        }
    }

    protected void finalize() {
        wipe();
    }

    @Override
    public int length() {
        if (chars != null) {
            return chars.length;
        }
        return 0;
    }

    @Override
    public char charAt(int index) {
        if (chars != null) {
            return chars[index];
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.valueOf(this.chars);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SecureCharSequence) {
            return Arrays.equals(chars, ((SecureCharSequence) o).chars);
        }
        return false;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        SecureCharSequence s = new SecureCharSequence(this, start, end);
        return s;
    }
}
