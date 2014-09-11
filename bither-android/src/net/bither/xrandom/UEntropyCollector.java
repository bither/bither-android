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

package net.bither.xrandom;

import net.bither.bitherj.utils.LogUtil;
import net.bither.bitherj.utils.Utils;

/**
 * Created by songchenwen on 14-9-11.
 */
public class UEntropyCollector {
    public static final int ENTROPY_CACHE_LENGTH = 32 * 100;

    public static interface UEntropyCollectorListener {
        public void onError(Exception e, UEntropySource source);
    }

    private boolean shouldCollectData;
    private UEntropyCollectorListener listener;

    public UEntropyCollector(UEntropyCollectorListener listener) {
        this.listener = listener;
    }

    public void onNewData(byte[] data, UEntropySource source) {
        if(!shouldCollectData()){
            return;
        }
        LogUtil.d(UEntropyCollector.class.getSimpleName(), "source: " + source.name() + "\ndata: " +
                "" + Utils.bytesToHexString(source.processData(data)));
    }

    public void onError(Exception e, UEntropySource source) {
        if (listener != null) {
            listener.onError(e, source);
        }
    }

    public void start(){
        shouldCollectData = true;
    }

    public void stop(){
        shouldCollectData = false;
    }

    public boolean shouldCollectData(){
        return shouldCollectData;
    }

    public enum UEntropySource {
        Unknown, Camera, Mic, Motion;

        public byte[] processData(byte[] data) {
            switch (this) {
                case Camera:
                    return data;
                case Mic:
                    return data;
                case Motion:
                    return data;
                default:
                    return data;
            }
        }
    }
}
