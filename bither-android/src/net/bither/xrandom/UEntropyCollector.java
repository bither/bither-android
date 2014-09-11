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

import net.bither.bitherj.crypto.IUEntropy;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by songchenwen on 14-9-11.
 */
public class UEntropyCollector implements IUEntropy {
    public static final int POOL_SIZE = 32 * 200;

    public static interface UEntropyCollectorListener {
        public void onError(Exception e, UEntropySource source);
    }

    private boolean shouldCollectData;
    private UEntropyCollectorListener listener;

    private PipedInputStream in;
    private PipedOutputStream out;
    private ExecutorService executor;

    public UEntropyCollector(UEntropyCollectorListener listener) {
        this.listener = listener;
        executor = Executors.newSingleThreadExecutor();
    }

    public void onNewData(final byte[] data, final UEntropySource source) {
        if (!shouldCollectData()) {
            return;
        }
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (!shouldCollectData()) {
                    return;
                }
                try {
                    int available = in.available();
                    int extraBytes = available + data.length - POOL_SIZE;
                    if (extraBytes <= 0) {
                        out.write(data);
                    } else if (extraBytes < data.length) {
                        out.write(Arrays.copyOf(data, data.length - extraBytes));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onError(Exception e, UEntropySource source) {
        if (listener != null) {
            listener.onError(e, source);
        }
    }

    public void start() throws IOException {
        shouldCollectData = true;
        in = new PipedInputStream(POOL_SIZE);
        out = new PipedOutputStream(in);
    }

    public void stop() {
        shouldCollectData = false;
        try {
            out.close();
            in.close();
            out = null;
            in = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean shouldCollectData() {
        return shouldCollectData;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        if (!shouldCollectData()) {
            throw new IllegalStateException("UEntropyCollector is not running");
        }
        try {
            while (in.available() < bytes.length) {

            }
            in.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
