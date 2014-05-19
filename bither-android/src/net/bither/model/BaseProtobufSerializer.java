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

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.Protos.Wallet.EncryptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.crypto.EncryptedPrivateKey;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterScrypt;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.store.WalletProtobufSerializer;
import com.google.bitcoin.wallet.WalletTransaction;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

public class BaseProtobufSerializer extends WalletProtobufSerializer {

	private static final Logger log = LoggerFactory
			.getLogger(BitherAddress.class);

	public void readWallet(Protos.Wallet walletProto, Wallet wallet)
			throws UnreadableWalletException {
		// Read the scrypt parameters that specify how encryption and decryption
		// is performed.
		if (walletProto.hasEncryptionParameters()) {
			Protos.ScryptParameters encryptionParameters = walletProto
					.getEncryptionParameters();
			wallet.setKeyCrypter(new KeyCrypterScrypt(encryptionParameters));
		}

		if (walletProto.hasDescription()) {
			wallet.setDescription(walletProto.getDescription());
		}

		// Read all keys
		for (Protos.Key keyProto : walletProto.getKeyList()) {
			if (!(keyProto.getType() == Protos.Key.Type.ORIGINAL || keyProto
					.getType() == Protos.Key.Type.ENCRYPTED_SCRYPT_AES)) {
				throw new UnreadableWalletException(
						"Unknown key type in wallet, type = "
								+ keyProto.getType());
			}

			byte[] privKey = keyProto.hasPrivateKey() ? keyProto
					.getPrivateKey().toByteArray() : null;
			EncryptedPrivateKey encryptedPrivateKey = null;
			if (keyProto.hasEncryptedPrivateKey()) {
				Protos.EncryptedPrivateKey encryptedPrivateKeyProto = keyProto
						.getEncryptedPrivateKey();
				encryptedPrivateKey = new EncryptedPrivateKey(
						encryptedPrivateKeyProto.getInitialisationVector()
								.toByteArray(), encryptedPrivateKeyProto
								.getEncryptedPrivateKey().toByteArray());
			}

			byte[] pubKey = keyProto.hasPublicKey() ? keyProto.getPublicKey()
					.toByteArray() : null;

			ECKey ecKey;
			final KeyCrypter keyCrypter = wallet.getKeyCrypter();
			if (keyCrypter != null
					&& keyCrypter.getUnderstoodEncryptionType() != EncryptionType.UNENCRYPTED) {
				// If the key is encrypted construct an ECKey using the
				// encrypted private key bytes.
				ecKey = new ECKey(encryptedPrivateKey, pubKey, keyCrypter);
			} else {
				// Construct an unencrypted private key.
				ecKey = new ECKey(privKey, pubKey);
			}
			ecKey.setCreationTimeSeconds((keyProto.getCreationTimestamp() + 500) / 1000);
			wallet.addKey(ecKey);
		}

		List<Script> scripts = Lists.newArrayList();
		for (Protos.Script protoScript : walletProto.getWatchedScriptList()) {
			try {
				Script script = new Script(protoScript.getProgram()
						.toByteArray(),
						protoScript.getCreationTimestamp() / 1000);
				scripts.add(script);
			} catch (ScriptException e) {
				throw new UnreadableWalletException(
						"Unparseable script in wallet");
			}
		}

		wallet.addWatchedScripts(scripts);

		// Read all transactions and insert into the txMap.
		for (Protos.Transaction txProto : walletProto.getTransactionList()) {
			readTransaction(txProto, wallet.getParams());
		}
		Class<?> walletProtobufClass = new WalletProtobufSerializer()
				.getClass();
		Method method = null;
		// try {
		// method = walletProtobufClass.getDeclaredMethod(
		// "connectTransactionOutputs",
		// org.bitcoinj.wallet.Protos.Transaction.class);
		// method.setAccessible(true);
		// } catch (NoSuchMethodException e1) {
		// log.info("read wallet error NoSuchMethodException:connectTransactionOutputs");
		// e1.printStackTrace();
		// }
		// Update transaction outputs to point to inputs that spend them
		for (Protos.Transaction txProto : walletProto.getTransactionList()) {

			try {
				WalletTransaction wtx = connectTransactionOutputs(txProto);
				wallet.addWalletTransaction(wtx);
			} catch (Exception e) {
				log.info("read tx error connectTransactionOutputs:"
						+ byteStringToHash(txProto.getHash()).toString());
				e.printStackTrace();
			}

		}

		// Update the lastBlockSeenHash.
		if (!walletProto.hasLastSeenBlockHash()) {
			wallet.setLastBlockSeenHash(null);
		} else {
			wallet.setLastBlockSeenHash(byteStringToHash(walletProto
					.getLastSeenBlockHash()));
		}
		if (!walletProto.hasLastSeenBlockHeight()) {
			wallet.setLastBlockSeenHeight(-1);
		} else {
			wallet.setLastBlockSeenHeight(walletProto.getLastSeenBlockHeight());
		}
		// Will default to zero if not present.
		wallet.setLastBlockSeenTimeSecs(walletProto.getLastSeenBlockTimeSecs());

		if (walletProto.hasKeyRotationTime()) {
			wallet.setKeyRotationTime(new Date(
					walletProto.getKeyRotationTime() * 1000));
		}
		try {

			method = walletProtobufClass.getDeclaredMethod("loadExtensions",
					Wallet.class, Protos.Wallet.class);
			method.setAccessible(true);
			method.invoke(this, wallet, walletProto);
		} catch (Exception e) {
			log.info("read wallet error loadExtensions");
			e.printStackTrace();
		}
		if (walletProto.hasVersion()) {
			wallet.setVersion(walletProto.getVersion());
		}

		// Make sure the object can be re-used to read another wallet without
		// corruption.
		txMap.clear();
	}

	private WalletTransaction connectTransactionOutputs(
			org.bitcoinj.wallet.Protos.Transaction txProto)
			throws UnreadableWalletException {
		Transaction tx = txMap.get(txProto.getHash());
		final WalletTransaction.Pool pool;
		switch (txProto.getPool()) {
		case DEAD:
			pool = WalletTransaction.Pool.DEAD;
			break;
		case PENDING:
			pool = WalletTransaction.Pool.PENDING;
			break;
		case SPENT:
			pool = WalletTransaction.Pool.SPENT;
			break;
		case UNSPENT:
			pool = WalletTransaction.Pool.UNSPENT;
			break;
		// Upgrade old wallets: inactive pool has been merged with the pending
		// pool.
		// Remove this some time after 0.9 is old and everyone has upgraded.
		// There should not be any spent outputs in this tx as old wallets would
		// not allow them to be spent
		// in this state.
		case INACTIVE:
		case PENDING_INACTIVE:
			pool = WalletTransaction.Pool.PENDING;
			break;
		default:
			throw new UnreadableWalletException("Unknown transaction pool: "
					+ txProto.getPool());
		}
		for (int i = 0; i < tx.getOutputs().size(); i++) {
			TransactionOutput output = tx.getOutputs().get(i);
			final Protos.TransactionOutput transactionOutput = txProto
					.getTransactionOutput(i);
			if (transactionOutput.hasSpentByTransactionHash()) {
				final ByteString spentByTransactionHash = transactionOutput
						.getSpentByTransactionHash();
				Transaction spendingTx = txMap.get(spentByTransactionHash);
				// remove by jjz, bitcoinj null wrong
				// if (spendingTx == null) {
				// throw new
				// UnreadableWalletException(String.format("Could not connect %s to %s",
				// tx.getHashAsString(),
				// byteStringToHash(spentByTransactionHash)));
				// }
				if (spendingTx != null) {
					final int spendingIndex = transactionOutput
							.getSpentByTransactionIndex();
					TransactionInput input = checkNotNull(spendingTx
							.getInput(spendingIndex));
					input.connect(output);
				}
			}
		}

		if (txProto.hasConfidence()) {
			Protos.TransactionConfidence confidenceProto = txProto
					.getConfidence();
			TransactionConfidence confidence = tx.getConfidence();
			readConfidence(tx, confidenceProto, confidence);
		}

		return new WalletTransaction(pool, tx);
	}

	private void readConfidence(Transaction tx,
			Protos.TransactionConfidence confidenceProto,
			TransactionConfidence confidence) throws UnreadableWalletException {
		// We are lenient here because tx confidence is not an essential part of
		// the wallet.
		// If the tx has an unknown type of confidence, ignore.
		if (!confidenceProto.hasType()) {
			log.warn("Unknown confidence type for tx {}", tx.getHashAsString());
			return;
		}
		ConfidenceType confidenceType;
		switch (confidenceProto.getType()) {
		case BUILDING:
			confidenceType = ConfidenceType.BUILDING;
			break;
		case DEAD:
			confidenceType = ConfidenceType.DEAD;
			break;
		// These two are equivalent (must be able to read old wallets).
		case NOT_IN_BEST_CHAIN:
			confidenceType = ConfidenceType.PENDING;
			break;
		case PENDING:
			confidenceType = ConfidenceType.PENDING;
			break;
		case UNKNOWN:
			// Fall through.
		default:
			confidenceType = ConfidenceType.UNKNOWN;
			break;
		}
		confidence.setConfidenceType(confidenceType);
		if (confidenceProto.hasAppearedAtHeight()) {
			if (confidence.getConfidenceType() != ConfidenceType.BUILDING) {
				log.warn("Have appearedAtHeight but not BUILDING for tx {}",
						tx.getHashAsString());
				return;
			}
			confidence.setAppearedAtChainHeight(confidenceProto
					.getAppearedAtHeight());
		}
		if (confidenceProto.hasDepth()) {
			if (confidence.getConfidenceType() != ConfidenceType.BUILDING) {
				log.warn("Have depth but not BUILDING for tx {}",
						tx.getHashAsString());
				return;
			}
			confidence.setDepthInBlocks(confidenceProto.getDepth());
		}
		if (confidenceProto.hasWorkDone()) {
			if (confidence.getConfidenceType() != ConfidenceType.BUILDING) {
				log.warn("Have workDone but not BUILDING for tx {}",
						tx.getHashAsString());
				return;
			}
			confidence.setWorkDone(BigInteger.valueOf(confidenceProto
					.getWorkDone()));
		}
		if (confidenceProto.hasOverridingTransaction()) {
			if (confidence.getConfidenceType() != ConfidenceType.DEAD) {
				log.warn(
						"Have overridingTransaction but not OVERRIDDEN for tx {}",
						tx.getHashAsString());
				return;
			}
			Transaction overridingTransaction = txMap.get(confidenceProto
					.getOverridingTransaction());
			if (overridingTransaction == null) {
				log.warn(
						"Have overridingTransaction that is not in wallet for tx {}",
						tx.getHashAsString());
				return;
			}
			confidence.setOverridingTransaction(overridingTransaction);
		}
		for (Protos.PeerAddress proto : confidenceProto.getBroadcastByList()) {
			InetAddress ip;
			try {
				ip = InetAddress.getByAddress(proto.getIpAddress()
						.toByteArray());
			} catch (UnknownHostException e) {
				throw new UnreadableWalletException(
						"Peer IP address does not have the right length", e);
			}
			int port = proto.getPort();
			PeerAddress address = new PeerAddress(ip, port);
			address.setServices(BigInteger.valueOf(proto.getServices()));
			confidence.markBroadcastBy(address);
		}
		switch (confidenceProto.getSource()) {
		case SOURCE_SELF:
			confidence.setSource(TransactionConfidence.Source.SELF);
			break;
		case SOURCE_NETWORK:
			confidence.setSource(TransactionConfidence.Source.NETWORK);
			break;
		case SOURCE_UNKNOWN:
			// Fall through.
		default:
			confidence.setSource(TransactionConfidence.Source.UNKNOWN);
			break;
		}
	}

	private void readTransaction(Protos.Transaction txProto,
			NetworkParameters params) throws UnreadableWalletException {
		Transaction tx = new Transaction(params);
		if (txProto.hasUpdatedAt()) {
			tx.setUpdateTime(new Date(txProto.getUpdatedAt()));
		}

		for (Protos.TransactionOutput outputProto : txProto
				.getTransactionOutputList()) {
			BigInteger value = BigInteger.valueOf(outputProto.getValue());
			byte[] scriptBytes = outputProto.getScriptBytes().toByteArray();
			TransactionOutput output = new TransactionOutput(params, tx, value,
					scriptBytes);
			tx.addOutput(output);
		}

		for (Protos.TransactionInput transactionInput : txProto
				.getTransactionInputList()) {
			byte[] scriptBytes = transactionInput.getScriptBytes()
					.toByteArray();
			TransactionOutPoint outpoint = new TransactionOutPoint(
					params,
					transactionInput.getTransactionOutPointIndex() & 0xFFFFFFFFL,
					byteStringToHash(transactionInput
							.getTransactionOutPointHash()));
			TransactionInput input = new TransactionInput(params, tx,
					scriptBytes, outpoint);
			if (transactionInput.hasSequence()) {
				input.setSequenceNumber(transactionInput.getSequence());
			}
			tx.addInput(input);
		}

		for (int i = 0; i < txProto.getBlockHashCount(); i++) {
			ByteString blockHash = txProto.getBlockHash(i);
			int relativityOffset = 0;
			if (txProto.getBlockRelativityOffsetsCount() > 0)
				relativityOffset = txProto.getBlockRelativityOffsets(i);
			tx.addBlockAppearance(byteStringToHash(blockHash), relativityOffset);
		}

		if (txProto.hasLockTime()) {
			tx.setLockTime(0xffffffffL & txProto.getLockTime());
		}

		if (txProto.hasPurpose()) {
			switch (txProto.getPurpose()) {
			case UNKNOWN:
				tx.setPurpose(Transaction.Purpose.UNKNOWN);
				break;
			case USER_PAYMENT:
				tx.setPurpose(Transaction.Purpose.USER_PAYMENT);
				break;
			case KEY_ROTATION:
				tx.setPurpose(Transaction.Purpose.KEY_ROTATION);
				break;
			default:
				throw new RuntimeException(
						"New purpose serialization not implemented");
			}
		} else {
			// Old wallet: assume a user payment as that's the only reason a new
			// tx would have been created back then.
			tx.setPurpose(Transaction.Purpose.USER_PAYMENT);
		}
		// tx of api is not complete
		// Transaction should now be complete.
		// Sha256Hash protoHash = byteStringToHash(txProto.getHash());
		// if (!tx.getHash().equals(protoHash))
		// throw new UnreadableWalletException(String.format(
		// "Transaction did not deserialize completely: %s vs %s",
		// tx.getHash(), protoHash));
		Sha256Hash protoHash = byteStringToHash(txProto.getHash());
		if (txMap.containsKey(txProto.getHash()))
			throw new UnreadableWalletException(
					"Wallet contained duplicate transaction "
							+ byteStringToHash(txProto.getHash()));
		// add by jjz
		Field txField;
		try {
			txField = Transaction.class.getDeclaredField("hash");
			txField.setAccessible(true);
			txField.set(tx, protoHash);
		} catch (Exception e) {
			log.info("read wallet error hash" + protoHash.toString());
		}

		txMap.put(txProto.getHash(), tx);
	}

}
