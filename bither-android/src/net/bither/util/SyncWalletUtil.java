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

import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.store.BlockStoreException;

import net.bither.BitherSetting;
import net.bither.exception.NoAddressException;
import net.bither.model.BitherAddress;
import net.bither.runnable.AbstractMultiThread.MultiThreadListener;
import net.bither.runnable.BaseRunnable;
import net.bither.runnable.DownloadSpvRunnable;
import net.bither.runnable.GetMyTxFromBitherRunnable;
import net.bither.runnable.RebuildRunnable;
import net.bither.runnable.SyncBlockAndWalletMutiThread;
import net.bither.service.BlockchainService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class SyncWalletUtil {

	public static boolean needGetTxFromApi(BlockchainService blockchainService)
			throws NoAddressException {
		boolean needSyncWallet = false;
		List<BitherAddress> bitherAddressList = WalletUtils
				.getBitherAddressList(true);
		if (bitherAddressList == null || bitherAddressList.size() == 0) {
			throw new NoAddressException("no address or address is not init");
		}
		for (BitherAddress bitherAddress : bitherAddressList) {
			if (bitherAddress.getLastBlockSeenHeight() <= 0) {
				needSyncWallet = true;
			}
		}
		return needSyncWallet;

	}

	public static boolean needRebuildBlock(BlockchainService blockchainService)
			throws NoAddressException, BlockStoreException {
		StoredBlock storedBlock = blockchainService.getBlockStore();
		int blockHeight = storedBlock.getHeight();
		boolean needRebuildBlock = false;
		List<BitherAddress> bitherAddressList = WalletUtils
				.getBitherAddressList(true);
		if (bitherAddressList == null || bitherAddressList.size() == 0) {
			throw new NoAddressException("no address or address is not init");
		}
		for (BitherAddress bitherAddress : bitherAddressList) {
			int lastBlockSeenHeight = bitherAddress.getLastBlockSeenHeight();
			if (blockHeight - lastBlockSeenHeight < BitherSetting.MAX_DISTANCE_HIGH_OF_API_STORE) {
				if (lastBlockSeenHeight < blockHeight) {
					needRebuildBlock = true;
				}
			}
		}
		return needRebuildBlock;

	}

	public static boolean noConnectPeer(BlockchainService blockchainService)
			throws NoAddressException, BlockStoreException {
		return !needGetTxFromApi(blockchainService)
				&& !needRebuildBlock(blockchainService);
	}

	public static void reBuildBlock(BlockchainService blockchainService,
			File blockFile) {
		if (!SyncBlockAndWalletMutiThread.getInstance().isRunning()) {
			List<BaseRunnable> baseRunnables = new ArrayList<BaseRunnable>();
			RebuildRunnable rebuildRunnable = new RebuildRunnable(
					blockchainService, blockFile);
			baseRunnables.add(rebuildRunnable);
			SyncBlockAndWalletMutiThread.getInstance().run(baseRunnables,
					new RebuildBlockListener(blockchainService));

		}

	}

	public static void dowloadSpvStoredBlock(
			@Nullable final BlockchainService blockchainService) {
		if (!SyncBlockAndWalletMutiThread.getInstance().isRunning()) {
			List<BaseRunnable> baseRunnables = new ArrayList<BaseRunnable>();
			DownloadSpvRunnable getBlockHeaderRunnable = new DownloadSpvRunnable(
					blockchainService);
			baseRunnables.add(getBlockHeaderRunnable);
			SyncBlockAndWalletMutiThread.getInstance().run(baseRunnables,
					new GetSpvBlockListener());
		}

	}

	public static void syncWallet(BlockchainService blockchainService)
			throws NoAddressException, BlockStoreException {
		if (needGetTxFromApi(blockchainService)) {
			getTxFromApiWallet(blockchainService);
		}
	}

	private static void getTxFromApiWallet(BlockchainService blockchainService)
			throws BlockStoreException {
		if (WalletUtils.getBitherAddressList(true) != null) {
			List<BitherAddress> listAddress = new ArrayList<BitherAddress>();
			for (BitherAddress bitherAddress : WalletUtils
					.getBitherAddressList(true)) {
				if (bitherAddress.getLastBlockSeenHeight() <= 0) {
					listAddress.add(bitherAddress);
				}
			}
			if (listAddress != null && listAddress.size() > 0) {
				getTransactionOfBitherAddress(blockchainService, listAddress);
			}
		}
	}

	private static void getTransactionOfBitherAddress(
			final BlockchainService blockchainService,
			final List<BitherAddress> list) {
		BroadcastUtil.sendBroadcastProgressState(0.1);
		if (!SyncBlockAndWalletMutiThread.getInstance().isRunning()) {
			List<BaseRunnable> runnables = new ArrayList<BaseRunnable>();
			GetMyTxFromBitherRunnable getTransactionsRunnable = new GetMyTxFromBitherRunnable(
					blockchainService, list);
			runnables.add(getTransactionsRunnable);
			SyncBlockAndWalletMutiThread.getInstance().run(runnables,
					new GetTransactionListener(blockchainService));
		}
	}

	private static class GetTransactionListener implements MultiThreadListener {
		private BlockchainService mBlockchainService;

		public GetTransactionListener(BlockchainService blockchainService) {
			mBlockchainService = blockchainService;
		}

		@Override
		public void success(final Object object) {
			try {
				if (!needRebuildBlock(mBlockchainService)) {
					mBlockchainService.initSyncBlockChain();
				}
			} catch (NoAddressException e) {
				e.printStackTrace();
			} catch (BlockStoreException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void prepare() {
		}

		@Override
		public void error(int errorCode, Object obj) {
			BroadcastUtil
					.sendBroadcastProgressState(BitherSetting.SYNC_PROGRESS_COMPLETE);
		}

		@Override
		public void nextStep() {
		}

	}

	private static class RebuildBlockListener implements MultiThreadListener {
		private BlockchainService mBlockchainService;

		public RebuildBlockListener(BlockchainService blockchainService) {
			mBlockchainService = blockchainService;
		}

		@Override
		public void prepare() {
		}

		@Override
		public void success(Object object) {
			try {
				if (!needGetTxFromApi(mBlockchainService)) {
					mBlockchainService.initSyncBlockChain();
				}
			} catch (NoAddressException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void error(int errorCode, Object obj) {

		}

		@Override
		public void nextStep() {
		}

	}

	private static class GetSpvBlockListener implements MultiThreadListener {

		@Override
		public void success(Object object) {
			BroadcastUtil.sendBroadcastGetSpvBlockComplete(true);
		}

		@Override
		public void prepare() {
		}

		@Override
		public void error(int errorCode, Object obj) {
			BroadcastUtil.sendBroadcastGetSpvBlockComplete(false);
		}

		@Override
		public void nextStep() {

		}

	}

}
