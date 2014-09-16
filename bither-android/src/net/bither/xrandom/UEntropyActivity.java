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
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.utils.LogUtil;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.util.KeyUtil;
import net.bither.util.SecureCharSequence;
import net.bither.xrandom.audio.AudioVisualizerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 14-9-11.
 */
public class UEntropyActivity extends Activity implements UEntropyCollector
        .UEntropyCollectorListener, DialogPassword.DialogPasswordListener {
    public static final String PrivateKeyCountKey = UEntropyActivity.class.getName() + "" +
            ".private_key_count_key";

    private static final long VIBRATE_DURATION = 50L;

    private Vibrator vibrator;

    private UEntropyCollector entropyCollector;

    private View vOverlay;

    private int targetCount;

    private static final Logger log = LoggerFactory.getLogger(UEntropyActivity.class);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, R.anim.scanner_in_exit);
        targetCount = getIntent().getExtras().getInt(PrivateKeyCountKey, 0);
        if (targetCount <= 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_uentropy);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vOverlay = findViewById(R.id.v_overlay);

        entropyCollector = new UEntropyCollector(this);

        entropyCollector.addSources(
                new UEntropyCamera((SurfaceView) findViewById(R.id.scan_activity_preview), entropyCollector),
                new UEntropyMic(entropyCollector, (AudioVisualizerView) findViewById(R.id.v_mic)),
                new UEntropyMotion(this, entropyCollector)
        );

        vOverlay.postDelayed(new Runnable() {
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
    public void onUEntropySourceError(Exception e, IUEntropySource source) {
        log.warn("UEntropyCollectorError source: {}, {}", source.type().name(), e.getMessage());
    }

    private void startAnimation() {
        AlphaAnimation anim = new AlphaAnimation(1, 0.6f);
        anim.setFillAfter(true);
        anim.setDuration(500);
        vOverlay.startAnimation(anim);
    }

    private void stopAnimation(final Runnable finishRun) {
        AlphaAnimation anim = new AlphaAnimation(0.6f, 1);
        anim.setFillAfter(true);
        anim.setDuration(500);
        vOverlay.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                finishRun.run();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    public void onPasswordEntered(final SecureCharSequence password) {
        if (password == null) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            startAnimation();
            new ThreadNeedService(null, UEntropyActivity.this) {
                @Override
                public void runWithService(BlockchainService service) {
                    boolean success = false;
                    final ArrayList<String> addresses = new ArrayList<String>();
                    try {
                        entropyCollector.start();
                        LogUtil.i(UEntropyActivity.class.getSimpleName(), "start generating");
                        addresses.clear();
                        List<Address> as = KeyUtil.addPrivateKeyByRandomWithPassphras(service,
                                entropyCollector, password, targetCount);
                        if (as != null && as.size() > 0) {
                            for (int i = as.size() - 1;
                                 i >= 0;
                                 i--) {
                                addresses.add(as.get(i).getAddress());
                            }
                        }
                        LogUtil.i(UEntropyActivity.class.getSimpleName(), "stop generating");
                        entropyCollector.stop();
                        success = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    password.wipe();
                    final Runnable finishRun;
                    if (success) {
                        finishRun = new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent();
                                intent.putExtra(BitherSetting.INTENT_REF
                                        .ADD_PRIVATE_KEY_SUGGEST_CHECK_TAG,
                                        AppSharedPreference.getInstance().getPasswordSeed() ==
                                                null);
                                intent.putExtra(BitherSetting.INTENT_REF
                                        .ADDRESS_POSITION_PASS_VALUE_TAG, addresses);
                                setResult(RESULT_OK, intent);
                                finish();
                            }
                        };
                    } else {
                        finishRun = new Runnable() {
                            @Override
                            public void run() {
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                        };
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopAnimation(finishRun);
                        }
                    });
                }
            }.start();
        }
    }
}
