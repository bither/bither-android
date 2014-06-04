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

import android.content.Intent;

import com.google.bitcoin.core.Transaction;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.model.AddressInfo;
import net.bither.model.BitherAddress;
import net.bither.model.Ticker;
import net.bither.service.BlockchainService;

import java.math.BigInteger;
import java.util.List;

public class BroadcastUtil {
	public static final String ACTION_START_DOWLOAD_BLOCK_STATE = R.class
			.getPackage().getName() + ".start_dowload_block_state";

	public static final String ACTION_TOTAL_BITCOIN_STATE = R.class
			.getPackage().getName() + ".total_state";
	public static final String ACTION_TOTAL_BITCOIN = "total_bitcoin";

	public static final String ACTION_ADDRESS_STATE = R.class.getPackage()
			.getName() + ".address";

	public static final String ACTION_ADDRESS_INFO = "address_info";
	public static final String ACTION_ADDRESS_ERROR = "address_error";

	public static final String ACTION_MARKET = R.class.getPackage().getName()
			+ ".market";

	public static final String ACTION_TICKER_INFO = "ticker_info";

	public static final String ACTION_SYNC_BLOCK_AND_WALLET_STATE = R.class
			.getPackage().getName() + ".sync_block_wallet";
	public static final String ACTION_PROGRESS_INFO = "progress_info";

	public static final String ACTION_ADDRESS_LOAD_COMPLETE_STATE = R.class
			.getPackage().getName() + ".load_complete";

	public static final String ACTION_DOWLOAD_SPV_BLOCK = R.class.getPackage()
			.getName() + ".dowload_block_api_complete";
	public static final String ACTION_DOWLOAD_SPV_BLOCK_STATE = "complete";

	public static final String ACTION_ADDRESS_LOAD_COMPLETE_INFO = "load_complete";

	// public static final String ACTION_SYNC_BEGIN = "sync_begin_info";
	// public static final String ACTION_SYNC_END = "sync_end_info";
	public static void sendBroadcastDowloadBlockState() {
		final Intent broadcast = new Intent(ACTION_START_DOWLOAD_BLOCK_STATE);
		BitherApplication.mContext.sendBroadcast(broadcast);
	}

	public static void sendBroadcastTotalBitcoinState(BigInteger bigInteger) {

		final Intent broadcast = new Intent(ACTION_TOTAL_BITCOIN_STATE);
		broadcast.putExtra(ACTION_TOTAL_BITCOIN, bigInteger);
		BitherApplication.mContext.sendStickyBroadcast(broadcast);
		LogUtil.d("total",
				"sendBroadcastTotalBitcoinState" + bigInteger.toString());
	}

	public static void removeBroadcastTotalBitcoinState() {
		BitherApplication.mContext.removeStickyBroadcast(new Intent(
				ACTION_TOTAL_BITCOIN_STATE));
	}

	public static void sendBroadcastAddressState(BitherAddress bitherAddress) {
		final Intent broadcast = new Intent(ACTION_ADDRESS_STATE);
		AddressInfo addressInfo = new AddressInfo(bitherAddress);
		bitherAddress.setAddressInfo(addressInfo);
		broadcast.putExtra(ACTION_ADDRESS_INFO, addressInfo);
		broadcast.putExtra(ACTION_ADDRESS_STATE, bitherAddress.getAddress());
		BitherApplication.mContext.sendBroadcast(broadcast);
	}

	public static void sendBroadcastAddressState(BitherAddress bitherAddress,
			int errorCode, Object obj) {
		final Intent broadcast = new Intent(ACTION_ADDRESS_STATE);
		broadcast.putExtra(ACTION_ADDRESS_ERROR, errorCode);
		broadcast.putExtra(ACTION_ADDRESS_STATE, bitherAddress.getAddress());
		BitherApplication.mContext.sendBroadcast(broadcast);
	}

	public static void removeAddressBitcoinState() {
		BitherApplication.mContext.removeStickyBroadcast(new Intent(
				ACTION_ADDRESS_STATE));
	}

	public static void sendBroadcastMarketState(List<Ticker> tickers) {
		if (tickers != null && tickers.size() > 0) {
			MarketUtil.setTickerList(tickers);
			final Intent broadcast = new Intent(ACTION_MARKET);
			BitherApplication.mContext.sendBroadcast(broadcast);
		}
	}

	public static void removeMarketState() {
		BitherApplication.mContext.removeStickyBroadcast(new Intent(
				ACTION_MARKET));
	}

	public static void sendBroadcastProgressState(double value) {
		final Intent broadcast = new Intent(ACTION_SYNC_BLOCK_AND_WALLET_STATE);
		broadcast.putExtra(ACTION_PROGRESS_INFO, value);
		BitherApplication.mContext.sendBroadcast(broadcast);
	}

	public static void sendBroadcastTx(Transaction tx, int addressPosition,
			boolean hasPrivateKey) {
		final Intent intent = new Intent(
				BlockchainService.ACTION_BROADCAST_TRANSACTION, null,
				BitherApplication.mContext, BlockchainService.class);
		intent.putExtra(BlockchainService.ACTION_BROADCAST_TRANSACTION_HASH, tx
				.getHash().getBytes());
		intent.putExtra(
				BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG,
				addressPosition);
		intent.putExtra(
				BitherSetting.INTENT_REF.ADDRESS_HAS_PRIVATE_KEY_PASS_VALUE_TAG,
				hasPrivateKey);
		BitherApplication.mContext.startService(intent);
	}

	public static void removeProgressState() {
		BitherApplication.mContext.removeStickyBroadcast(new Intent(
				ACTION_SYNC_BLOCK_AND_WALLET_STATE));

	}

	public static void sendBroadcastAddressLoadCompleteState(boolean value) {
		final Intent broadcast = new Intent(ACTION_ADDRESS_LOAD_COMPLETE_STATE);
		broadcast.putExtra(ACTION_ADDRESS_LOAD_COMPLETE_INFO, value);
		BitherApplication.mContext.sendStickyBroadcast(broadcast);
	}

	public static void sendBroadcastGetSpvBlockComplete(boolean isComplete) {
		final Intent intent = new Intent(ACTION_DOWLOAD_SPV_BLOCK);
		intent.putExtra(ACTION_DOWLOAD_SPV_BLOCK_STATE, isComplete);
		BitherApplication.mContext.sendStickyBroadcast(intent);
		LogUtil.d("broadcase", "sendBroadcastAddressGetBlockComplete");
	}

	public static void removeBroadcastGetSpvBlockCompelte() {
		BitherApplication.mContext.removeStickyBroadcast(new Intent(
				ACTION_DOWLOAD_SPV_BLOCK));
	}

	public static void removeAddressLoadCompleteState(BitherApplication app) {
		app.removeStickyBroadcast(new Intent(ACTION_ADDRESS_LOAD_COMPLETE_STATE));
	}
}
