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

package net.bither.xrandom.audio;

import com.google.common.primitives.Shorts;

/**
 * Created by songchenwen on 14-9-15.
 */
public class AmplitudeData {
    private static final int sampleCount = 256;

    private int amplitude;

    public AmplitudeData(byte[] rawData) {
        if (rawData == null) {
            amplitude = 0;
            return;
        }

        int step = rawData.length / Shorts.BYTES / sampleCount;

        int count = 0;
        double sum = 0;
        for (int i = 0;
             i < rawData.length - Shorts.BYTES;
             i += step) {
            byte[] bytes = new byte[Shorts.BYTES];
            for (int j = 0;
                 j < Shorts.BYTES;
                 j++) {
                bytes[j] = rawData[i + j];
            }
            short s = Shorts.fromByteArray(bytes);
            sum += s * s;
            count++;
        }
        amplitude = (int) Math.sqrt(sum / count);
    }

    public int amplitude() {
        return amplitude;
    }
}
