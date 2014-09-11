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

import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import net.bither.camera.CameraManager;

import java.io.IOException;

/**
 * Created by songchenwen on 14-9-11.
 */
public class UEntropyCamera implements SurfaceHolder.Callback {
    private static final long AUTO_FOCUS_INTERVAL_MS = 2500L;

    private static boolean DISABLE_CONTINUOUS_AUTOFOCUS = Build.MODEL.equals("GT-I9100") //
            // Galaxy S2
            || Build.MODEL.equals("SGH-T989") // Galaxy S2
            || Build.MODEL.equals("SGH-T989D") // Galaxy S2 X
            || Build.MODEL.equals("SAMSUNG-SGH-I727") // Galaxy S2 Skyrocket
            || Build.MODEL.equals("GT-I9300") // Galaxy S3
            || Build.MODEL.equals("GT-N7000"); // Galaxy Note


    private final CameraManager cameraManager = new CameraManager();
    private SurfaceHolder surfaceHolder;
    private HandlerThread cameraThread;
    private Handler cameraHandler;

    private UEntropyCollector collector;

    public UEntropyCamera(SurfaceView surfaceView, UEntropyCollector collector) {
        cameraThread = new HandlerThread("UEntropyCameraThread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());

        this.collector = collector;
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void release() {
        cameraHandler.post(closeRunnable);
        surfaceHolder.removeCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        cameraHandler.post(openRunnable);
    }


    private final Runnable openRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                final Camera camera = cameraManager.open(surfaceHolder,
                        !DISABLE_CONTINUOUS_AUTOFOCUS);

                final String focusMode = camera.getParameters().getFocusMode();
                final boolean nonContinuousAutoFocus = Camera.Parameters.FOCUS_MODE_AUTO.equals
                        (focusMode) || Camera.Parameters.FOCUS_MODE_MACRO.equals(focusMode);

                if (nonContinuousAutoFocus) {
                    cameraHandler.post(new AutoFocusRunnable(camera));
                }

                cameraHandler.post(fetchCameraDataRunnable);

            } catch (final IOException x) {
                collector.onError(x, UEntropyCollector.UEntropySource.Camera);
            } catch (final RuntimeException x) {
                collector.onError(x, UEntropyCollector.UEntropySource.Camera);
            }
        }
    };

    private final Runnable closeRunnable = new Runnable() {
        @Override
        public void run() {
            cameraManager.close();

            // cancel background thread
            cameraHandler.removeCallbacksAndMessages(null);
            cameraThread.quit();
        }
    };

    private final class AutoFocusRunnable implements Runnable {
        private final Camera camera;

        public AutoFocusRunnable(final Camera camera) {
            this.camera = camera;
        }

        @Override
        public void run() {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(final boolean success, final Camera camera) {
                    // schedule again
                    cameraHandler.postDelayed(AutoFocusRunnable.this, AUTO_FOCUS_INTERVAL_MS);
                }
            });
        }
    }

    private final Runnable fetchCameraDataRunnable = new Runnable() {

        @Override
        public void run() {
            cameraManager.requestPreviewFrame(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(final byte[] data, final Camera camera) {
                    collector.onNewData(data, UEntropyCollector.UEntropySource.Camera);
                    cameraHandler.post(fetchCameraDataRunnable);
                }
            });
        }
    };


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
