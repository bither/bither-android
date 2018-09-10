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


import net.bither.R;
import net.bither.bitherj.core.SplitCoin;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Utils;
import net.bither.image.glcrop.Util;

import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class InputParser {
    public abstract static class StringInputParser extends InputParser {
        private final String input;
        private final SplitCoin splitCoin;

        public StringInputParser(@Nonnull final String input, SplitCoin splitCoin) {
            this.input = input;
            this.splitCoin = splitCoin;
        }

        @Override
        public void parse() {
            if (input.startsWith("bitcoin:")) {
                try {
                    final BitcoinURI bitcoinUri = new BitcoinURI(input);
                    final String address = bitcoinUri.getAddress();
                    final String addressLabel = bitcoinUri.getLabel();
                    final long amount = bitcoinUri.getAmount();
                    final String bluetoothMac = (String) bitcoinUri.getParameterByName(Bluetooth
                            .MAC_URI_PARAM);
                    bitcoinRequest(address, addressLabel, amount, bluetoothMac);
                } catch (final BitcoinURI.BitcoinURIParseException x) {
                    error(R.string.input_parser_invalid_bitcoin_uri, input);
                }
            } else if (PATTERN_BITCOIN_ADDRESS.matcher(input).matches() && splitCoin != null) {
                if (Utils.validSplitBitCoinAddress(input,splitCoin)) {
                    bitcoinRequest(input, null, 0, null);
                } else {
                    error(R.string.input_parser_invalid_address);
                }
            }else if(PATTERN_BITCOIN_ADDRESS.matcher(input).matches()) {
                if (Utils.validBicoinAddress(input)) {
                    bitcoinRequest(input, null, 0, null);
                } else {
                    error(R.string.input_parser_invalid_address);
                }

            } else {
                cannotClassify(input);
            }
        }
    }

    public abstract void parse();

    protected abstract void bitcoinRequest(@Nonnull String address,
                                           @Nullable String addressLabel, @Nullable long amount,
                                           @Nullable String bluetoothMac);

    protected abstract void error(int messageResId, Object... messageArgs);

    protected void cannotClassify(@Nonnull final String input) {
        error(R.string.input_parser_cannot_classify, input);
    }

    private static final Pattern PATTERN_BITCOIN_ADDRESS = Pattern.compile("[" + new String
            (Base58.ALPHABET) + "]{20,40}");
    private static final Pattern PATTERN_PRIVATE_KEY = Pattern.compile("5[" + new String(Base58
            .ALPHABET) + "]{50,51}");
    private static final Pattern PATTERN_TRANSACTION = Pattern.compile
            ("[0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$\\*\\+\\-\\.\\/\\:]{100,}");
}
