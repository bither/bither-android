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

import java.io.IOException;
import java.math.BigInteger;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.bither.BitherSetting;
import net.bither.R;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.bitcoin.uri.BitcoinURIParseException;

public abstract class InputParser {
	public abstract static class StringInputParser extends InputParser {
		private final String input;

		public StringInputParser(@Nonnull final String input) {
			this.input = input;
		}

		@Override
		public void parse() {
			if (input.startsWith("bitcoin:")) {
				try {
					final BitcoinURI bitcoinUri = new BitcoinURI(null, input);
					final Address address = bitcoinUri.getAddress();
					final String addressLabel = bitcoinUri.getLabel();
					final BigInteger amount = bitcoinUri.getAmount();
					final String bluetoothMac = (String) bitcoinUri
							.getParameterByName(Bluetooth.MAC_URI_PARAM);

					bitcoinRequest(address, addressLabel, amount, bluetoothMac);
				} catch (final BitcoinURIParseException x) {
					error(R.string.input_parser_invalid_bitcoin_uri, input);
				}
			} else if (PATTERN_BITCOIN_ADDRESS.matcher(input).matches()) {
				try {
					final Address address = new Address(
							BitherSetting.NETWORK_PARAMETERS, input);

					bitcoinRequest(address, null, null, null);
				} catch (final AddressFormatException x) {
					error(R.string.input_parser_invalid_address);
				}
			} else if (PATTERN_PRIVATE_KEY.matcher(input).matches()) {
				try {
					final ECKey key = new DumpedPrivateKey(
							BitherSetting.NETWORK_PARAMETERS, input).getKey();
					final Address address = new Address(
							BitherSetting.NETWORK_PARAMETERS,
							key.getPubKeyHash());

					bitcoinRequest(address, null, null, null);
				} catch (final AddressFormatException x) {
					error(R.string.input_parser_invalid_address);
				}
			} else if (PATTERN_TRANSACTION.matcher(input).matches()) {
				try {
					final Transaction tx = new Transaction(
							BitherSetting.NETWORK_PARAMETERS,
							Qr.decodeBinary(input));

					directTransaction(tx);
				} catch (final IOException x) {
					error(R.string.input_parser_invalid_transaction,
							x.getMessage());
				} catch (final ProtocolException x) {
					error(R.string.input_parser_invalid_transaction,
							x.getMessage());
				}
			} else {
				cannotClassify(input);
			}
		}
	}

	public abstract static class BinaryInputParser extends InputParser {
		private final String inputType;
		private final byte[] input;

		public BinaryInputParser(@Nonnull final String inputType,
				@Nonnull final byte[] input) {
			this.inputType = inputType;
			this.input = input;
		}

		@Override
		public void parse() {
			if (BitherSetting.MIMETYPE_TRANSACTION.equals(inputType)) {
				try {
					final Transaction tx = new Transaction(
							BitherSetting.NETWORK_PARAMETERS, input);

					directTransaction(tx);
				} catch (final ProtocolException x) {
					error(R.string.input_parser_invalid_transaction,
							x.getMessage());
				}
			} else {
				cannotClassify(inputType);
			}
		}
	}

	public abstract void parse();

	protected abstract void bitcoinRequest(@Nonnull Address address,
			@Nullable String addressLabel, @Nullable BigInteger amount,
			@Nullable String bluetoothMac);

	protected abstract void directTransaction(@Nonnull Transaction transaction);

	protected abstract void error(int messageResId, Object... messageArgs);

	protected void cannotClassify(@Nonnull final String input) {
		error(R.string.input_parser_cannot_classify, input);
	}

	private static final Pattern PATTERN_BITCOIN_ADDRESS = Pattern.compile("["
			+ new String(Base58.ALPHABET) + "]{20,40}");
	private static final Pattern PATTERN_PRIVATE_KEY = Pattern.compile("5["
			+ new String(Base58.ALPHABET) + "]{50,51}");
	private static final Pattern PATTERN_TRANSACTION = Pattern
			.compile("[0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$\\*\\+\\-\\.\\/\\:]{100,}");
}
