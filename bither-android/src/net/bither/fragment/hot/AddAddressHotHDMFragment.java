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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.hot.AddHotAddressActivity;
import net.bither.bitherj.api.http.Http400Exception;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.HDMBId;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.hd.DeterministicKey;
import net.bither.bitherj.crypto.hd.HDKeyDerivation;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.ScanActivity;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.HDMTriangleBgView;
import net.bither.ui.base.WrapLayoutParamsForAnimator;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogHDMInfo;
import net.bither.ui.base.dialog.DialogHDMServerUnsignedQRCode;
import net.bither.ui.base.dialog.DialogHdmKeychainAddHot;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.util.ExceptionUtil;
import net.bither.util.KeyUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;
import net.bither.xrandom.HDMKeychainHotUEntropyActivity;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by songchenwen on 15/1/9.
 */
public class AddAddressHotHDMFragment extends Fragment implements AddHotAddressActivity
        .AddAddress, DialogPassword.PasswordGetter.PasswordGetterDelegate {
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
    private DialogPassword.PasswordGetter passwordGetter;
    private DialogProgress dp;

    private boolean isServerClicked = false;

    private HDMBId hdmBid;

    private byte[] coldRoot;

    private boolean hdmKeychainLimit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_address_hot_hdm, container, false);
        initView(v);
        hdmKeychainLimit = WalletUtils.isHDMKeychainLimit();
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
        v.findViewById(R.id.ibtn_info).setOnClickListener(DialogHDMInfo.ShowClick);
        ViewGroup.LayoutParams lpContainer = flContainer.getLayoutParams();
        lpContainer.width = UIUtil.getScreenWidth();
        lpContainer.height = lpContainer.width - flContainer.getPaddingLeft() - flContainer
                .getPaddingRight() + flContainer.getPaddingTop() + flContainer.getPaddingBottom();
        flParent.getLayoutParams().width = lpContainer.width;
        flParent.getLayoutParams().height = lpContainer.height;

        llHot.setOnClickListener(hotClick);
        llCold.setOnClickListener(coldClick);
        llServer.setOnClickListener(serverClick);
        dp = new DialogProgress(getActivity(), R.string.please_wait);
        dp.setCancelable(false);
        passwordGetter = new DialogPassword.PasswordGetter(getActivity(), this);
    }

    private View.OnClickListener hotClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (hdmKeychainLimit) {
                return;
            }
            new DialogHdmKeychainAddHot(getActivity(), new DialogHdmKeychainAddHot
                    .DialogHdmKeychainAddHotDelegate() {

                @Override
                public void addWithXRandom() {
                    HDMKeychainHotUEntropyActivity.passwordGetter = passwordGetter;
                    startActivityForResult(new Intent(getActivity(),
                            HDMKeychainHotUEntropyActivity.class), XRandomRequestCode);
                }

                @Override
                public void addWithoutXRandom() {
                    new Thread() {
                        @Override
                        public void run() {
                            SecureCharSequence password = passwordGetter.getPassword();
                            if (password == null) {
                                return;
                            }
                            HDMKeychain keychain = new HDMKeychain(new SecureRandom(), password);
                            KeyUtil.setHDKeyChain(keychain, password);
                            ThreadUtil.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (dp.isShowing()) {
                                        dp.dismiss();
                                    }
                                    moveToCold(true);
                                }
                            });
                        }
                    }.start();
                }
            }).show();
        }
    };

    private View.OnClickListener coldClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (hdmKeychainLimit) {
                return;
            }
            new DialogConfirmTask(getActivity(), getString(R.string.hdm_keychain_add_scan_cold),
                    new Runnable() {

                        @Override
                        public void run() {
                            ThreadUtil.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(getActivity(), ScanActivity.class);
                                    startActivityForResult(intent, ScanColdRequestCode);
                                }
                            });
                        }
                    }).show();
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (XRandomRequestCode == requestCode && resultCode == Activity.RESULT_OK &&
                AddressManager.getInstance().getHdmKeychain() != null) {
            llCold.postDelayed(new Runnable() {
                @Override
                public void run() {
                    moveToCold(true);
                }
            }, 500);
        }
        if (ScanColdRequestCode == requestCode && resultCode == Activity.RESULT_OK) {
            String result = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            try {
                coldRoot = Utils.hexStringToByteArray(result);
                final int count = BitherSetting.HDM_ADDRESS_PER_SEED_PREPARE_COUNT -
                        AddressManager.getInstance().getHdmKeychain().uncompletedAddressCount();
                if (!dp.isShowing() && passwordGetter.hasPassword() && count > 0) {
                    dp.show();
                }
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            if (count > 0) {
                                SecureCharSequence password = passwordGetter.getPassword();
                                if (password == null) {
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (dp.isShowing()) {
                                                dp.dismiss();
                                            }
                                        }
                                    });
                                    return;
                                }
                                AddressManager.getInstance().getHdmKeychain().prepareAddresses
                                        (count, password, Arrays.copyOf(coldRoot, coldRoot.length));
                            }
                            initHDMBidFromColdRoot();
                            ThreadUtil.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (dp.isShowing()) {
                                        dp.dismiss();
                                    }
                                    if (isServerClicked) {
                                        serverClick.onClick(llServer);
                                    } else {
                                        moveToServer(true);
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            coldRoot = null;
                            ThreadUtil.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (dp.isShowing()) {
                                        dp.dismiss();
                                    }
                                    DropdownMessage.showDropdownMessage(getActivity(),
                                            R.string.hdm_keychain_add_scan_cold);
                                }
                            });
                        }
                    }
                }.start();
            } catch (Exception e) {
                e.printStackTrace();
                coldRoot = null;
                DropdownMessage.showDropdownMessage(getActivity(),
                        R.string.hdm_keychain_add_scan_cold);
            }
        }
        if (ServerQRCodeRequestCode == requestCode && resultCode == Activity.RESULT_OK) {
            final String result = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            if (hdmBid == null) {
                return;
            }
            if (!dp.isShowing()) {
                dp.show();
            }
            final DialogProgress dd = dp;
            new ThreadNeedService(null, getActivity()) {
                @Override
                public void runWithService(BlockchainService service) {
                    try {
                        SecureCharSequence password = passwordGetter.getPassword();
                        if(password == null){
                            return;
                        }
                        hdmBid.setSignature(result, password);
                        if (service != null) {
                            service.stopAndUnregister();
                        }
                        final HDMKeychain keychain = AddressManager.getInstance().getHdmKeychain();
                        final List<HDMAddress> as = keychain
                                .completeAddresses(1, password,
                                        new HDMKeychain.HDMFetchRemotePublicKeys() {
                                            @Override
                                            public void completeRemotePublicKeys(CharSequence
                                                                                         password, List<HDMAddress.Pubs> partialPubs) {
                                                try {
                                                    HDMKeychain.getRemotePublicKeys(hdmBid, password, partialPubs);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    int msg = R.string.network_or_connection_error;
                                                    if (e instanceof Http400Exception) {
                                                        msg = ExceptionUtil
                                                                .getHDMHttpExceptionMessage((
                                                                        (Http400Exception) e)
                                                                        .getErrorCode());
                                                    }
                                                    final int m = msg;
                                                    ThreadUtil.runOnMainThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (dp != null && dp.isShowing()) {
                                                                dp.dismiss();
                                                            }
                                                            DropdownMessage.showDropdownMessage(getActivity(), m);
                                                        }
                                                    });
                                                }
                                            }
                                        });

                        if (service != null) {
                            service.startAndRegister();
                        }
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dd.isShowing()) {
                                    dd.dismiss();
                                    if (dp != null && dp.isShowing()) {
                                        dp.dismiss();
                                    }
                                    if (as.size() > 0) {
                                        moveToFinal(true);
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dd.isShowing()) {
                                    dd.dismiss();
                                }
                                DropdownMessage.showDropdownMessage(getActivity(),
                                        R.string.hdm_keychain_add_sign_server_qr_code_error);
                            }
                        });
                    }
                }
            }.start();
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
            
            if (hdmKeychainLimit) {
                return;
            }
            if (coldRoot == null && hdmBid == null) {
                isServerClicked = true;
                coldClick.onClick(llCold);
                return;
            }
            if (dp == null) {
                dp = new DialogProgress(getActivity(), R.string.please_wait);
            }
            if (!dp.isShowing()) {
                dp.show();
            }
            new Thread() {
                @Override
                public void run() {
                    try {
                        initHDMBidFromColdRoot();
                        final String preSign = hdmBid.getPreSignString();
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dp.isShowing()) {
                                    dp.dismiss();
                                }
                                new DialogHDMServerUnsignedQRCode(getActivity(), preSign,
                                        new DialogHDMServerUnsignedQRCode
                                                .DialogHDMServerUnsignedQRCodeListener() {
                                            @Override
                                            public void scanSignedHDMServerQRCode() {
                                                startActivityForResult(new Intent(getActivity(),
                                                        ScanActivity.class), ServerQRCodeRequestCode);
                                            }
                                        }).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        int msg = R.string.network_or_connection_error;
                        if (e instanceof Http400Exception) {
                            msg = ExceptionUtil.getHDMHttpExceptionMessage(((Http400Exception) e)
                                    .getErrorCode());
                        }
                        final int m = msg;
                        if (dp.isShowing()) {
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(getActivity(), m);
                    }
                }
            }.start();
        }
    };

    private void findCurrentStep() {
        moveToHot(false);
        if (AddressManager.getInstance().getHdmKeychain() != null) {
            moveToCold(false);
            if (AddressManager.getInstance().getHdmKeychain().uncompletedAddressCount() > 0) {
                moveToServer(false);
                if (hdmKeychainLimit) {
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

    private void moveToCold(boolean anim) {
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
                }
            });
        }
    }

    private void moveToServer(boolean anim) {
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
                }
            });
        }
    }

    private void moveToFinal(boolean animToFinish) {
        hdmKeychainLimit = WalletUtils.isHDMKeychainLimit();
        llHot.setEnabled(false);
        llHot.setSelected(true);
        llCold.setEnabled(false);
        llCold.setSelected(true);
        llServer.setEnabled(false);
        llServer.setSelected(true);
        stopAllFlash();
        if (!animToFinish) {
            vBg.addLine(llServer, llHot);
            if (hdmKeychainLimit) {
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

    private void initHDMBidFromColdRoot() {
        if (hdmBid == null) {
            if (hdmBid != null) {
                return;
            }
            DeterministicKey root = HDKeyDerivation.createMasterPubKeyFromExtendedBytes(Arrays.copyOf
                    (coldRoot, coldRoot.length));
            DeterministicKey key = root.deriveSoftened(0);
            String address = Utils.toAddress(key.getPubKeyHash());
            root.wipe();
            key.wipe();
            hdmBid = new HDMBId(address);
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
                        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.hdm_keychain_add_spin);
                        anim.setFillAfter(true);
                        flContainer.startAnimation(anim);
                        flContainer.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ((AddHotAddressActivity) getActivity()).save();
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

    @Override
    public void onDestroyView() {
        if (passwordGetter != null) {
            passwordGetter.wipe();
        }
        if (coldRoot != null) {
            Utils.wipeBytes(coldRoot);
        }
        super.onDestroyView();
    }


    @Override
    public void beforePasswordDialogShow() {
        if (dp != null && dp.isShowing()) {
            dp.dismiss();
        }
    }

    @Override
    public void afterPasswordDialogDismiss() {
        if (dp != null && !dp.isShowing()) {
            dp.show();
        }
    }
}
