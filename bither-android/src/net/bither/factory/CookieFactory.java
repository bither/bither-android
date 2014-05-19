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

package net.bither.factory;

import net.bither.api.GetCookieApi;
import net.bither.preference.PersistentCookieStore;

public class CookieFactory {

	private static boolean isRunning = false;

	private CookieFactory() {

	}

	public synchronized static boolean initCookie() {
		boolean success = true;
		isRunning = true;
		PersistentCookieStore persistentCookieStore = PersistentCookieStore
				.getInstance();
		if (persistentCookieStore.getCookies() == null
				|| persistentCookieStore.getCookies().size() == 0) {
			try {
				GetCookieApi getCookieApi = new GetCookieApi();
				getCookieApi.handleHttpPost();
			} catch (Exception e) {
				success = false;
				e.printStackTrace();
			}
		}
		isRunning = false;
		return success;

	}

	public static boolean isRunning() {
		return isRunning;
	}

}
