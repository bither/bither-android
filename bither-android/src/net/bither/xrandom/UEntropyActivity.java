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
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.util.KeyUtil;
import net.bither.util.SecureCharSequence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by songchenwen on 14-9-11.
 */
public class UEntropyActivity extends Activity implements UEntropyCollector
        .UEntropyCollectorListener, DialogPassword.DialogPasswordListener {
    private static final long VIBRATE_DURATION = 50L;

    private Vibrator vibrator;

    private UEntropyCollector entropyCollector;

    private FrameLayout flOverlay;

    private static final Logger log = LoggerFactory.getLogger(UEntropyActivity.class);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, R.anim.scanner_in_exit);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setContentView(R.layout.activity_uentropy);
        flOverlay = (FrameLayout) findViewById(R.id.fl_overlay);

        entropyCollector = new UEntropyCollector(this);
        entropyCollector.addSources(new UEntropyCamera((SurfaceView) findViewById(R.id
                .scan_activity_preview), entropyCollector), new UEntropyMic(entropyCollector),
                new UEntropyMotion(this, entropyCollector));

        flOverlay.postDelayed(new Runnable() {
            @Override
            public void run() {
                DialogPassword dialogPassword = new DialogPassword(UEntropyActivity.this,
                        UEntropyActivity.this);
                dialogPassword.setNeedCancelEvent(true);
                dialogPassword.show();
            }
        }, 600);
    }

    @Override
    protected void onResume() {
        super.onResume();
        entropyCollector.onResume();
    }

    @Override
    protected void onPause() {
        entropyCollector.onPause();
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
    public void onError(Exception e, IUEntropySource source) {
        log.warn("UEntropyCollectorError source: {}, {}", source.type().name(), e.getMessage());
    }

    @Override
    public void onPasswordEntered(final SecureCharSequence password) {
        if (password == null) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            new ThreadNeedService(null, UEntropyActivity.this) {
                @Override
                public void runWithService(BlockchainService service) {
                    try {
                        entropyCollector.start();
                        KeyUtil.addPrivateKeyByRandomWithPassphras(service, entropyCollector,
                                password, 5);
                        entropyCollector.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    password.wipe();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                }
            }.start();
        }
    }
}
