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

package net.bither.runnable;

import java.io.File;

import net.bither.api.BitherErrorApi;
import net.bither.util.FileUtil;

public class AddErrorMsgRunnable extends BaseRunnable {

	@Override
	public void run() {
		obtainMessage(HandlerMessage.MSG_PREPARE);
		try {
			File errorFile = FileUtil.getErrorLogFile();
			if (errorFile.exists()) {
				String errorMsg = FileUtil.readFile(errorFile);
				BitherErrorApi addFeedbackApi = new BitherErrorApi(errorMsg);
				addFeedbackApi.handleHttpPost();
				errorFile.delete();
				obtainMessage(HandlerMessage.MSG_SUCCESS);
			}

		} catch (Exception e) {
			e.printStackTrace();
			obtainMessage(HandlerMessage.MSG_FAILURE_NETWORK);
		}

	}
}
