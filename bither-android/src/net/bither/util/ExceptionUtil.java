/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.util;

import net.bither.R;
import net.bither.bitherj.api.http.HttpSetting;

/**
 * Created by songchenwen on 15/1/15.
 */
public class ExceptionUtil {

    public static final int getHDMHttpExceptionMessage(int code) {
        switch (code) {
            case HttpSetting.HDMBIdIsAlready:
                return R.string.hdm_exception_bid_already_exists;
            case HttpSetting.MessageSignatureIsWrong:
                return R.string.hdm_keychain_add_sign_server_qr_code_error;
            default:
                return R.string.network_or_connection_error;
        }
    }
}
