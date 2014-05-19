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

import java.util.UUID;

import javax.annotation.Nonnull;

public class Bluetooth {
	public static final UUID BLUETOOTH_UUID = UUID
			.fromString("3357A7BB-762D-464A-8D9A-DCA592D57D5B");
	public static final String MAC_URI_PARAM = "bt";

	public static String compressMac(@Nonnull final String mac) {
		return mac.replaceAll(":", "");
	}

	public static String decompressMac(@Nonnull final String compressedMac) {
		final StringBuilder mac = new StringBuilder();
		for (int i = 0; i < compressedMac.length(); i += 2)
			mac.append(compressedMac.substring(i, i + 2)).append(':');
		mac.setLength(mac.length() - 1);

		return mac.toString();
	}
}
