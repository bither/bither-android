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
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import net.bither.R;
import net.bither.ui.base.ScannerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by songchenwen on 14-9-11.
 */
public class UEntropyActivity extends Activity implements UEntropyCollector
        .UEntropyCollectorListener {
    private static final long VIBRATE_DURATION = 50L;

    protected ScannerView scannerView;
    protected FrameLayout flOverlayContainer;
    private Vibrator vibrator;

    private UEntropyCollector entropyCollector;
    private UEntropyCamera camera;

    private static final Logger log = LoggerFactory.getLogger(UEntropyActivity.class);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, R.anim.scanner_in_exit);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setContentView(R.layout.scan_activity);
        flOverlayContainer = (FrameLayout) findViewById(R.id.fl_overlay_container);
        scannerView = (ScannerView) findViewById(R.id.scan_activity_mask);
        entropyCollector = new UEntropyCollector(this);
        entropyCollector.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = new UEntropyCamera((SurfaceView) findViewById(R.id.scan_activity_preview),
                entropyCollector);
    }

    @Override
    protected void onPause() {
        camera.onPause();
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
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void vibrate() {
        vibrator.vibrate(VIBRATE_DURATION);
    }

    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.scanner_out_enter, 0);
    }

    @Override
    public void onError(Exception e, UEntropyCollector.UEntropySource source) {
        log.warn("UEntropyCollectorError source: {}, {}", source.name(), e.getMessage());
    }
}
