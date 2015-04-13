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

import net.bither.BitherApplication;
import net.bither.R;

/**
 * Created by songchenwen on 2015/4/13.
 */
public class PasswordStrengthUtil {
    public static final PasswordStrength PassingPasswordStrength = PasswordStrength.Normal;
    public static final PasswordStrength WarningPasswordStrength = PasswordStrength.Medium;

    public enum PasswordStrength {
        Weak(0), Normal(1), Medium(2), Strong(3), VeryStrong(4);

        private int value;

        PasswordStrength(int value) {
            this.value = value;
        }

        public int getNameRes() {
            switch (this) {
                case Normal:
                    return R.string.password_strength_normal;
                case Medium:
                    return R.string.password_strength_medium;
                case Strong:
                    return R.string.password_strength_strong;
                case VeryStrong:
                    return R.string.password_strength_very_strong;
                default:
                    return R.string.password_strength_weak;
            }
        }

        public String getName() {
            return BitherApplication.mContext.getString(getNameRes());
        }

        public int getColorRes() {
            switch (this) {
                case Normal:
                    return R.color.password_strength_normal;
                case Medium:
                    return R.color.password_strength_medium;
                case Strong:
                    return R.color.password_strength_strong;
                case VeryStrong:
                    return R.color.password_strength_very_strong;
                default:
                    return R.color.password_strength_weak;
            }
        }

        public int getColor() {
            return BitherApplication.mContext.getResources().getColor(getColorRes());
        }

        public int getValue() {
            return value;
        }

        public boolean passed() {
            return getValue() >= PassingPasswordStrength.getValue();
        }

        public boolean warning() {
            return passed() && getValue() <= WarningPasswordStrength.getValue();
        }
    }

    public static PasswordStrength checkPassword(CharSequence password) {
        switch (getRating(password)) {
            case 0:
                return PasswordStrength.Weak;
            case 1:
                return PasswordStrength.Normal;
            case 2:
                return PasswordStrength.Medium;
            case 3:
                return PasswordStrength.Strong;
            default:
                return PasswordStrength.VeryStrong;
        }
    }

    private static int getRating(CharSequence password) {
        if (password == null || password.length() < 6) {
            return 0;
        }
        int strength = 0;
        if (password.length() > 9) {
            strength++;
        }
        int digitCount = getDigitCount(password);
        int symbolCount = getSymbolCount(password);
        boolean upperAndLower = bothUpperAndLower(password);
        if (digitCount > 0 && digitCount != password.length()) {
            strength++;
        }
        if (symbolCount > 0 && symbolCount != password.length()) {
            strength++;
        }
        if (upperAndLower) {
            strength++;
        }
        return strength;
    }

    private static boolean bothUpperAndLower(CharSequence password) {
        if (password == null || password.length() == 0) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        int length = password.length();
        for (int i = 0;
             i < length;
             i++) {
            char c = password.charAt(i);
            if (!upper) {
                upper = Character.isUpperCase(c);
            }
            if (!lower) {
                lower = Character.isLowerCase(c);
            }
            if (upper && lower) {
                break;
            }
        }
        return upper && lower;
    }

    private static int getDigitCount(CharSequence password) {
        if (password == null || password.length() == 0) {
            return 0;
        }
        int numDigits = 0;
        int length = password.length();
        for (int i = 0;
             i < length;
             i++) {
            if (Character.isDigit(password.charAt(i))) {
                numDigits++;
            }
        }
        return numDigits;
    }

    private static int getSymbolCount(CharSequence password) {
        if (password == null || password.length() == 0) {
            return 0;
        }
        int numSymbol = 0;
        int length = password.length();
        for (int i = 0;
             i < length;
             i++) {
            char c = password.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c)) {
                numSymbol++;
            }
        }
        return numSymbol;
    }
}
