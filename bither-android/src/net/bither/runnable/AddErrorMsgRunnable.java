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

import net.bither.BitherApplication;
import net.bither.api.BitherErrorApi;
import net.bither.bitherj.utils.Utils;
import net.bither.exception.UEHandler;
import net.bither.util.StringUtil;

import java.io.File;

public class AddErrorMsgRunnable extends BaseRunnable {

    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);
        try {
            File errorFile = new File(UEHandler.getErrorLogFile(),"error.log");
            if (errorFile.exists()) {
                String errorMsg = Utils.readFile(errorFile);
                if (!StringUtil.isEmpty(errorMsg)) {
                    BitherErrorApi addFeedbackApi = new BitherErrorApi(errorMsg);
                    addFeedbackApi.handleHttpPost();
                }
                errorFile.delete();
                obtainMessage(HandlerMessage.MSG_SUCCESS);
            }

        } catch (Exception e) {
            e.printStackTrace();
            obtainMessage(HandlerMessage.MSG_FAILURE_NETWORK);
        }

    }
}
