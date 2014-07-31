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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.bither.BitherSetting.MarketType;
import net.bither.util.ExchangeUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class Ticker implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// key of
	private final static String VOLUME = "volume";
	private final static String LAST = "last";
	private final static String HIGH = "high";
	private final static String LOW = "low";
	private final static String ASK = "ask";
	private final static String BID = "bid";

	/***
	 * total of btc
	 */
	private double mAmount;
	private double mLevel;
	private double pHigh;
	private double pLow;
	private double pNew;
	private double mAmp;
	private double pOpen;

	private double pSell;
	private double pBuy;
	/***
	 * total of money
	 */
	private double mTotal;
	private Date mDate;
	private MarketType marketType;

	public double getAmount() {
		return mAmount;
	}

	public void setAmount(double mAmount) {
		this.mAmount = mAmount;
	}

	public double getLevel() {
		return mLevel;
	}

	public void setLevel(double mLevel) {
		this.mLevel = mLevel;
	}

	public double getHigh() {
		return pHigh;
	}

	public double getDefaultExchangeHigh() {
		return pHigh * ExchangeUtil.getRate(getMarketType());
	}

	public void setHigh(double pHigh) {
		this.pHigh = pHigh;
	}

	public double getLow() {
		return pLow;
	}

	public double getDefaultExchangeLow() {
		return pLow * ExchangeUtil.getRate(getMarketType());
	}

	public void setLow(double pLow) {
		this.pLow = pLow;
	}

	public double getDefaultExchangePrice() {
		return pNew * ExchangeUtil.getRate(getMarketType());

	}

	public double getPrice() {
		return pNew;
	}

	public void setNew(double pNew) {
		this.pNew = pNew;
	}

	public double getTotal() {
		return mTotal;
	}

	public void setTotal(double mTotal) {
		this.mTotal = mTotal;
	}

	public double getAmp() {
		return mAmp;
	}

	public void setAmp(double mAmp) {
		this.mAmp = mAmp;
	}

	public double getOpen() {
		return pOpen;
	}

	public void setOpen(double pOpen) {

		this.pOpen = pOpen;
	}


	public Date getDate() {
		return mDate;
	}

	public void setDate(Date mDate) {
		this.mDate = mDate;
	}

	public MarketType getMarketType() {
		return marketType;
	}

	public void setMarketType(MarketType marketType) {
		this.marketType = marketType;
	}

	public double getSell() {
		return pSell;
	}

	public double getDefaultExchangeSell() {
		return pSell * ExchangeUtil.getRate(getMarketType());
	}

	public void setSell(double pSell) {
		this.pSell = pSell;
	}

	public double getBuy() {
		return pBuy;
	}

	public double getDefaultExchangeBuy() {
		return pBuy * ExchangeUtil.getRate(getMarketType());
	}

	public void setBuy(double pBuy) {
		this.pBuy = pBuy;
	}

	private static Ticker formatTicker(JSONObject json, MarketType marketType)
			throws JSONException {
		Ticker ticker = new Ticker();
		if (!json.isNull(VOLUME)) {
			ticker.setAmount(json.getDouble(VOLUME) / Math.pow(10, 8));
		}
		if (!json.isNull(HIGH)) {
			ticker.setHigh(json.getDouble(HIGH) / 100);
		}
		if (!json.isNull(LOW)) {
			ticker.setLow(json.getDouble(LOW) / 100);
		}
		if (!json.isNull(LAST)) {
			ticker.setNew(json.getDouble(LAST) / 100);
		}
		if (!json.isNull(BID)) {
			ticker.setBuy(json.getDouble(BID) / 100);
		}
		if (!json.isNull(ASK)) {
			ticker.setSell(json.getDouble(ASK) / 100);
		}

		ticker.setAmp(-1);
		ticker.setTotal(-1);
		ticker.setLevel(-1);
		ticker.setOpen(-1);
		ticker.setMarketType(marketType);
		return ticker;

	}

	public static List<Ticker> formatList(JSONObject json) throws JSONException {
		List<Ticker> tickers = new ArrayList<Ticker>();
		for (int i = 0; i < MarketType.values().length; i++) {
			String key = Integer.toString(i + 1);
			if (!json.isNull(key)) {
				JSONObject tickerJson = json.getJSONObject(key);
				Ticker ticker = formatTicker(tickerJson, MarketType.values()[i]);
				tickers.add(ticker);
			}
		}
		return tickers;
	}

}
