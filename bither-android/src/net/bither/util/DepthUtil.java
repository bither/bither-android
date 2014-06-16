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

import net.bither.BitherSetting.MarketType;
import net.bither.model.Depth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DepthUtil {
	private static List<Depth> depths = init();

	private static List<Depth> init() {
		File file = FileUtil.getDepthFile();
		@SuppressWarnings("unchecked")
		List<Depth> depths = (List<Depth>) FileUtil.deserialize(file);
		if (depths == null) {
			depths = new ArrayList<Depth>();
		}
		return depths;
	}

	public static Depth getKDepth(MarketType marketType) {
		synchronized (depths) {
			for (Depth depth : depths) {
				if (depth.getMarketType() == marketType) {
					return depth;
				}
			}
			return null;
		}
	}

	public static void addDepth(Depth depth) {
		synchronized (depths) {
			File file = FileUtil.getDepthFile();
			depths.remove(depth);
			depths.add(depth);
			FileUtil.serializeObject(file, depths);
		}
	}

}
