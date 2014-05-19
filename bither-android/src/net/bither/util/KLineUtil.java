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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.bither.BitherSetting.KlineTimeType;
import net.bither.BitherSetting.MarketType;
import net.bither.model.KLine;

public class KLineUtil {
	private static List<KLine> kLines = init();

	private static List<KLine> init() {
		File file = FileUtil.getKlineFile();
		@SuppressWarnings("unchecked")
		List<KLine> kLines = (List<KLine>) FileUtil.deserialize(file);
		if (kLines == null) {
			kLines = new ArrayList<KLine>();
		}
		return kLines;
	}

	public static KLine getKLine(MarketType marketType,
			KlineTimeType klineTimeType) {
		synchronized (kLines) {
			for (KLine kLine : kLines) {
				if (kLine.getMarketType() == marketType
						&& kLine.getKlineTimeType() == klineTimeType) {
					return kLine;
				}
			}
			return null;
		}
	}

	public static void addKline(KLine kLine) {
		synchronized (kLines) {
			File file = FileUtil.getKlineFile();
			kLines.remove(kLine);
			kLines.add(kLine);
			FileUtil.serializeObject(file, kLines);
		}
	}

}
