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

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Set;

import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet.BalanceType;

public class AddressInfo implements Serializable {
	private static final long serialVersionUID = 609650192710235779L;
	private BigInteger balance;
	private int txCount;
	private BigInteger received;
	private BigInteger sent;

	public AddressInfo(BitherAddress address) {
		setBalance(address.getBalance(BalanceType.ESTIMATED));
		Set<Transaction> txs = address.getTransactions(false);
		setTxCount(txs.size());
		received = BigInteger.ZERO;
		sent = BigInteger.ZERO;
		for (Transaction tx : txs) {
			try {
				BigInteger value = tx.getValue(address);
				if (value.signum() > 0) {
					received = received.add(value.abs());
				}
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}
		sent = received.subtract(balance);
	}

	public BigInteger getReceived() {
		return received;
	}

	public BigInteger getSent() {
		return sent;
	}

	public BigInteger getBalance() {
		return balance;
	}

	public void setBalance(BigInteger balance) {
		this.balance = balance;
	}

	public int getTxCount() {
		return txCount;
	}

	public void setTxCount(int txSize) {
		this.txCount = txSize;
	}
}
