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

import android.graphics.Color;

import net.bither.BitherApplication;
import net.bither.BitherSetting.KlineTimeType;
import net.bither.BitherSetting.MarketType;
import net.bither.R;
import net.bither.charts.entity.BitherOHLCEntity;
import net.bither.charts.entity.DateValueEntity;
import net.bither.charts.entity.IStickEntity;
import net.bither.charts.entity.LineEntity;
import net.bither.charts.entity.ListChartData;
import net.bither.charts.entity.MarketDepthEntity;
import net.bither.charts.entity.OHLCEntity;
import net.bither.charts.view.GridChart;
import net.bither.charts.view.MACandleStickChart;
import net.bither.charts.view.MarketDepthChart;
import net.bither.model.Depth;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChartsUtil {
	public static List<IStickEntity> formatJsonArray(MarketType marketType,
			KlineTimeType klineTimeType, JSONArray jsonArray)
			throws JSONException {
		double rate = ExchangeUtil.getRate(marketType);
		List<IStickEntity> ohlc = new ArrayList<IStickEntity>();
		for (int i = jsonArray.length() - 1; i >= 0; i--) {
			JSONArray tickerArray = jsonArray.getJSONArray(i);
			long time = tickerArray.getLong(0) * 1000;
			Date date = new Date(time);
			String title = DateTimeUtil.getXTitle(klineTimeType, date);
			double open = tickerArray.getDouble(1) / 100 * rate;
			double high = tickerArray.getDouble(2) / 100 * rate;
			double low = tickerArray.getDouble(3) / 100 * rate;
			double close = tickerArray.getDouble(4) / 100 * rate;
			double volume = tickerArray.getDouble(5) / Math.pow(10, 8);
			if (volume == 0) {
				if (i == 0) {
					continue;
				} else {
					BitherOHLCEntity bitherOHLCEntity = (BitherOHLCEntity) ohlc
							.get(i - 1);
					open = bitherOHLCEntity.getOpen();
					high = bitherOHLCEntity.getHigh();
					low = bitherOHLCEntity.getLow();
					close = bitherOHLCEntity.getClose();
				}

			}
			BitherOHLCEntity bitherOHLCEntity = new BitherOHLCEntity(open,
					high, low, close, volume, title, time);

			ohlc.add(bitherOHLCEntity);
		}

		return ohlc;
	}

	public static synchronized void initMarketDepth(
			MarketDepthChart marketDepthChart, Depth depth, boolean isRefresh) {

		List<LineEntity<DateValueEntity>> lines = new ArrayList<LineEntity<DateValueEntity>>();
		LineEntity<DateValueEntity> MALineData = new LineEntity<DateValueEntity>();
		MALineData.setLineData(depth.getDateValueEntities());
		lines.add(MALineData);
		MarketDepthEntity marketDepthEntity = new MarketDepthEntity(lines,
				depth.getSplitIndex());
		marketDepthChart.setMareketDepthEntity(marketDepthEntity);

		int lineColor = Color.argb(30, 255, 255, 255);

		marketDepthChart.setLongitudeFontSize(14);
		marketDepthChart.setLatitudeFontSize(14);
		marketDepthChart.setLongitudeFontColor(Color.WHITE);
		marketDepthChart.setLatitudeColor(lineColor);
		marketDepthChart.setLatitudeFontColor(Color.WHITE);
		marketDepthChart.setLongitudeColor(lineColor);
		marketDepthChart.setMaxValue((int) depth.getMaxVolume());
		marketDepthChart.setMinValue(0);
		// marketDepthChart.setDisplayFrom(0);
		marketDepthChart
				.setDisplayNumber(depth.getDateValueEntities().size() - 1);
		marketDepthChart.setMinDisplayNumber(depth.getDateValueEntities()
				.size() - 1);
		marketDepthChart.setZoomBaseLine(0);
		marketDepthChart.setDisplayLongitudeTitle(true);
		marketDepthChart.setDisplayLatitudeTitle(true);
		marketDepthChart.setDisplayLatitude(true);
		marketDepthChart.setDisplayLongitude(true);
		marketDepthChart.setDisplayBorder(false);
		marketDepthChart.setDataQuadrantPaddingTop(5);
		marketDepthChart.setDataQuadrantPaddingBottom(5);
		marketDepthChart.setDataQuadrantPaddingLeft(5);
		marketDepthChart.setDataQuadrantPaddingRight(5);
		marketDepthChart.setAxisYTitleQuadrantWidth(50);
		marketDepthChart.setAxisXTitleQuadrantHeight(20);
		marketDepthChart.setAxisXPosition(GridChart.AXIS_X_POSITION_BOTTOM);
		marketDepthChart.setAxisYPosition(GridChart.AXIS_Y_POSITION_RIGHT);
		if (isRefresh) {
			marketDepthChart.invalidate();
		}
	}

	public synchronized static void initMACandleStickChart(
			MACandleStickChart macandlestickchart, List<IStickEntity> ohlc,
			boolean isRefresh) {
		List<LineEntity<DateValueEntity>> lines = new ArrayList<LineEntity<DateValueEntity>>();

		LineEntity<DateValueEntity> MA10 = new LineEntity<DateValueEntity>();
		MA10.setTitle("MA10");
		MA10.setLineColor(BitherApplication.mContext.getResources().getColor(
				R.color.ten_kline));
		MA10.setLineData(initMA(ohlc, 10));
		lines.add(MA10);

		LineEntity<DateValueEntity> MA30 = new LineEntity<DateValueEntity>();
		MA30.setTitle("MA25");
		MA30.setLineColor(BitherApplication.mContext.getResources().getColor(
				R.color.thrity_kline));
		MA30.setLineData(initMA(ohlc, 30));
		lines.add(MA30);

		int lineColor = Color.argb(30, 255, 255, 255);
		macandlestickchart.setLongitudeFontSize(14);
		macandlestickchart.setLatitudeFontSize(14);
		macandlestickchart.setLatitudeColor(lineColor);
		macandlestickchart.setLongitudeColor(lineColor);

		macandlestickchart.setLongitudeFontColor(Color.WHITE);
		macandlestickchart.setLatitudeFontColor(Color.WHITE);
		double maxValue = getMaxValue(ohlc);
		double minValue = getMinValue(ohlc);
		if (maxValue - minValue > 50) {
			macandlestickchart.setGraduation(10);
		} else {
			macandlestickchart.setGraduation(1);
		}
		macandlestickchart.setMaxSticksNum(ohlc.size());
		macandlestickchart.setLatitudeNum(5);
		macandlestickchart.setLongitudeNum(5);
		macandlestickchart.setMaxValue(getMaxValue(ohlc));
		macandlestickchart.setMinValue(getMinValue(ohlc));

		macandlestickchart.setDisplayBorder(false);
		macandlestickchart.setDisplayLongitudeTitle(true);
		macandlestickchart.setDisplayLatitudeTitle(true);
		macandlestickchart.setDisplayLatitude(true);
		macandlestickchart.setDisplayLongitude(true);

		macandlestickchart.setDataQuadrantPaddingTop(5);
		macandlestickchart.setDataQuadrantPaddingBottom(5);
		macandlestickchart.setDataQuadrantPaddingLeft(5);
		macandlestickchart.setDataQuadrantPaddingRight(5);
		macandlestickchart.setAxisYTitleQuadrantWidth(50);
		macandlestickchart.setAxisXTitleQuadrantHeight(20);
		macandlestickchart.setAxisXPosition(GridChart.AXIS_X_POSITION_BOTTOM);
		macandlestickchart.setAxisYPosition(GridChart.AXIS_Y_POSITION_RIGHT);

		macandlestickchart.setLinesData(lines);

		macandlestickchart.setStickData(new ListChartData<IStickEntity>(ohlc));
		if (isRefresh) {
			macandlestickchart.invalidate();
		}
	}

	private static double getMaxValue(List<IStickEntity> ohlc) {
		double maxValue = 0;
		for (IStickEntity biEntity : ohlc) {
			OHLCEntity ohlcEntity = (OHLCEntity) biEntity;
			if (ohlcEntity.getClose() > maxValue) {
				maxValue = ohlcEntity.getClose();
			}
		}
		return maxValue;

	}

	private static double getMinValue(List<IStickEntity> ohlc) {
		double minValue = Double.MAX_VALUE;
		for (IStickEntity biEntity : ohlc) {
			OHLCEntity ohlcEntity = (OHLCEntity) biEntity;
			if (ohlcEntity.getClose() < minValue) {
				minValue = ohlcEntity.getClose();
			}
		}
		return minValue;

	}

	private static List<DateValueEntity> initMA(List<IStickEntity> ohlc,
			int days) {

		if (days < 2) {
			return null;
		}

		List<DateValueEntity> MA5Values = new ArrayList<DateValueEntity>();

		float sum = 0;
		float avg = 0;
		for (int i = 0; i < ohlc.size(); i++) {
			float close = (float) ((BitherOHLCEntity) ohlc.get(i)).getClose();
			if (i < days) {
				sum = sum + close;
				avg = sum / (i + 1f);
			} else {
				sum = sum
						+ close
						- (float) ((BitherOHLCEntity) ohlc.get(i - days))
								.getClose();
				avg = sum / days;
			}
			MA5Values.add(new DateValueEntity(avg, ohlc.get(i).getDate()));
		}

		return MA5Values;
	}

}
