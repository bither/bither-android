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

package net.bither.xrandom;

import net.bither.util.LogUtil;

import java.security.SecureRandom;

public class XRandom extends SecureRandom {

    private IUEntropy uEntropy;

    public XRandom(IUEntropy uEntropy) {
        this.uEntropy = uEntropy;
    }

    private byte[] getRandomBytes(int byteLength) {
        LogUtil.d(XRandom.class.getSimpleName(), "Request " + byteLength + " bytes from XRandom");
        byte[] uRandomBytes = getURandomBytes(byteLength);
        if (this.uEntropy == null) {
            return uRandomBytes;
        } else {
            byte[] result = new byte[byteLength];
            byte[] userEntropyBytes = getUEntropyBytes(byteLength);
            for (int i = 0; i < uRandomBytes.length; i++) {
                result[i] = (byte) (uRandomBytes[i] ^ userEntropyBytes[i]);
            }
            return result;
        }
    }

    private byte[] getURandomBytes(int length) {
        return URandom.nextBytes(length);
    }

    private byte[] getUEntropyBytes(int length) {
        return uEntropy.nextBytes(length);
    }

    @Override
    public void setSeed(long seed) {
    }

    @Override
    public synchronized void nextBytes(byte[] bytes) {
        byte[] nextBytes = getRandomBytes(bytes.length);
        if (nextBytes.length != bytes.length) {
            throw new RuntimeException("xrandom bytes length not match");
        }
        for (int i = 0;
             i < bytes.length && i < nextBytes.length;
             i++) {
            bytes[i] = nextBytes[i];
        }
    }

    @Override
    public byte[] generateSeed(int numBytes) {
        return getRandomBytes(numBytes);
    }
}
