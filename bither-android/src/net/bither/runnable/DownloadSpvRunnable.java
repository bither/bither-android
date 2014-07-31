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

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import net.bither.BitherSetting;
import net.bither.api.DownloadSpvApi;
import net.bither.service.BlockchainService;
import net.bither.util.LogUtil;

import com.google.bitcoin.core.StoredBlock;

public class DownloadSpvRunnable extends BaseRunnable {

	private BlockchainService mBlockchainService;

	public DownloadSpvRunnable(BlockchainService blockchainService) {
		this.mBlockchainService = blockchainService;
	}

	@Override
	public void run() {
		obtainMessage(HandlerMessage.MSG_PREPARE);
		try {
			DownloadSpvApi downloadSpvApi = new DownloadSpvApi();
			downloadSpvApi.handleHttpGet();
			StoredBlock storedBlock = downloadSpvApi.getResult();
            List<StoredBlock> blocks=new ArrayList<StoredBlock>();
            blocks.add(storedBlock);
            if (storedBlock.getHeight()% BitherSetting.NETWORK_PARAMETERS.getInterval()==0) {
                this.mBlockchainService.initSpvFile(blocks);
                this.mBlockchainService.setDownloadSpvFinish(true);
                obtainMessage(HandlerMessage.MSG_SUCCESS);
            }else{
                Log.e("spv", "service is not vaild");
                obtainMessage(HandlerMessage.MSG_FAILURE);
            }
		} catch (Exception e) {
			obtainMessage(HandlerMessage.MSG_FAILURE);
			e.printStackTrace();
		}

	}

}
