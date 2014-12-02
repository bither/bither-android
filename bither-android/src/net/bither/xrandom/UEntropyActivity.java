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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.cold.AddColdAddressActivity;
import net.bither.activity.hot.AddHotAddressActivity;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.BaseActivity;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogGenerateAddressFinalConfirm;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.KeyUtil;
import net.bither.util.PlaySound;
import net.bither.xrandom.audio.AudioVisualizerView;
import net.bither.xrandom.sensor.SensorVisualizerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class UEntropyActivity extends BaseActivity implements UEntropyCollector
        .UEntropyCollectorListener, IDialogPasswordListener {
    public static final String PrivateKeyCountKey = UEntropyActivity.class.getName() + ".private_key_count_key";
    private static final Logger log = LoggerFactory.getLogger(UEntropyActivity.class);
    private static final int MinGeneratingTime = 5000;

    private static final int VIBRATE_DURATION = 50;

    private Vibrator vibrator;

    private UEntropyCollector entropyCollector;
    private GenerateThread generateThread;

    private View vOverlay;
    private View vOverlayTop;
    private View vOverlayBottom;
    private View ivOverlayTop;
    private View ivOverlayBottom;
    private ProgressBar pb;
    private DialogProgress dpCancel;
    private DialogConfirmTask dialogCancelConfirm;

    private boolean isFinishing = false;

    private int targetCount;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, R.anim.uentropy_activity_start_exit);
        targetCount = getIntent().getExtras().getInt(PrivateKeyCountKey, 0);
        if (targetCount <= 0) {
            super.finish();
            return;
        }
        setContentView(R.layout.activity_uentropy);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vOverlay = findViewById(R.id.v_overlay);
        vOverlayTop = findViewById(R.id.v_overlay_top);
        vOverlayBottom = findViewById(R.id.v_overlay_bottom);
        ivOverlayTop = findViewById(R.id.iv_overlay_top);
        ivOverlayBottom = findViewById(R.id.iv_overlay_bottom);
        pb = (ProgressBar) findViewById(R.id.pb);
        findViewById(R.id.ibtn_cancel).setOnClickListener(cancelClick);
        dpCancel = new DialogProgress(this, R.string.xrandom_stopping);
        dpCancel.setCancelable(false);
        dialogCancelConfirm = new DialogConfirmTask(this,
                getString(R.string.xrandom_cancel_confirm), new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isFinishing = true;
                        dpCancel.show();
                        generateThread.cancel(cancelRunnable);
                    }
                });
            }
        });

        entropyCollector = new UEntropyCollector(this);

        entropyCollector.addSources(new UEntropyCamera((SurfaceView) findViewById(R.id.v_camera),
                entropyCollector), new UEntropyMic(entropyCollector,
                (AudioVisualizerView) findViewById(R.id.v_mic)), new UEntropySensor(this,
                entropyCollector, (SensorVisualizerView) findViewById(R.id.v_sensor)));
        generateThread = new GenerateThread();

        vOverlay.postDelayed(new Runnable() {
            @Override
            public void run() {
                DialogPassword dialogPassword = new DialogPassword(UEntropyActivity.this,
                        UEntropyActivity.this);
                dialogPassword.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                dialogPassword.setNeedCancelEvent(true);
                dialogPassword.show();
            }
        }, 1400);
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
        if(isFinishing){
            return;
        }
        if (generateThread.isAlive()) {
            cancelGenerate();
        } else {
            cancelRunnable.run();
        }
    }

    private View.OnClickListener cancelClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    private void cancelGenerate() {
        dialogCancelConfirm.show();
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
        if (dialogCancelConfirm != null && dialogCancelConfirm.isShowing()) {
            dialogCancelConfirm.dismiss();
        }
        if (dpCancel != null && dpCancel.isShowing()) {
            dpCancel.dismiss();
        }
        super.finish();
    }

    @Override
    public void onUEntropySourceError(Exception e, IUEntropySource source) {
        log.warn("UEntropyCollectorError source: {}, {}", source.type().name(), e.getMessage());
        if (entropyCollector.sources().size() == 0) {
            entropyCollector.stop();
        }
    }

    private void startAnimation() {
        vOverlay.postDelayed(new Runnable() {
            @Override
            public void run() {
                PlaySound.play(R.raw.xrandom_open_sound, null);
                TranslateAnimation topAnim = new TranslateAnimation(0, 0, 0, -vOverlayTop
                        .getHeight());
                topAnim.setFillAfter(true);
                topAnim.setDuration(600);
                TranslateAnimation bottomAnim = new TranslateAnimation(0, 0, 0, vOverlayBottom
                        .getHeight());
                bottomAnim.setFillAfter(true);
                bottomAnim.setDuration(600);
                ivOverlayTop.setVisibility(View.VISIBLE);
                ivOverlayBottom.setVisibility(View.VISIBLE);
                vOverlayTop.startAnimation(topAnim);
                vOverlayBottom.startAnimation(bottomAnim);
            }
        }, 1200);
    }

    private void stopAnimation(final Runnable finishRun) {
        PlaySound.play(R.raw.xrandom_close_sound, null);
        TranslateAnimation topAnim = new TranslateAnimation(0, 0, -vOverlayTop.getHeight(), 0);
        topAnim.setFillBefore(true);
        topAnim.setFillAfter(true);
        topAnim.setDuration(500);
        TranslateAnimation bottomAnim = new TranslateAnimation(0, 0, vOverlayBottom.getHeight(), 0);
        bottomAnim.setFillBefore(true);
        bottomAnim.setFillAfter(true);
        bottomAnim.setDuration(500);
        ivOverlayTop.setVisibility(View.VISIBLE);
        ivOverlayBottom.setVisibility(View.VISIBLE);
        bottomAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ivOverlayTop.setVisibility(View.GONE);
                ivOverlayBottom.setVisibility(View.GONE);
                vOverlay.postDelayed(finishRun, 600);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        vOverlayTop.startAnimation(topAnim);
        vOverlayBottom.startAnimation(bottomAnim);

    }

    private void onProgress(final double progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int p = (int) (pb.getMax() * progress);
                pb.setProgress(p);
            }
        });
    }

    private void onSuccess(final ArrayList<String> addresses) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isFinishing = true;
                if (dpCancel != null && dpCancel.isShowing()) {
                    dpCancel.dismiss();
                }
                stopAnimation(new Runnable() {
                    @Override
                    public void run() {
                        DialogGenerateAddressFinalConfirm dialog = new
                                DialogGenerateAddressFinalConfirm(UEntropyActivity.this,
                                addresses.size(), true);
                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                Intent intent = new Intent();
                                intent.putExtra(BitherSetting.INTENT_REF
                                        .ADD_PRIVATE_KEY_SUGGEST_CHECK_TAG,
                                        AppSharedPreference.getInstance().getPasswordSeed() ==
                                                null);
                                intent.putExtra(BitherSetting.INTENT_REF
                                        .ADDRESS_POSITION_PASS_VALUE_TAG, addresses);
                                setResult(RESULT_OK, intent);
                                finish();
                                overridePendingTransition(0, R.anim.slide_out_bottom);
                            }
                        });
                        dialog.show();
                    }
                });
            }
        });
    }

    private void onFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopAnimation(new Runnable() {
                    @Override
                    public void run() {
                        isFinishing = true;
                        String message;
                        if (entropyCollector.sources().size() == 0) {
                            message = getString(R.string.xrandom_no_source);
                        } else {
                            message = getString(R.string.xrandom_generating_failed);
                        }
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        backToFromActivity();
                                    }
                                });
                            }
                        };
                        DialogConfirmTask dialog = new DialogConfirmTask(UEntropyActivity
                                .this, message, runnable, runnable);
                        dialog.setCancelable(false);
                        dialog.show();
                    }
                });
            }
        });
    }

    private void backToFromActivity() {
        isFinishing = true;
        Class target;
        if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD) {
            target = AddColdAddressActivity.class;
        } else {
            target = AddHotAddressActivity.class;
        }
        Intent intent = new Intent(UEntropyActivity.this, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivity(intent);
        overridePendingTransition(R.anim.uentropy_activity_back_enter, 0);
        vOverlay.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 300);
    }

    private Runnable cancelRunnable = new Runnable() {
        @Override
        public void run() {
            isFinishing = true;
            if (dpCancel.isShowing()) {
                dpCancel.dismiss();
            }
            stopAnimation(new Runnable() {
                @Override
                public void run() {
                    backToFromActivity();
                }
            });
        }
    };

    @Override
    public void onPasswordEntered(final SecureCharSequence password) {
        if (password == null) {
            backToFromActivity();
        } else {
            startAnimation();
            generateThread.setPassword(password);
            generateThread.start();
        }
    }

    private class GenerateThread extends ThreadNeedService {
        private double saveProgress = 0.1;
        private double startProgress = 0.01;
        private double progressKeyRate = 0.5;
        private double progressEntryptRate = 0.5;

        private long startGeneratingTime;

        private SecureCharSequence password;
        private Runnable cancelRunnable;

        public GenerateThread() {
            super(null, UEntropyActivity.this);
        }

        public void setPassword(SecureCharSequence password) {
            this.password = password;
        }

        @Override
        public synchronized void start() {
            if (password == null) {
                throw new IllegalStateException("GenerateThread does not have password");
            }
            startGeneratingTime = System.currentTimeMillis();
            super.start();
            onProgress(startProgress);
        }

        public void cancel(Runnable cancelRunnable) {
            this.cancelRunnable = cancelRunnable;
        }

        private void finishGenerate(BlockchainService service) {
            if (password != null) {
                password.wipe();
                password = null;
            }
            if (service != null) {
                service.startAndRegister();
            }
            entropyCollector.stop();
        }

        @Override
        public void runWithService(BlockchainService service) {
            boolean success = false;
            final ArrayList<String> addressStrs = new ArrayList<String>();
            double progress = startProgress;
            double itemProgress = (1.0 - startProgress - saveProgress) / (double) targetCount;

            try {
                entropyCollector.start();
                if (service != null) {
                    service.stopAndUnregister();
                }

                List<Address> addressList = new ArrayList<Address>();
                for (int i = 0;
                     i < targetCount;
                     i++) {
                    if (cancelRunnable != null) {
                        finishGenerate(service);
                        runOnUiThread(cancelRunnable);
                        return;
                    }

                    XRandom xRandom = new XRandom(entropyCollector);
                    ECKey ecKey = ECKey.generateECKey(xRandom);
                    ecKey.setFromXRandom(true);
                    progress += itemProgress * progressKeyRate;
                    onProgress(progress);
                    if (cancelRunnable != null) {
                        finishGenerate(service);
                        runOnUiThread(cancelRunnable);
                        return;
                    }


                    // start encrypt
                    ecKey = PrivateKeyUtil.encrypt(ecKey, password);
                    Address address = new Address(ecKey.toAddress(), ecKey.getPubKey(),
                            PrivateKeyUtil.getEncryptedString(ecKey), ecKey.isFromXRandom());
                    addressList.add(address);
                    addressStrs.add(address.getAddress());

                    progress += itemProgress * progressEntryptRate;
                    onProgress(progress);
                }
                entropyCollector.stop();
                password.wipe();
                password = null;

                if (cancelRunnable != null) {
                    finishGenerate(service);
                    runOnUiThread(cancelRunnable);
                    return;
                }

                KeyUtil.addAddressListByDesc(service, addressList);
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            finishGenerate(service);
            if (success) {
                while (System.currentTimeMillis() - startGeneratingTime < MinGeneratingTime) {

                }
                onProgress(1);
                onSuccess(addressStrs);
            } else {
                onFailed();
            }
        }
    }

    @Override
    protected boolean shouldPresentPinCode() {
        return false;
    }
}
