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

import net.bither.api.BitherMytransactionsApi;

import org.json.JSONObject;

/**
 * Created by nn on 14-7-27.
 */
public class CheckAddressRunnable extends BaseRunnable {
    private final static String SPECIAL_TYPE = "special_type";

    private String address;

    public CheckAddressRunnable(String address) {
        this.address = address;

    }

    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);
        try {
            BitherMytransactionsApi bitherMytransactionsApi = new BitherMytransactionsApi(this.address);
            bitherMytransactionsApi.handleHttpGet();
            String result = bitherMytransactionsApi.getResult();
            JSONObject json = new JSONObject(result);
            boolean isCheck = json.isNull(SPECIAL_TYPE);
            obtainMessage(HandlerMessage.MSG_SUCCESS, isCheck);
        } catch (Exception e) {
            e.printStackTrace();
            obtainMessage(HandlerMessage.MSG_FAILURE);
        }
    }
}
