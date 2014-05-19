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

import net.bither.BitherSetting.MarketType;
import net.bither.api.GetExchangeDepthApi;
import net.bither.model.Depth;
import net.bither.util.DepthUtil;

public class GetExchangeDepthRunnable extends BaseRunnable {

	private MarketType marketType;

	public GetExchangeDepthRunnable(MarketType marketType) {
		this.marketType = marketType;

	}

	@Override
	public void run() {
		boolean hasCache = false;
		obtainMessage(HandlerMessage.MSG_PREPARE);
		try {
			Depth depth = DepthUtil.getKDepth(this.marketType);
			hasCache = depth != null;
			obtainMessage(HandlerMessage.MSG_SUCCESS_FROM_CACHE, depth);
			GetExchangeDepthApi getExchangeDepthApi = new GetExchangeDepthApi(
					marketType);
			getExchangeDepthApi.handleHttpGet();
			depth = getExchangeDepthApi.getResult();
			depth.setMarketType(this.marketType);
			DepthUtil.addDepth(depth);
			obtainMessage(HandlerMessage.MSG_SUCCESS, depth);
		} catch (Exception e) {
			if (!hasCache) {
				obtainMessage(HandlerMessage.MSG_FAILURE);
			}
			e.printStackTrace();
		}

	}

}
