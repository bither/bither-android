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

package net.bither.api;

import java.util.List;

import net.bither.BitherSetting.KlineTimeType;
import net.bither.BitherSetting.MarketType;
import net.bither.charts.entity.IStickEntity;
import net.bither.http.BitherUrl;
import net.bither.http.HttpGetResponse;
import net.bither.util.ChartsUtil;
import net.bither.util.StringUtil;

import org.json.JSONArray;

public class GetKlineApi extends HttpGetResponse<List<IStickEntity>> {

	private KlineTimeType klineTimeType;
	private MarketType marketType;

	public GetKlineApi(MarketType marketType, KlineTimeType klineTimeType) {

		this.klineTimeType = klineTimeType;
		this.marketType = marketType;
		String url = StringUtil.format(BitherUrl.BITHER_KLINE_URL,
				marketType.getValue(), klineTimeType.getValue());
		setUrl(url);

	}

	@Override
	public void setResult(String response) throws Exception {
		JSONArray jsonArray = new JSONArray(response);
		this.result = ChartsUtil.formatJsonArray(this.marketType,
				this.klineTimeType, jsonArray);

	}

}
