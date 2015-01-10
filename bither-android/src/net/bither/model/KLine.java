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

import net.bither.bitherj.BitherjSettings.KlineTimeType;
import net.bither.bitherj.BitherjSettings.MarketType;
import net.bither.charts.entity.IStickEntity;

import java.io.Serializable;
import java.util.List;

public class KLine implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public KLine(MarketType marketType, KlineTimeType klineTimeType,
			List<IStickEntity> stickEntities) {
		this.stickEntities = stickEntities;
		this.marketType = marketType;
		this.klineTimeType = klineTimeType;
	}

	private List<IStickEntity> stickEntities;
	private MarketType marketType;
	private KlineTimeType klineTimeType;

	public List<IStickEntity> getStickEntities() {
		return stickEntities;
	}

	public void setStickEntities(List<IStickEntity> stickEntities) {
		this.stickEntities = stickEntities;
	}

	public MarketType getMarketType() {
		return marketType;
	}

	public void setMarketType(MarketType marketType) {
		this.marketType = marketType;
	}

	public KlineTimeType getKlineTimeType() {
		return klineTimeType;
	}

	public void setKlineTimeType(KlineTimeType klineTimeType) {
		this.klineTimeType = klineTimeType;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof KLine) {
			KLine kLine = (KLine) o;
			return kLine.getKlineTimeType() == getKlineTimeType()
					&& kLine.getMarketType() == getMarketType();
		}
		return super.equals(o);
	}
}
