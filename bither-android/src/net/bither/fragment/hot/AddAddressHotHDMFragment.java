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

package net.bither.fragment.hot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

import net.bither.R;
import net.bither.activity.hot.AddHotAddressActivity;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.delegate.HDMSingular;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.AddPrivateKeyActivity;
import net.bither.ui.base.DialogFragmentHDMSingularColdSeed;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.HDMTriangleBgView;
import net.bither.ui.base.WrapLayoutParamsForAnimator;
import net.bither.ui.base.dialog.DialogHDMInfo;
import net.bither.ui.base.dialog.DialogWithArrow;
import net.bither.util.HDMHotAddAndroid;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;
import net.bither.xrandom.HDMKeychainHotUEntropyActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/1/9.
 */
public class AddAddressHotHDMFragment extends Fragment implements AddHotAddressActivity
        .AddAddress, HDMHotAddAndroid.IHDMHotAddDelegate, HDMSingular.HDMSingularDelegate, DialogFragmentHDMSingularColdSeed.DialogFragmentHDMSingularColdSeedListener {
    private static final int XRandomRequestCode = 1552;
    private static final int ScanColdRequestCode = 1623;
    private static final int ServerQRCodeRequestCode = 1135;

    private FrameLayout flParent;
    private FrameLayout flContainer;
    private HDMTriangleBgView vBg;
    private LinearLayout llHot;
    private LinearLayout llCold;
    private LinearLayout llServer;
    private ImageView ivHotLight;
    private ImageView ivColdLight;
    private ImageView ivServerLight;
    private TextView tvHot;
    private TextView tvCold;
    private TextView tvServer;
    private LinearLayout llSingular;
    private CheckBox cbxSingular;
    private View llSingularRunning;
    private HDMHotAddAndroid hdmHotAddWithAndroid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_address_hot_hdm, container, false);
        initView(v);
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                findCurrentStep();
            }
        }, 100);
        return v;
    }

    private void initView(View v) {
        flParent = (FrameLayout) v.findViewById(R.id.fl_parent);
        flContainer = (FrameLayout) v.findViewById(R.id.fl_container);
        vBg = (HDMTriangleBgView) v.findViewById(R.id.v_bg);
        llHot = (LinearLayout) v.findViewById(R.id.ll_hot);
        llCold = (LinearLayout) v.findViewById(R.id.ll_cold);
        llServer = (LinearLayout) v.findViewById(R.id.ll_server);
        ivHotLight = (ImageView) v.findViewById(R.id.iv_hot_light);
        ivColdLight = (ImageView) v.findViewById(R.id.iv_cold_light);
        ivServerLight = (ImageView) v.findViewById(R.id.iv_server_light);
        tvHot = (TextView) v.findViewById(R.id.tv_hot);
        tvCold = (TextView) v.findViewById(R.id.tv_cold);
        tvServer = (TextView) v.findViewById(R.id.tv_server);
        llSingular = (LinearLayout) v.findViewById(R.id.ll_singular);
        cbxSingular = (CheckBox) v.findViewById(R.id.cbx_singular);
        llSingularRunning = v.findViewById(R.id.ll_singular_running);
        v.findViewById(R.id.ibtn_info).setOnClickListener(DialogHDMInfo.ShowClick);
        v.findViewById(R.id.ibtn_singular_info).setOnClickListener(singularInfoClick);
        ViewGroup.LayoutParams lpContainer = flContainer.getLayoutParams();
        lpContainer.width = UIUtil.getScreenWidth();
        lpContainer.height = lpContainer.width - flContainer.getPaddingLeft() - flContainer
                .getPaddingRight() + flContainer.getPaddingTop() + flContainer.getPaddingBottom();
        flParent.getLayoutParams().width = lpContainer.width;
        flParent.getLayoutParams().height = lpContainer.height;

        llHot.setOnClickListener(hotClick);
        llCold.setOnClickListener(coldClick);
        llServer.setOnClickListener(serverClick);
        hdmHotAddWithAndroid = new HDMHotAddAndroid(getActivity(), this, this);

    }

    private View.OnClickListener hotClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            hdmHotAddWithAndroid.hotClick();

        }
    };

    private View.OnClickListener coldClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hdmHotAddWithAndroid.coldClick();
        }

    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (XRandomRequestCode == requestCode && resultCode == Activity.RESULT_OK) {
            hdmHotAddWithAndroid.xrandomResult();
        }
        if (ScanColdRequestCode == requestCode && resultCode == Activity.RESULT_OK) {
            String result = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            hdmHotAddWithAndroid.scanColdResult(result);

        }
        if (ServerQRCodeRequestCode == requestCode && resultCode == Activity.RESULT_OK) {
            final String result = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            hdmHotAddWithAndroid.serverQRCode(result);
        }
    }

    private View.OnClickListener serverClick = new View.OnClickListener() {
        private boolean clicked = false;

        @Override
        public void onClick(View v) {
            if (clicked) {
                return;
            }
            clicked = true;
            v.postDelayed(new Runnable() {
                @Override
                public void run() {
                    clicked = false;
                }
            }, 800);
            hdmHotAddWithAndroid.serviceClick();
        }
    };

    private void findCurrentStep() {
        moveToHot(false);
        if (AddressManager.getInstance().getHdmKeychain() != null) {
            moveToCold(false);
            if (AddressManager.getInstance().getHdmKeychain().uncompletedAddressCount() > 0) {
                moveToServer(false);
                if (hdmHotAddWithAndroid.hdmKeychainLimit) {
                    moveToFinal(false);
                }
            }
        }
    }

    private void moveToHot(boolean anim) {
        llHot.setEnabled(true);
        llHot.setSelected(false);
        llCold.setEnabled(false);
        llCold.setSelected(false);
        llServer.setEnabled(false);
        llServer.setSelected(false);
        showFlash(ivHotLight);
    }

    public void moveToCold(boolean anim) {
        llHot.setEnabled(false);
        llHot.setSelected(true);
        llServer.setEnabled(false);
        llServer.setSelected(false);
        llCold.setSelected(false);
        if (!anim) {
            vBg.addLine(llHot, llCold);
            llCold.setEnabled(true);
            showFlash(ivColdLight);
        } else {
            stopAllFlash();
            vBg.addLineAnimated(llHot, llCold, new Runnable() {
                @Override
                public void run() {
                    showFlash(ivColdLight);
                    llCold.setEnabled(true);
                    if (hdmHotAddWithAndroid.singular.isInSingularMode()) {
                        hdmHotAddWithAndroid.singular.cold();
                    }
                }
            });
        }
    }

    public void moveToServer(boolean anim) {
        if (llServer.isEnabled()) {
            return;
        }
        llHot.setEnabled(false);
        llHot.setSelected(true);
        llCold.setEnabled(false);
        llCold.setSelected(true);
        llServer.setSelected(false);
        if (!anim) {
            vBg.addLine(llCold, llServer);
            llServer.setEnabled(true);
            showFlash(ivServerLight);
        } else {
            stopAllFlash();
            vBg.addLineAnimated(llCold, llServer, new Runnable() {
                @Override
                public void run() {
                    showFlash(ivServerLight);
                    llServer.setEnabled(true);
                    if (hdmHotAddWithAndroid.singular.isInSingularMode()) {
                        hdmHotAddWithAndroid.singular.server();
                    }
                }
            });
        }
    }

    public void moveToFinal(boolean animToFinish) {
        hdmHotAddWithAndroid.hdmKeychainLimit = AddressManager.isHDMKeychainLimit();
        llHot.setEnabled(false);
        llHot.setSelected(true);
        llCold.setEnabled(false);
        llCold.setSelected(true);
        llServer.setEnabled(false);
        llServer.setSelected(true);
        stopAllFlash();
        if (!animToFinish) {
            vBg.addLine(llServer, llHot);
            if (hdmHotAddWithAndroid.hdmKeychainLimit) {
                llHot.setEnabled(true);
                llCold.setEnabled(true);
                llServer.setEnabled(true);
            }
        } else {
            vBg.addLineAnimated(llServer, llHot, new Runnable() {
                @Override
                public void run() {
                    finalAnimation();
                }
            });
        }
    }

    private void stopAllFlash() {
        showFlash(null);
    }

    private void showFlash(ImageView iv) {
        ImageView[] ivs = new ImageView[]{ivHotLight, ivColdLight, ivServerLight};
        for (ImageView v : ivs) {
            if (v != iv) {
                v.clearAnimation();
                v.setVisibility(View.INVISIBLE);
            }
        }
        if (iv != null) {
            iv.setVisibility(View.VISIBLE);
            iv.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                    R.anim.hdm_keychain_add_one_part_flash));

        }
    }


    private void finalAnimation() {
        final int fadeDuration = 400;
        final int zoomDuration = 500;

        AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(fadeDuration);
        fadeOut.setFillAfter(true);
        vBg.startAnimation(fadeOut);
        tvHot.startAnimation(fadeOut);
        tvCold.startAnimation(fadeOut);
        tvServer.startAnimation(fadeOut);
        if (llSingularRunning.getVisibility() == View.VISIBLE) {
            llSingularRunning.startAnimation(fadeOut);
        }
        flContainer.postDelayed(new Runnable() {
            @Override
            public void run() {
                vBg.setVisibility(View.GONE);
                tvHot.setVisibility(View.INVISIBLE);
                tvCold.setVisibility(View.INVISIBLE);
                tvServer.setVisibility(View.INVISIBLE);

                int[] size = getCompactContainerSize();
                WrapLayoutParamsForAnimator animWrapper = new WrapLayoutParamsForAnimator
                        (flContainer);
                ObjectAnimator animatorWidth = ObjectAnimator.ofInt(animWrapper, "width",
                        size[0]).setDuration(zoomDuration);
                ObjectAnimator animatorHeight = ObjectAnimator.ofInt(animWrapper, "height",
                        size[1]).setDuration(zoomDuration);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(animatorWidth, animatorHeight);
                animatorSet.start();

                flContainer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Animation anim = AnimationUtils.loadAnimation(getActivity(),
                                R.anim.hdm_keychain_add_spin);
                        anim.setFillAfter(true);
                        flContainer.startAnimation(anim);
                        flContainer.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ((AddPrivateKeyActivity) getActivity()).save();
                            }
                        }, anim.getDuration());
                    }
                }, zoomDuration);
            }
        }, fadeDuration);
    }

    private int[] getCompactContainerSize() {
        int extraHeight = tvHot.getHeight();
        int width = llCold.getWidth() * 2;
        int height = (int) (width / 4 * Math.tan(Math.PI / 3)) + width / 2 + extraHeight * 2;
        return new int[]{width + flContainer.getPaddingLeft() + flContainer.getPaddingRight(),
                height + flContainer.getPaddingTop() + flContainer.getPaddingBottom()};
    }

    @Override
    public ArrayList<String> getAddresses() {
        List<HDMAddress> as = AddressManager.getInstance().getHdmKeychain().getAddresses();
        ArrayList<String> s = new ArrayList<String>();
        for (HDMAddress a : as) {
            s.add(a.getAddress());
        }
        return s;
    }

    private View.OnClickListener singularInfoClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogWithArrow d = new DialogWithArrow(v.getContext());
            d.setContentView(R.layout.dialog_hdm_singular_mode_info);
            d.show(v);
        }
    };

    @Override
    public void setSingularModeAvailable(boolean available) {
        if (available) {
            llSingular.setVisibility(View.VISIBLE);
        } else {
            llSingular.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSingularModeBegin() {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                llSingular.setVisibility(View.GONE);
                llSingularRunning.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean shouldGoSingularMode() {
        return cbxSingular.isChecked();
    }

    @Override
    public void singularHotFinish() {
        moveToCold(true);
    }

    @Override
    public void singularColdFinish() {
        moveToServer(true);
    }

    @Override
    public void singularServerFinish(final List<String> words, final String qr) {
        hdmHotAddWithAndroid.hdmKeychainLimit = AddressManager.isHDMKeychainLimit();
        llHot.setEnabled(false);
        llHot.setSelected(true);
        llCold.setEnabled(false);
        llCold.setSelected(true);
        llServer.setEnabled(false);
        llServer.setSelected(true);
        stopAllFlash();
        vBg.addLineAnimated(llServer, llHot, new Runnable() {
            @Override
            public void run() {
                DialogFragmentHDMSingularColdSeed.newInstance(words, qr, AddAddressHotHDMFragment
                        .this).show(getActivity().getSupportFragmentManager(),
                        DialogFragmentHDMSingularColdSeed.FragmentTag);
            }
        });
    }

    @Override
    public void HDMSingularColdSeedRemembered() {
        finalAnimation();
    }

    @Override
    public void singularShowNetworkFailure() {
        DropdownMessage.showDropdownMessage(getActivity(), R.string.network_or_connection_error);
        vBg.removeAllLines();
        findCurrentStep();
    }

    @Override
    public void callKeychainHotUEntropy() {
        startActivityForResult(new Intent(getActivity(), HDMKeychainHotUEntropyActivity.class),
                XRandomRequestCode);
    }

    @Override
    public void callServerQRCode() {

        startActivityForResult(new Intent(getActivity(), ScanActivity.class),
                ServerQRCodeRequestCode);


    }

    @Override
    public void callScanCold() {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getActivity(), ScanActivity.class);
                startActivityForResult(intent, ScanColdRequestCode);

            }
        });
    }

    public boolean canCancel() {
        if (hdmHotAddWithAndroid.singular != null) {
            return !hdmHotAddWithAndroid.singular.isInSingularMode();
        }
        return true;
    }


    @Override
    public void onDestroyView() {
        hdmHotAddWithAndroid.wipe();
        super.onDestroyView();
    }


}
