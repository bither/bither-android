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

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import net.bither.bitherj.utils.Sha256Hash;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UEntropyCollector implements IUEntropy, IUEntropySource {
    public static final int POOL_SIZE = 32 * 200;
    private static final int ENTROPY_XOR_MULTIPLIER = (int) Math.pow(2, 4);

    public static interface UEntropyCollectorListener {
        public void onUEntropySourceError(Exception e, IUEntropySource source);
    }

    private boolean shouldCollectData;
    private UEntropyCollectorListener listener;

    private PipedInputStream in;
    private PipedOutputStream out;

    private HashSet<IUEntropySource> sources;
    private boolean paused;

    private ExecutorService executor;

    public UEntropyCollector(UEntropyCollectorListener listener) {
        this.listener = listener;
        paused = true;
        sources = new HashSet<IUEntropySource>();
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
                byte[] processedData = source.processData(data);
                try {
                    int available = in.available();
                    int extraBytes = available + processedData.length - POOL_SIZE;
                    if (extraBytes <= 0) {
                        out.write(processedData);
                    } else if (extraBytes < processedData.length) {
                        out.write(Arrays.copyOf(processedData, processedData.length - extraBytes));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onError(Exception e, IUEntropySource source) {
        if (sources.contains(source)) {
            source.onPause();
            Iterator<IUEntropySource> iterator = sources.iterator();
            while (iterator.hasNext()) {
                IUEntropySource item = iterator.next();
                if (item.equals(source)) {
                    iterator.remove();
                }
            }
        }
        if (listener != null) {
            listener.onUEntropySourceError(e, source);
        }
    }

    public void start() throws IOException {
        if (shouldCollectData) {
            return;
        }
        shouldCollectData = true;
        in = new PipedInputStream(POOL_SIZE);
        out = new PipedOutputStream(in);
    }

    public void stop() {
        if (!shouldCollectData) {
            return;
        }
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
    public byte[] nextBytes(int length) {
        byte[] bytes = null;
        if (!shouldCollectData()) {
            throw new IllegalStateException("UEntropyCollector is not running");
        }
        try {
            for (int i = 0;
                 i < ENTROPY_XOR_MULTIPLIER;
                 i++) {
                byte[] itemBytes = new byte[length];
                while (in.available() < itemBytes.length) {
                    if (!shouldCollectData()) {
                        throw new IllegalStateException("UEntropyCollector is not running");
                    }
                }
                in.read(itemBytes);
                if (i == ENTROPY_XOR_MULTIPLIER - 1) {
                    itemBytes = Sha256Hash.create(itemBytes).getBytes();
                }
                if (bytes == null) {
                    bytes = itemBytes;
                } else {
                    for (int k = 0;
                         k < bytes.length && k < itemBytes.length;
                         k++) {
                        bytes[k] = (byte) (bytes[k] ^ itemBytes[k]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[length];
        }
        return bytes;
    }

    public enum UEntropySource {
        Unknown, Camera(8), Mic(16), Sensor;

        private int bytesInOneBatch;

        UEntropySource(int bytesInOneBatch) {
            this.bytesInOneBatch = bytesInOneBatch;
        }

        UEntropySource() {
            this(1);
        }

        public byte[] processData(byte[] data) {
            if (data.length <= bytesInOneBatch) {
                return data;
            }
            byte[] result = new byte[bytesInOneBatch + 1];
            byte[] locatorBytes;
            for (int i = 0;
                 i < bytesInOneBatch;
                 i++) {
                int position = (int) (Math.random() * data.length);
                try {
                    locatorBytes = URandom.nextBytes(Ints.BYTES);
                    int value = Math.abs(Ints.fromByteArray(locatorBytes));
                    position = (int) (((float) value / (float) Integer.MAX_VALUE) * data.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                position = Math.min(Math.max(position, 0), data.length - 1);
                result[i] = data[position];
            }
            byte[] timestampBytes = Longs.toByteArray(System.currentTimeMillis());
            result[bytesInOneBatch] = timestampBytes[timestampBytes.length - 1];
            return result;
        }
    }

    public void addSource(IUEntropySource source) {
        sources.add(source);
        if (!paused) {
            source.onResume();
        }
    }

    public void addSources(IUEntropySource... sources) {
        for (IUEntropySource source : sources) {
            addSource(source);
        }
    }

    @Override
    public void onResume() {
        paused = false;
        for (IUEntropySource source : sources()) {
            source.onResume();
        }
    }

    @Override
    public void onPause() {
        paused = true;
        for (IUEntropySource source : sources()) {
            source.onPause();
        }
    }

    @Override
    public UEntropySource type() {
        return UEntropySource.Unknown;
    }

    public HashSet<IUEntropySource> sources() {
        return new HashSet<>(sources);
    }
}
