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

import android.os.Handler;
import android.os.Looper;

public class ThreadUtil {

	private static Handler mainThreadHandler;

	private ThreadUtil() {
	}

	public static Handler getMainThreadHandler() {
		if (mainThreadHandler == null) {
			mainThreadHandler = new Handler(Looper.getMainLooper());
		}
		return mainThreadHandler;
	}

	public static void runOnMainThread(Runnable runnable) {
        if(Looper.myLooper() == Looper.getMainLooper()){
            runnable.run();
        }else{
		    getMainThreadHandler().post(runnable);
        }
	}
}
