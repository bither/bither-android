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

import java.io.File;

import net.bither.BitherSetting.AppMode;
import net.bither.activity.cold.ColdActivity;
import net.bither.activity.hot.HotActivity;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.BaseRunnable;
import net.bither.runnable.HandlerMessage;
import net.bither.ui.base.ColdWalletInitCheckView;
import net.bither.ui.base.DialogConfirmTask;
import net.bither.ui.base.DialogFirstRunWarning;
import net.bither.ui.base.ProgressDialog;
import net.bither.ui.base.WrapLayoutParamsForAnimator;
import net.bither.util.BroadcastUtil;
import net.bither.util.FileUtil;
import net.bither.util.LogUtil;
import net.bither.util.ServiceUtil;
import net.bither.util.SystemUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.ObjectAnimator;

public class ChooseModeActivity extends Activity {
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

    private boolean receiverRegistered = false;

    private ColdWalletInitCheckView vColdWalletInitCheck;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVersionCode();
        File oldWatchOnlyFile = FileUtil.getOldWatchOnlyCacheDir();
        if (oldWatchOnlyFile.exists()) {
            upgradeV4();
        } else {
            initActivity();
        }
    }

    private void initActivity() {
        setContentView(R.layout.activity_choose_mode);
        AppMode appMode = AppSharedPreference.getInstance().getAppMode();
        if (appMode == null) {
            BitherApplication.getBitherApplication().startBlockchainService(
                    false);
            initView();
            showFirstRunWarning();
        } else {
            if (appMode == AppMode.COLD) {
                vColdWalletInitCheck = (ColdWalletInitCheckView) findViewById(R.id
                        .v_cold_wallet_init_check);
                if (vColdWalletInitCheck.check()) {
                    gotoActivity(appMode);
                    finish();
                    return;
                } else {
                    initView();
                    configureColdWait();
                }
            } else if (appMode == AppMode.HOT) {
                BitherApplication.getBitherApplication()
                        .startBlockchainService(false);
                if (!existSpvFile()) {
                    initView();
                    configureWarmWait();
                } else {
                    gotoActivity(appMode);
                    finish();
                    return;
                }
            }
        }
    }

    private void showFirstRunWarning(){
        new DialogFirstRunWarning(this).show();
    }

    private Handler upgradeV4Handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case HandlerMessage.MSG_PREPARE:
                    if (progressDialog == null) {
                        progressDialog = new ProgressDialog(
                                ChooseModeActivity.this,
                                getString(R.string.please_wait), null);
                        progressDialog.setCancelable(false);
                    }
                    progressDialog.show();
                    break;
                case HandlerMessage.MSG_SUCCESS:
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    initActivity();
                    break;

                default:
                    break;
            }
        }

        ;
    };

    private void upgradeV4() {
        BaseRunnable upgradeV4Runnable = new BaseRunnable() {

            @Override
            public void run() {
                obtainMessage(HandlerMessage.MSG_PREPARE);
                AppSharedPreference.getInstance().clear();
                File walletFile = FileUtil.getOldWatchOnlyCacheDir();
                FileUtil.delFolder(walletFile.getAbsolutePath());
                File watchOnlyAddressSequenceFile = FileUtil
                        .getWatchOnlyAddressSequenceFile();
                if (watchOnlyAddressSequenceFile.exists()) {
                    watchOnlyAddressSequenceFile.delete();
                }
                File blockFile = FileUtil.getBlockChainFile();
                if (blockFile.exists()) {
                    blockFile.delete();
                }
                File errorFolder = FileUtil.getWatchErrorDir();
                FileUtil.delFolder(errorFolder.getAbsolutePath());
                obtainMessage(HandlerMessage.MSG_SUCCESS);

            }
        };
        upgradeV4Runnable.setHandler(upgradeV4Handler);
        new Thread(upgradeV4Runnable).start();

    }

    private static void setVersionCode() {
        AppSharedPreference appSharedPreference = AppSharedPreference
                .getInstance();
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
        vCold.setOnClickListener(coldClick);
        vWarm.setOnClickListener(warmClick);
        btnWarmExtraRetry.setOnClickListener(warmRetryClick);
    }

    private OnClickListener coldClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogConfirmTask dialog = new DialogConfirmTask(
                    ChooseModeActivity.this,
                    getStyledConfirmString(getString(R.string.choose_mode_cold_confirm)),
                    new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    modeSelected(AppMode.COLD);
                                    vColdWalletInitCheck.check();
                                    ObjectAnimator animator = ObjectAnimator
                                            .ofFloat(
                                                    new ShowHideView(
                                                            new View[]{vColdExtra},
                                                            new View[]{
                                                                    rlWarm,
                                                                    vWarmBg}
                                                    ),
                                                    "Progress", 1
                                            ).setDuration(
                                                    AnimHideDuration);
                                    animator.setInterpolator(new AccelerateDecelerateInterpolator
                                            ());
                                    vColdWalletInitCheck.prepareAnim();
                                    animator.addListener(coldClickAnimListener);
                                    animator.start();
                                }
                            });
                        }
                    }
            );
            dialog.show();
        }
    };

    private OnClickListener warmClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogConfirmTask dialog = new DialogConfirmTask(
                    ChooseModeActivity.this,
                    getStyledConfirmString(getString(R.string.choose_mode_warm_confirm)),
                    new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    modeSelected(AppMode.HOT);
                                    llWarmExtraError.setVisibility(View.GONE);
                                    llWarmExtraWaiting
                                            .setVisibility(View.VISIBLE);
                                    if (existSpvFile()) {
                                        ObjectAnimator animator = ObjectAnimator
                                                .ofFloat(
                                                        new ShowHideView(
                                                                new View[]{},
                                                                new View[]{
                                                                        vColdBg,
                                                                        rlCold}
                                                        ),
                                                        "Progress", 1
                                                )
                                                .setDuration(AnimHideDuration);
                                        animator.setInterpolator(new
                                                AccelerateDecelerateInterpolator());
                                        animator.addListener(warmClickAnimListener);
                                        animator.start();
                                    } else {
                                        ObjectAnimator animator = ObjectAnimator
                                                .ofFloat(
                                                        new ShowHideView(
                                                                new View[]{vWarmExtra},
                                                                new View[]{
                                                                        vColdBg,
                                                                        rlCold}
                                                        ),
                                                        "Progress", 1
                                                )
                                                .setDuration(AnimHideDuration);
                                        animator.setInterpolator(new
                                                AccelerateDecelerateInterpolator());
                                        animator.addListener(warmClickAnimListener);
                                        animator.start();
                                    }
                                }
                            });
                        }
                    }
            );
            dialog.show();
        }
    };

    private void modeSelected(final AppMode mode) {
        vCold.setClickable(false);
        vWarm.setClickable(false);
        AppSharedPreference.getInstance().setAppMode(mode);
        WalletUtils.initWalletList();
    }

    private AnimatorListener coldClickAnimListener = new AnimatorListener() {

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

    private AnimatorListener warmClickAnimListener = new AnimatorListener() {

        @Override
        public void onAnimationEnd(Animator animation) {
            if (vWarmExtra.getHeight() > UIUtil.getScreenHeight() / 3) {
                checkWarmDataReady();
            } else {
                Animation anim = AnimationUtils.loadAnimation(
                        ChooseModeActivity.this, R.anim.choose_mode_grow);
                anim.setDuration(AnimGrowDuration);
                anim.setAnimationListener(new ModeGrowAnimatorListener(
                        AppMode.HOT));
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
        NotificationManager notificationManager = (NotificationManager) BitherApplication.mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager
                .cancel(BitherSetting.NOTIFICATION_ID_NETWORK_ALERT);
    }

    private class ModeGrowAnimatorListener implements AnimationListener {
        private AppMode mode;

        public ModeGrowAnimatorListener(AppMode mode) {
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

    private void gotoActivity(AppMode appMode) {
        Intent intent = null;
        if (appMode == AppMode.HOT) {
            intent = new Intent(ChooseModeActivity.this, HotActivity.class);

        } else if (appMode == AppMode.COLD) {
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
        if (AppSharedPreference.getInstance().getAppMode() == AppMode.COLD) {
            coldCheck(false);
        }
    }

    @Override
    protected void onPause() {
        vCold.removeCallbacks(coldCheckRunnable);
        super.onPause();
    }

    private void coldCheck(boolean anim) {
        if (anim) {
            vColdWalletInitCheck.checkAnim();
            vCold.postDelayed(coldCheckRunnable,
                    ColdWalletInitCheckView.CheckAnimDuration);
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
                        ObjectAnimator animator = ObjectAnimator.ofFloat(
                                new ShowHideView(new View[]{},
                                        new View[]{vColdExtra}), "Progress",
                                1
                        ).setDuration(AnimHideDuration);
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

    private AnimatorListener coldCheckAnimListener = new AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            Animation anim = AnimationUtils.loadAnimation(
                    ChooseModeActivity.this, R.anim.choose_mode_grow);
            anim.setDuration(AnimGrowDuration);
            anim.setAnimationListener(new ModeGrowAnimatorListener(AppMode.COLD));
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
        receiverRegistered = true;
        registerReceiver(warmDataReadyReceiver, new IntentFilter(
                BroadcastUtil.ACTION_DOWLOAD_SPV_BLOCK));
    }

    @Override
    protected void onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(warmDataReadyReceiver);
            receiverRegistered = false;
        }
        super.onDestroy();
    }

    private OnClickListener warmRetryClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            llWarmExtraError.setVisibility(View.GONE);
            llWarmExtraWaiting.setVisibility(View.VISIBLE);
            ServiceUtil.dowloadSpvBlock();
        }
    };

    private BroadcastReceiver warmDataReadyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d("broadcase", intent.getAction());
            boolean completed = intent.getBooleanExtra(
                    BroadcastUtil.ACTION_DOWLOAD_SPV_BLOCK_STATE, false);
            BroadcastUtil.removeBroadcastGetSpvBlockCompelte();
            if (existSpvFile() && completed) {
                llWarmExtraError.setVisibility(View.GONE);
                llWarmExtraWaiting.setVisibility(View.VISIBLE);
                vWarm.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        ObjectAnimator animator = ObjectAnimator.ofFloat(
                                new ShowHideView(new View[]{},
                                        new View[]{vWarmExtra}), "Progress",
                                1
                        ).setDuration(AnimHideDuration);
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

    private boolean existSpvFile() {
        File blockChainFile = FileUtil.getBlockChainFile();
        return blockChainFile.exists();

    }

    private void configureWarmWait() {
        vCold.setClickable(false);
        vWarm.setClickable(false);
        new WrapLayoutParamsForAnimator(vWarmExtra).setLayoutWeight(1);
        new WrapLayoutParamsForAnimator(rlCold).setLayoutWeight(0);
        new WrapLayoutParamsForAnimator(vColdBg).setLayoutWeight(0);
        checkWarmDataReady();
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
        spn.setSpan(new ForegroundColorSpan(getResources()
                        .getColor(R.color.red)), 0, firstLineEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        spn.setSpan(new StyleSpan(Typeface.BOLD), 0, firstLineEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spn;
    }
}
