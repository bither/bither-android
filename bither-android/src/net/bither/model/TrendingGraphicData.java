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

import static com.google.common.base.Preconditions.checkState;
import net.bither.util.ExchangeUtil;
import net.bither.util.TrendingGraphicUtil;

import org.json.JSONArray;
import org.json.JSONException;

public class TrendingGraphicData {

	private static long EXPORED_TIME = 1 * 60 * 60 * 1000;

	private static TrendingGraphicData EmptyData;

	private double high;
	private double low;
	private double[] prices;

	private double[] rates;

	private long createTime = -1;

	public TrendingGraphicData(double high, double low, double[] prices) {
		checkState(prices.length == TrendingGraphicUtil.TRENDING_GRAPIC_COUNT);
		this.high = high;
		this.low = low;
		this.prices = prices;
		this.createTime = System.currentTimeMillis();

		caculateRate();
	}

	public double getHigh() {
		return high;
	}

	public double getLow() {
		return low;
	}

	public double[] getPrices() {
		return prices;
	}

	public double[] getRates() {
		return rates;
	}

	private void caculateRate() {
		int count = prices.length;
		rates = new double[count];
		double interval = high - low;
		for (int i = 0; i < count; i++) {
			rates[i] = Math.max(0, prices[i] - low) / interval;
		}
	}

	public boolean isExpired() {
		return this.createTime == -1
				|| this.createTime + EXPORED_TIME < System.currentTimeMillis();
	}

	public static TrendingGraphicData format(JSONArray jsonArray)
			throws JSONException {
		double high = 0;
		double low = Double.MAX_VALUE;
		double rate = ExchangeUtil.getExchangeRate();
		double[] prices = new double[jsonArray.length()];
		for (int i = 0; i < jsonArray.length(); i++) {
			double price = jsonArray.getDouble(i) / 100 * rate;
			if (price == 0) {
				if (i == 0) {
					int j = i;
					while (price == 0 && j < jsonArray.length()) {
						price = jsonArray.getDouble(j) / 100 * rate;
						j++;
					}

				} else {
					int j = i - 1;
					price = jsonArray.getDouble(i - 1) / 100 * rate;
					while (price == 0 && j > 0) {
						price = jsonArray.getDouble(j) / 100 * rate;
						j--;
					}
				}
			}
			prices[i] = price;
			if (high < price) {
				high = price;
			}
			if (low > price) {
				low = price;
			}
		}
		TrendingGraphicData trendingGraphicData = new TrendingGraphicData(high,
				low, prices);
		return trendingGraphicData;

	}

	public static TrendingGraphicData getEmptyData() {
		if (EmptyData == null) {
			double[] prices = new double[TrendingGraphicUtil.TRENDING_GRAPIC_COUNT];
			for (int i = 0; i < prices.length; i++) {
				prices[i] = 0.5;
			}
			EmptyData = new TrendingGraphicData(1, 0, prices);
		}
		return EmptyData;
	}

}
