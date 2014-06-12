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

package net.bither.runnable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.bither.BitherSetting;
import net.bither.model.BitherAddress;
import net.bither.service.BlockchainService;
import net.bither.util.WalletUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.SPVBlockStore;

public class RebuildRunnable extends BaseRunnable {
	private BlockchainService mBlockchainService;
	private File mBlockFile;
	private static final Logger log = LoggerFactory
			.getLogger(RebuildRunnable.class);

	public RebuildRunnable(BlockchainService blockchainService, File blockFile) {
		this.mBlockchainService = blockchainService;
		this.mBlockFile = blockFile;
	}

	@Override
	public void run() {
		obtainMessage(HandlerMessage.MSG_PREPARE);

		try {
			int blockStoreHight = mBlockchainService.getBlockStore()
					.getHeight();
			List<BitherAddress> bitherAddresses = WalletUtils
					.getBitherAddressList(true);
			int minLastSeenHeight = Integer.MAX_VALUE;
			for (BitherAddress bitherAddress : bitherAddresses) {
				int lastSeenHeight = bitherAddress.getLastBlockSeenHeight();
				if (blockStoreHight - lastSeenHeight < BitherSetting.MAX_DISTANCE_HIGH_OF_API_STORE) {
					if (minLastSeenHeight > lastSeenHeight) {
						minLastSeenHeight = bitherAddress
								.getLastBlockSeenHeight();
						log.info("address:" + bitherAddress.getAddress()
								+ ",minLastHeight:" + minLastSeenHeight);
					}
				}
			}
			List<StoredBlock> storedBlocks = mBlockchainService
					.getRecentBlocks(blockStoreHight);
			File tempFile = new File(mBlockchainService.getDir("blockstore",
					Context.MODE_PRIVATE), "temp");
			if (tempFile.exists()) {
				tempFile.delete();
			}
			BlockStore blockStore = new SPVBlockStore(
					BitherSetting.NETWORK_PARAMETERS, tempFile);
			blockStore.getChainHead(); // detect corruptions as early as
			for (int i = storedBlocks.size() - 1; i >= 0; i--) {
				StoredBlock storedBlock = storedBlocks.get(i);
				if (storedBlock.getHeight() <= minLastSeenHeight) {
					blockStore.put(storedBlocks.get(i));
					blockStore.setChainHead(storedBlocks.get(i));
				}
			}
			if (Utils.isWindows()) {
				// Work around an issue on Windows whereby you can't rename over
				// existing files.
				File canonical = this.mBlockFile.getCanonicalFile();
				canonical.delete();
				if (tempFile.renameTo(canonical))
					return; // else fall through.
				throw new IOException("Failed to rename " + tempFile + " to "
						+ canonical);
			} else if (!tempFile.renameTo(this.mBlockFile)) {
				throw new IOException("Failed to rename " + tempFile + " to "
						+ this.mBlockFile);
			}
			obtainMessage(HandlerMessage.MSG_SUCCESS);
		} catch (Exception e) {
			obtainMessage(HandlerMessage.MSG_FAILURE);
			e.printStackTrace();
		}

	}
}
