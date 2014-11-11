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

import net.bither.BitherSetting.MarketType;
import net.bither.bitherj.utils.Utils;
import net.bither.charts.entity.DateValueEntity;
import net.bither.util.ExchangeUtil;
import net.bither.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Depth implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String BIDS = "bids";
	private static final String ASKS = "asks";

	private MarketType marketType;
	private List<DateValueEntity> dateValueEntities;

	private double maxVolume;
	private int splitIndex;

	public List<DateValueEntity> getDateValueEntities() {
		return dateValueEntities;
	}

	public void setDateValueEntities(List<DateValueEntity> dateValueEntities) {
		this.dateValueEntities = dateValueEntities;
	}

	public double getMaxVolume() {
		return maxVolume;
	}

	public void setMaxVolume(double maxVolume) {
		this.maxVolume = maxVolume;
	}

	public static Depth formatJsonOfMarketDepth(MarketType marketType,
			JSONObject json) throws JSONException {
		Depth depth = new Depth();

		double rate = ExchangeUtil.getRate(marketType);
		int bidMaxPrice = 0;
		int askMinPrice = Integer.MAX_VALUE;

		List<DateValueEntity> bidDateValueEntities = new ArrayList<DateValueEntity>();
		List<DateValueEntity> askDateValueEntities = new ArrayList<DateValueEntity>();
		double bidSumVolume = 0;
		int splitIndex = 0;
		if (!json.isNull(BIDS)) {
			JSONArray bidArray = json.getJSONArray(BIDS);

			for (int i = bidArray.length() - 1; i >= 0; i--) {
				JSONArray bid = bidArray.getJSONArray(i);
				int bidPrice = bid.getInt(0);
				double price = ((double) bidPrice) / 100 * rate;
				double volume = bid.getDouble(1) / Math.pow(10, 8);
				if (bidMaxPrice < bidPrice) {
					bidMaxPrice = bidPrice;
				}
				bidSumVolume = bidSumVolume + volume;
				DateValueEntity dateValueEntity = new DateValueEntity(
						(float) bidSumVolume,
                        Utils.formatDoubleToMoneyString(price), bidPrice);
				bidDateValueEntities.add(dateValueEntity);

			}
			splitIndex = bidArray.length();

		}
		double askSumVolume = 0;
		if (!json.isNull(ASKS)) {
			JSONArray askArray = json.getJSONArray(ASKS);

			for (int i = 0; i < askArray.length(); i++) {
				JSONArray ask = askArray.getJSONArray(i);
				int askPrice = ask.getInt(0);
				double price = ((double) askPrice) / 100 * rate;
				double volume = ask.getDouble(1) / Math.pow(10, 8);
				askSumVolume = askSumVolume + volume;
				if (askPrice < askMinPrice) {
					askMinPrice = askPrice;
				}
				DateValueEntity dateValueEntity = new DateValueEntity(
						(float) askSumVolume,
                        Utils.formatDoubleToMoneyString(price), askPrice);
				askDateValueEntities.add(dateValueEntity);
			}

		}
		int mixPrice = (askMinPrice + bidMaxPrice) / 2;
		DateValueEntity zeroDateValue = new DateValueEntity(0,
                Utils.formatDoubleToMoneyString(((double) mixPrice) / 100
						* rate), mixPrice);
		List<DateValueEntity> dateValueEntities = new ArrayList<DateValueEntity>();
		dateValueEntities.addAll(bidDateValueEntities);
		dateValueEntities.add(zeroDateValue);
		dateValueEntities.addAll(askDateValueEntities);
		Collections.sort(dateValueEntities, new ComparatorDateValue());
		depth.setMaxVolume(Math.max(askSumVolume, bidSumVolume));
		depth.setDateValueEntities(dateValueEntities);
		depth.setSplitIndex(splitIndex);
		return depth;

	}

	public MarketType getMarketType() {
		return marketType;
	}

	public void setMarketType(MarketType marketType) {
		this.marketType = marketType;
	}

	public int getSplitIndex() {
		return splitIndex;
	}

	public void setSplitIndex(int splitIndex) {
		this.splitIndex = splitIndex;
	}

	private static class ComparatorDateValue implements
			Comparator<DateValueEntity> {

		@Override
		public int compare(DateValueEntity lhs, DateValueEntity rhs) {

			if (lhs.getDate() > rhs.getDate()) {
				return 1;
			} else if (lhs.getDate() < rhs.getDate()) {
				return -1;
			}
			return 0;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Depth) {
			Depth depth = (Depth) o;
			return depth.getMarketType() == getMarketType();
		}
		return super.equals(o);
	}

}
