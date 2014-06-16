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

import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.SPVBlockStore;

import net.bither.BitherSetting;
import net.bither.model.BitherAddress;
import net.bither.service.BlockchainService;
import net.bither.util.WalletUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class RebuildRunnable extends BaseRunnable {
    private BlockchainService mBlockchainService;
    private File mBlockFile;
    private static final Logger log = LoggerFactory.getLogger(RebuildRunnable.class);

    public RebuildRunnable(BlockchainService blockchainService, File blockFile) {
        this.mBlockchainService = blockchainService;
        this.mBlockFile = blockFile;
    }

    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);

        try {
            int blockStoreHight = mBlockchainService.getBlockStore().getHeight();
            List<BitherAddress> bitherAddresses = WalletUtils.getBitherAddressList(true);
            int minLastSeenHeight = Integer.MAX_VALUE;
            for (BitherAddress bitherAddress : bitherAddresses) {
                int lastSeenHeight = bitherAddress.getLastBlockSeenHeight();
                if (blockStoreHight - lastSeenHeight < BitherSetting
                        .MAX_DISTANCE_HIGH_OF_API_STORE) {
                    if (minLastSeenHeight > lastSeenHeight) {
                        minLastSeenHeight = bitherAddress.getLastBlockSeenHeight();
                        log.info("address:" + bitherAddress.getAddress() + "," +
                                "minLastHeight:" + minLastSeenHeight);
                    }
                }
            }

            BlockStore blockStore = new SPVBlockStore(BitherSetting.NETWORK_PARAMETERS,
                    this.mBlockFile);
            StoredBlock storedBlock = blockStore.getChainHead();

            while (storedBlock.getHeight() > minLastSeenHeight) {
                storedBlock = blockStore.get(storedBlock.getHeader().getPrevBlockHash());
            }
            // TODO need to remodel spv
            blockStore.setChainHead(storedBlock);
            obtainMessage(HandlerMessage.MSG_SUCCESS);
        } catch (Exception e) {
            obtainMessage(HandlerMessage.MSG_FAILURE);
            e.printStackTrace();
        }

    }
}
