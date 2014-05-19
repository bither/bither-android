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

package net.bither.model;

import java.io.IOException;
import java.io.InputStream;

import org.bitcoinj.wallet.Protos;

import com.google.bitcoin.store.UnreadableWalletException;

public class BitherAddressWithPrivateKeyProtobufSerializer extends
		BaseProtobufSerializer {
	@Override
	public BitherAddressWithPrivateKey readWallet(InputStream input)
			throws UnreadableWalletException {
		Protos.Wallet walletProto = null;
		try {
			walletProto = parseToProto(input);
		} catch (IOException e) {
			throw new UnreadableWalletException("Could not load wallet file", e);
		}

		BitherAddressWithPrivateKey bitherAddress = new BitherAddressWithPrivateKey(
				false);
		readWallet(walletProto, bitherAddress);
		return bitherAddress;
	}

}
