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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.Arrays;

/**
 * Created by songchenwen on 14-9-11.
 */
public class UEntropyMic {

    private static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    public static final int SAMPPERSEC = 8000;
    private int buffersizebytes;

    private HandlerThread micThread;
    private Handler micHandler;
    private AudioRecord audioRecord;

    private UEntropyCollector collector;

    public UEntropyMic(UEntropyCollector collector) {
        micThread = new HandlerThread("UEntropyMicThread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        micThread.start();
        micHandler = new Handler(micThread.getLooper());

        this.collector = collector;

        micHandler.post(openRunnable);
    }

    public void release() {
        micHandler.removeCallbacksAndMessages(null);
        micHandler.post(closeRunnable);
    }

    private final Runnable openRunnable = new Runnable() {
        @Override
        public void run() {
            buffersizebytes = AudioRecord.getMinBufferSize(SAMPPERSEC, channelConfiguration,
                    audioEncoding);
            audioRecord = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC,
                    SAMPPERSEC, channelConfiguration, audioEncoding, buffersizebytes);
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.startRecording();
                micHandler.post(readRunnable);
            } else {
                release();
                collector.onError(new IllegalStateException("startRecording() called on an " +
                        "uninitialized AudioRecord."), UEntropyCollector.UEntropySource.Mic);
            }
        }
    };

    private final Runnable readRunnable = new Runnable() {
        @Override
        public void run() {
            if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord
                    .RECORDSTATE_RECORDING) {
                byte[] data = new byte[buffersizebytes];
                int outLength = audioRecord.read(data, 0, buffersizebytes);
                collector.onNewData(Arrays.copyOf(data, outLength),
                        UEntropyCollector.UEntropySource.Mic);
            }
            micHandler.post(readRunnable);
        }
    };

    private final Runnable closeRunnable = new Runnable() {
        @Override
        public void run() {
            if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord
                    .RECORDSTATE_RECORDING) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            micHandler.removeCallbacksAndMessages(null);
            micThread.quit();
        }
    };

}
