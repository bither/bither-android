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

package net.bither.util;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import com.google.zxing.common.StringUtils;

import net.bither.AndroidImplAbstractApp;
import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.exception.ScriptException;
import net.bither.bitherj.script.Script;
import net.bither.bitherj.utils.Utils;
import net.bither.image.glcrop.Util;
import net.bither.preference.AppSharedPreference;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WalletUtils {
    public static final RelativeSizeSpan SMALLER_SPAN = new RelativeSizeSpan(
            0.85f);


    private static final Pattern P_SIGNIFICANT = Pattern.compile("^([-+]"
            + BitherSetting.CHAR_THIN_SPACE + ")?\\d*(\\.\\d{0,2})?");
    private static final Object SIGNIFICANT_SPAN = new StyleSpan(Typeface.BOLD);


    public static Editable formatHash(@Nonnull final String address,
                                      final int groupSize, final int lineSize) {
        return formatHash(null, address, groupSize, lineSize,
                BitherSetting.CHAR_THIN_SPACE);
    }

    // remeber to wipe #address
    public static SecureCharSequence formatHashFromCharSequence(@Nonnull final SecureCharSequence address, final int groupSize, final int lineSize){
        int length = address.length();
        length = length + length / groupSize - 1;
        char[] chars = new char[length];
        for(int i = 0; i < length; i++){
            if(i % (groupSize + 1) == groupSize){
                if((i + 1) % (lineSize + lineSize / groupSize) == 0){
                    chars[i] = '\n';
                }else{
                    chars[i] = ' ';
                }
            } else {
                chars[i] = address.charAt(i - i / (groupSize + 1));
            }
        }
        return new SecureCharSequence(chars);
    }

    public static boolean isInternal(@Nonnull final Tx tx) {
        if (tx.isCoinBase()) {
            return false;
        }

        final List<Out> outputs = tx.getOuts();
        if (outputs.size() != 1) {
            return false;
        }

        try {
            final Out output = outputs.get(0);
            final Script scriptPubKey = output.getScriptPubKey();
            if (!scriptPubKey.isSentToRawPubKey()) {
                return false;
            }

            return true;
        } catch (final ScriptException x) {
            return false;
        }
    }

    public static Editable formatHash(@Nullable final String prefix,
                                      @Nonnull final String address, final int groupSize,
                                      final int lineSize, final char groupSeparator) {
        final SpannableStringBuilder builder = prefix != null ? new SpannableStringBuilder(
                prefix) : new SpannableStringBuilder();

        final int len = address.length();
        for (int i = 0;
             i < len;
             i += groupSize) {
            final int end = i + groupSize;
            final String part = address.substring(i, end < len ? end : len);

            builder.append(part);
            builder.setSpan(new TypefaceSpan("monospace"), builder.length()
                            - part.length(), builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            if (end < len) {
                final boolean endOfLine = lineSize > 0 && end % lineSize == 0;
                builder.append(endOfLine ? '\n' : groupSeparator);
            }
        }

        return builder;
    }

    public static void formatSignificant(@Nonnull final Editable s,
                                         @Nullable final RelativeSizeSpan
                                                 insignificantRelativeSizeSpan) {
        s.removeSpan(SIGNIFICANT_SPAN);
        if (insignificantRelativeSizeSpan != null) {
            s.removeSpan(insignificantRelativeSizeSpan);
        }

        final Matcher m = P_SIGNIFICANT.matcher(s);
        if (m.find()) {
            final int pivot = m.group().length();
            s.setSpan(SIGNIFICANT_SPAN, 0, pivot,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (s.length() > pivot && insignificantRelativeSizeSpan != null) {
                s.setSpan(insignificantRelativeSizeSpan, pivot, s.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }


    public static Address findPrivateKey(String address) {
        for (Address bitherAddressWithPrivateKey : AddressManager.getInstance().getPrivKeyAddresses()) {

            if (Utils.compareString(address,
                    bitherAddressWithPrivateKey.getAddress())) {
                return bitherAddressWithPrivateKey;
            }
        }
        return null;
    }


    public static boolean isPrivateLimit() {
        int maxPrivateKey = AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD ?
                BitherSetting.WATCH_ONLY_ADDRESS_COUNT_LIMIT
                : BitherSetting.PRIVATE_KEY_OF_HOT_COUNT_LIMIT;
        return AddressManager.getInstance().getPrivKeyAddresses() != null
                && AddressManager.getInstance().getPrivKeyAddresses().size() >= maxPrivateKey;
    }

    public static boolean isWatchOnlyLimit() {
        return AddressManager.getInstance().getWatchOnlyAddresses() != null
                && AddressManager.getInstance().getWatchOnlyAddresses().size() >= BitherSetting
                .WATCH_ONLY_ADDRESS_COUNT_LIMIT;
    }

    public static boolean isHDMLimit() {
        if(AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD){
            return AddressManager.getInstance().getHdmKeychain() != null;
        } else {
            if(AddressManager.getInstance().getHdmKeychain() == null){
                return false;
            }
            return AddressManager.getInstance().getHdmKeychain().getAddresses().size() >= BitherSetting.HDM_ADDRESS_PER_SEED_COUNT_LIMIT;
        }
    }
}
