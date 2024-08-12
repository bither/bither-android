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

package net.bither;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import net.bither.activity.cold.ColdActivity;
import net.bither.activity.hot.HotActivity;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.AddressManager;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.HandlerMessage;
import net.bither.service.BlockchainService;
import net.bither.ui.base.BaseActivity;
import net.bither.ui.base.ColdWalletInitCheckView;
import net.bither.ui.base.RelativeLineHeightSpan;
import net.bither.ui.base.WrapLayoutParamsForAnimator;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogUpgrade;
import net.bither.util.AdUtil;
import net.bither.util.BroadcastUtil;
import net.bither.util.ImageFileUtil;
import net.bither.util.LogUtil;
import net.bither.util.SystemUtil;
import net.bither.util.UIUtil;
import net.bither.util.UpgradeUtil;
import net.bither.xrandom.URandom;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class ChooseModeActivity extends BaseActivity {
    private static final int AnimHideDuration = 600;
    private static final int AnimGrowDuration = 500;
    private static final int ColdCheckInterval = 700;

    private View vColdBg;
    private View vWarmBg;
    private View vCold;
    private View vWarm;
    private View rlCold;
    private View rlWarm;
    private View vWarmExtra;
    private View vColdExtra;
    private View llWarmExtraWaiting;
    private View llWarmExtraError;
    private View btnWarmExtraRetry;
    private Button btnChangeToCold;

    private boolean receiverRegistered = false;

    private ColdWalletInitCheckView vColdWalletInitCheck;
    private DialogUpgrade dialogUpgrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppSharedPreference.getInstance().getAppMode() == null) {
            AppSharedPreference.getInstance().setAppMode(BitherjSettings.AppMode.HOT);
        }
        if (URandom.urandomFile.exists()) {
            if (UpgradeUtil.needUpgrade()) {
                upgrade();
            } else {
                setVersionCode();
                initActivity();
            }
        } else {
            DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(ChooseModeActivity.this, getString(R.string.urandom_not_exists), new Runnable() {
                @Override
                public void run() {
                    finish();

                }
            }, new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
            dialogConfirmTask.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            dialogConfirmTask.setCancelable(false);
            dialogConfirmTask.show();
        }
    }

    private boolean isShowAd() {
        JSONObject cacheAdJsonObject = AdUtil.getCacheAdJSON();
        if (cacheAdJsonObject != null) {
            File imageFile = ImageFileUtil.getAdImageFolder(getString(R.string.ad_image_name));
            if (imageFile.exists()) {
                File files[] = imageFile.listFiles();
                if (files != null && files.length > 0) {
                    try {
                        if (!cacheAdJsonObject.getString("timestamp").equalsIgnoreCase("0")) {
                            return true;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private void downloadAd() {
        AdUtil adUtil = new AdUtil();
        adUtil.getAd();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 20) {
            initActivity();
        }
    }

    private void upgrade() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (AppSharedPreference.getInstance().getVerionCode() < UpgradeUtil.BITHERJ_VERSION_CODE) {
                    AppSharedPreference.getInstance().setDownloadSpvFinish(false);
                }
                UpgradeUtil.upgradeNewVerion(upgradeHandler);
            }
        }).start();


    }

    private Handler upgradeHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case HandlerMessage.MSG_PREPARE:
                    dialogUpgrade = new DialogUpgrade(ChooseModeActivity.this);
                    dialogUpgrade.setMessage(getUpgradeString());
                    dialogUpgrade.setCancelable(false);
                    dialogUpgrade.show();
                    break;
                case HandlerMessage.MSG_SUCCESS:
                    if (dialogUpgrade != null) {
                        dialogUpgrade.dismiss();
                    }
                    setVersionCode();
                    initActivity();
                    if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.HOT) {
                        dowloadSpvBlock();
                    }
                    break;
                case HandlerMessage.MSG_FAILURE:
                    if (dialogUpgrade != null) {
                        dialogUpgrade.dismiss();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void initActivity() {
        setContentView(R.layout.activity_choose_mode);
        BitherjSettings.AppMode appMode = AppSharedPreference.getInstance().getAppMode();
        if (appMode == null) {
            BitherApplication.startBlockchainService();
            dowloadSpvBlock();
            initView();
        } else {
            if (appMode == BitherjSettings.AppMode.COLD) {
                vColdWalletInitCheck = (ColdWalletInitCheckView) findViewById(R.id
                        .v_cold_wallet_init_check);
                if (vColdWalletInitCheck.check()) {
                    gotoActivity(appMode);
                    finish();
                } else {
                    initView();
                    configureColdWait();
                }
            } else if (appMode == BitherjSettings.AppMode.HOT) {
                BitherApplication.startBlockchainService();
                if (!AppSharedPreference.getInstance().getDownloadSpvFinish()) {
                    initView();
                    checkWarmDataReady();
                    dowloadSpvBlock();
                    configureWarmWait();
                } else {
                    gotoActivity(appMode);
                    finish();
                }
            }
        }
    }


    private static void setVersionCode() {
        AppSharedPreference appSharedPreference = AppSharedPreference.getInstance();
        int lastVersionCode = appSharedPreference.getVerionCode();
        BitherApplication.isFirstIn = lastVersionCode == 0;
        int versionCode = SystemUtil.getAppVersionCode();
        if (versionCode > lastVersionCode) {
            appSharedPreference.setVerionCode(versionCode);
        }
    }

    private void initView() {
        vColdBg = findViewById(R.id.v_cold_bg);
        vWarmBg = findViewById(R.id.v_warm_bg);
        vCold = findViewById(R.id.v_cold);
        vWarm = findViewById(R.id.v_warm);
        rlCold = findViewById(R.id.rl_cold);
        rlWarm = findViewById(R.id.rl_warm);
        vColdExtra = findViewById(R.id.v_cold_extra);
        vWarmExtra = findViewById(R.id.v_warm_extra);
        vColdWalletInitCheck = (ColdWalletInitCheckView) findViewById(R.id
                .v_cold_wallet_init_check);
        llWarmExtraWaiting = findViewById(R.id.ll_warm_extra_waiting);
        llWarmExtraError = findViewById(R.id.ll_warm_extra_error);
        btnWarmExtraRetry = findViewById(R.id.btn_warm_extra_retry);
        btnWarmExtraRetry.setOnClickListener(warmRetryClick);
        btnChangeToCold = (Button) findViewById(R.id.btn_change_to_cold);
        btnChangeToCold.setOnClickListener(changeToColdClick);
        if (AddressManager.getInstance().getAllAddresses().size() > 0
                || AddressManager.getInstance().hasHDMKeychain()) {
            btnChangeToCold.setVisibility(View.GONE);
        }
    }

    private OnClickListener changeToColdClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            DialogConfirmTask dialog = new DialogConfirmTask(ChooseModeActivity.this,
                    getStyledConfirmString(getString(R.string
                            .launch_sequence_switch_to_cold_warn)),
                    new Runnable() {
                        @Override
                        public void run() {
                            BlockchainService.getInstance().onStop();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    modeSelected(BitherjSettings.AppMode.COLD);
                                    llWarmExtraError.setVisibility(View.GONE);
                                    vColdWalletInitCheck.check();
                                    ObjectAnimator animator = ObjectAnimator.ofFloat(new ShowHideView(new
                                                    View[]{vColdExtra, vColdBg, rlCold}, new View[]{rlWarm, vWarmBg, vWarmExtra}), "Progress",
                                            1).setDuration(AnimHideDuration);
                                    animator.setInterpolator(new AccelerateDecelerateInterpolator());
                                    vColdWalletInitCheck.prepareAnim();
                                    animator.addListener(coldClickAnimListener);
                                    animator.start();
                                }
                            });
                        }
                    });
            dialog.show();
        }


        private SpannableString getStyledConfirmString(String str) {
            int firstLineEnd = str.indexOf("\n");
            SpannableString spn = new SpannableString(str);
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.red)), 0,
                    firstLineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spn.setSpan(new StyleSpan(Typeface.BOLD), 0, firstLineEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spn.setSpan(new RelativeSizeSpan(0.8f), firstLineEnd, str.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spn;
        }
    };

    private OnClickListener coldClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogConfirmTask dialog = new DialogConfirmTask(ChooseModeActivity.this,
                    getStyledConfirmString(getString(R.string.choose_mode_cold_confirm)),
                    new Runnable() {
                        @Override
                        public void run() {
                            BlockchainService.getInstance().onStop();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    modeSelected(BitherjSettings.AppMode.COLD);
                                    vColdWalletInitCheck.check();
                                    ObjectAnimator animator = ObjectAnimator.ofFloat(new ShowHideView(new
                                                    View[]{vColdExtra}, new View[]{rlWarm, vWarmBg}), "Progress",
                                            1).setDuration(AnimHideDuration);
                                    animator.setInterpolator(new AccelerateDecelerateInterpolator());
                                    vColdWalletInitCheck.prepareAnim();
                                    animator.addListener(coldClickAnimListener);
                                    animator.start();
                                }
                            });
                        }
                    });
            dialog.show();
        }
    };

    private OnClickListener warmClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogConfirmTask dialog = new DialogConfirmTask(ChooseModeActivity.this,
                    getStyledConfirmString(getString(R.string.choose_mode_warm_confirm)),
                    new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    modeSelected(BitherjSettings.AppMode.HOT);
                                    llWarmExtraError.setVisibility(View.GONE);
                                    llWarmExtraWaiting.setVisibility(View.VISIBLE);
                                    if (AppSharedPreference.getInstance().getDownloadSpvFinish()) {
                                        ObjectAnimator animator = ObjectAnimator.ofFloat(new ShowHideView
                                                        (new View[]{}, new View[]{vColdBg, rlCold}), "Progress",
                                                1).setDuration(AnimHideDuration);
                                        animator.setInterpolator(new AccelerateDecelerateInterpolator());
                                        animator.addListener(warmClickAnimListener);
                                        animator.start();
                                    } else {
                                        ObjectAnimator animator = ObjectAnimator.ofFloat(new ShowHideView
                                                        (new View[]{vWarmExtra}, new View[]{vColdBg, rlCold}),
                                                "Progress", 1).setDuration(AnimHideDuration);
                                        animator.setInterpolator(new AccelerateDecelerateInterpolator());
                                        animator.addListener(warmClickAnimListener);
                                        animator.start();
                                    }
                                }
                            });
                        }
                    });
            dialog.show();
        }
    };

    private void modeSelected(final BitherjSettings.AppMode mode) {
        vCold.setClickable(false);
        vWarm.setClickable(false);
        AppSharedPreference.getInstance().setAppMode(mode);

    }

    private Animator.AnimatorListener coldClickAnimListener = new Animator.AnimatorListener() {

        @Override
        public void onAnimationEnd(Animator animation) {
            coldCheck(true);
        }

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }
    };

    private Animator.AnimatorListener warmClickAnimListener = new Animator.AnimatorListener() {

        @Override
        public void onAnimationEnd(Animator animation) {
            if (vWarmExtra.getHeight() > UIUtil.getScreenHeight() / 3) {
                checkWarmDataReady();
            } else {
                Animation anim = AnimationUtils.loadAnimation(ChooseModeActivity.this,
                        R.anim.choose_mode_grow);
                anim.setDuration(AnimGrowDuration);
                anim.setAnimationListener(new ModeGrowAnimatorListener(BitherjSettings.AppMode
                        .HOT));
                vWarm.startAnimation(anim);
            }
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

    private void removeNetworkNotification() {
        NotificationManager notificationManager = (NotificationManager) BitherApplication
                .mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(BitherSetting.NOTIFICATION_ID_NETWORK_ALERT);
    }

    private class ModeGrowAnimatorListener implements AnimationListener {
        private BitherjSettings.AppMode mode;

        public ModeGrowAnimatorListener(BitherjSettings.AppMode mode) {
            this.mode = mode;
        }

        @Override
        public void onAnimationStart(Animation animation) {
            removeNetworkNotification();
            gotoActivity(mode);
            overridePendingTransition(0, R.anim.choose_mode_activity_exit);
            finish();
        }

        @Override
        public void onAnimationEnd(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    private void gotoActivity(BitherjSettings.AppMode appMode) {
        Intent intent = null;
        if (appMode == BitherjSettings.AppMode.HOT) {
            intent = new Intent(ChooseModeActivity.this, HotActivity.class);

        } else if (appMode == BitherjSettings.AppMode.COLD) {
            intent = new Intent(ChooseModeActivity.this, ColdActivity.class);

        }
        startActivity(intent);
    }

    private static class ShowHideView {
        private WrapLayoutParamsForAnimator show[];
        private WrapLayoutParamsForAnimator hide[];

        private float progress = 0;

        public ShowHideView(View[] show, View[] hide) {
            this.show = new WrapLayoutParamsForAnimator[show.length];
            this.hide = new WrapLayoutParamsForAnimator[hide.length];
            for (int i = 0;
                 i < show.length;
                 i++) {
                this.show[i] = new WrapLayoutParamsForAnimator(show[i]);
            }
            for (int i = 0;
                 i < hide.length;
                 i++) {
                this.hide[i] = new WrapLayoutParamsForAnimator(hide[i]);
            }
        }

        public void setProgress(float progress) {
            this.progress = progress;
            for (WrapLayoutParamsForAnimator s : show) {
                s.setLayoutWeight(progress);
            }
            progress = 1.0f - progress;
            for (WrapLayoutParamsForAnimator h : hide) {
                h.setLayoutWeight(progress);
            }
        }

        public float getProgress() {
            return progress;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vCold != null && AppSharedPreference.getInstance().getAppMode() == BitherjSettings
                .AppMode.COLD) {
            coldCheck(false);
        }
    }

    @Override
    protected void onPause() {
        if (vCold != null) {
            vCold.removeCallbacks(coldCheckRunnable);
        }
        super.onPause();
    }

    private void coldCheck(boolean anim) {
        if (anim) {
            vColdWalletInitCheck.checkAnim();
            vCold.postDelayed(coldCheckRunnable, ColdWalletInitCheckView.CheckAnimDuration);
        } else {
            coldCheckRunnable.run();
        }
    }

    private Runnable coldCheckRunnable = new Runnable() {

        @Override
        public void run() {
            if (vColdWalletInitCheck.check()) {
                vCold.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ObjectAnimator animator = ObjectAnimator.ofFloat(new ShowHideView(new
                                        View[]{}, new View[]{vColdExtra}), "Progress",
                                1).setDuration(AnimHideDuration);
                        animator.setInterpolator(new AccelerateDecelerateInterpolator());
                        animator.addListener(coldCheckAnimListener);
                        animator.start();
                    }
                }, ColdCheckInterval);
                return;
            }
            vCold.postDelayed(coldCheckRunnable, ColdCheckInterval);
        }
    };

    private Animator.AnimatorListener coldCheckAnimListener = new Animator.AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            Animation anim = AnimationUtils.loadAnimation(ChooseModeActivity.this,
                    R.anim.choose_mode_grow);
            anim.setDuration(AnimGrowDuration);
            anim.setAnimationListener(new ModeGrowAnimatorListener(BitherjSettings.AppMode.COLD));
            vCold.startAnimation(anim);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    };

    private void checkWarmDataReady() {
        if (receiverRegistered) {
            return;
        }
        receiverRegistered = true;
        registerReceiver(warmDataReadyReceiver, new IntentFilter(BroadcastUtil.ACTION_DOWLOAD_SPV_BLOCK));
    }

    @Override
    protected void onDestroy() {
        unregisterWarmDataReadyReceiver();
        super.onDestroy();
    }

    private void unregisterWarmDataReadyReceiver() {
        if (receiverRegistered) {
            unregisterReceiver(warmDataReadyReceiver);
            receiverRegistered = false;
        }
    }

    private final OnClickListener warmRetryClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            llWarmExtraError.setVisibility(View.GONE);
            llWarmExtraWaiting.setVisibility(View.VISIBLE);
            checkWarmDataReady();
            dowloadSpvBlock();
        }
    };

    private final BroadcastReceiver warmDataReadyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d("broadcase", intent.getAction());
            boolean completed = intent.getBooleanExtra(BroadcastUtil.ACTION_DOWLOAD_SPV_BLOCK_STATE, false);
            unregisterWarmDataReadyReceiver();
            if (AppSharedPreference.getInstance().getDownloadSpvFinish() && completed) {
                llWarmExtraError.setVisibility(View.GONE);
                llWarmExtraWaiting.setVisibility(View.VISIBLE);
                vWarm.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        ObjectAnimator animator = ObjectAnimator.ofFloat(new ShowHideView(new
                                        View[]{}, new View[]{vWarmExtra}), "Progress",
                                1).setDuration(AnimHideDuration);
                        animator.setInterpolator(new AccelerateDecelerateInterpolator());
                        animator.addListener(warmClickAnimListener);
                        animator.start();
                    }
                }, 200);
            } else {
                llWarmExtraError.setVisibility(View.VISIBLE);
                llWarmExtraWaiting.setVisibility(View.GONE);
            }
        }
    };

    private void configureWarmWait() {
        vCold.setClickable(false);
        vWarm.setClickable(false);
        new WrapLayoutParamsForAnimator(vWarmExtra).setLayoutWeight(1);
        new WrapLayoutParamsForAnimator(rlCold).setLayoutWeight(0);
        new WrapLayoutParamsForAnimator(vColdBg).setLayoutWeight(0);
    }

    private void configureColdWait() {
        vCold.setClickable(false);
        vWarm.setClickable(false);
        llWarmExtraError.setVisibility(View.GONE);
        llWarmExtraWaiting.setVisibility(View.VISIBLE);
        new WrapLayoutParamsForAnimator(vColdExtra).setLayoutWeight(1);
        new WrapLayoutParamsForAnimator(rlWarm).setLayoutWeight(0);
        new WrapLayoutParamsForAnimator(vWarmBg).setLayoutWeight(0);
        coldCheck(false);
    }

    private SpannableString getStyledConfirmString(String str) {
        int firstLineEnd = str.indexOf("\n");
        SpannableString spn = new SpannableString(str);
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.red)), 0,
                firstLineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spn.setSpan(new StyleSpan(Typeface.BOLD), 0, firstLineEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spn;
    }

    private SpannableString getUpgradeString() {
        String str = getString(R.string.begin_upgrade);
        int firstLineEnd = str.indexOf("\n");
        SpannableString spn = new SpannableString(str);
        spn.setSpan(new RelativeSizeSpan(0.9f), firstLineEnd + 1, str.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spn.setSpan(new RelativeLineHeightSpan(0.4f), firstLineEnd + 1, str.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spn;
    }

    private void dowloadSpvBlock() {
        BlockchainService.getInstance().downloadSpvBlock();
    }

    @Override
    protected boolean shouldPresentPinCode() {
        return false;
    }

    @Override
    protected boolean shouldPinCodeCheckBackground() {
        return false;
    }
}
