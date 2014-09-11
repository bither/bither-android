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

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import net.bither.R;
import net.bither.camera.CameraManager;
import net.bither.ui.base.ScannerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by songchenwen on 14-9-11.
 */
public class UEntropyActivity extends Activity implements SurfaceHolder.Callback {
    private static final long VIBRATE_DURATION = 50L;
    private static final long AUTO_FOCUS_INTERVAL_MS = 2500L;

    private final CameraManager cameraManager = new CameraManager();
    protected ScannerView scannerView;
    private SurfaceHolder surfaceHolder;
    protected FrameLayout flOverlayContainer;
    private Vibrator vibrator;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private UEntropyCollector entropyCollector;

    private static boolean DISABLE_CONTINUOUS_AUTOFOCUS = Build.MODEL.equals("GT-I9100") //
            // Galaxy S2
            || Build.MODEL.equals("SGH-T989") // Galaxy S2
            || Build.MODEL.equals("SGH-T989D") // Galaxy S2 X
            || Build.MODEL.equals("SAMSUNG-SGH-I727") // Galaxy S2 Skyrocket
            || Build.MODEL.equals("GT-I9300") // Galaxy S3
            || Build.MODEL.equals("GT-N7000"); // Galaxy Note

    private static final Logger log = LoggerFactory.getLogger(UEntropyActivity.class);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, R.anim.scanner_in_exit);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setContentView(R.layout.scan_activity);
        flOverlayContainer = (FrameLayout) findViewById(R.id.fl_overlay_container);
        scannerView = (ScannerView) findViewById(R.id.scan_activity_mask);
        entropyCollector = new UEntropyCollector();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraThread = new HandlerThread("cameraThread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());

        final SurfaceView surfaceView = (SurfaceView) findViewById(R.id.scan_activity_preview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        cameraHandler.post(openRunnable);
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width,
                               final int height) {
    }

    @Override
    protected void onPause() {
        cameraHandler.post(closeRunnable);

        surfaceHolder.removeCallback(this);

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // don't launch camera app
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
//                cameraHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        cameraManager
//                                .setTorch(keyCode == KeyEvent.KEYCODE_VOLUME_UP);
//                    }
//                });
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void vibrate() {
        vibrator.vibrate(VIBRATE_DURATION);
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


                final Rect framingRect = cameraManager.getFrame();
                final Rect framingRectInPreview = cameraManager.getFramePreview();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scannerView.setFraming(framingRect, framingRectInPreview);
                    }
                });
            } catch (final IOException x) {
                log.info("problem opening camera", x);
                finish();
            } catch (final RuntimeException x) {
                log.info("problem opening camera", x);
                finish();
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
                    entropyCollector.onNewData(data, UEntropyCollector.UEntropySource.CAMERA);
                    cameraHandler.post(fetchCameraDataRunnable);
                }
            });
        }
    };

    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.scanner_out_enter, 0);
    }
}
