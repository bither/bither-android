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

package net.bither.fragment.hot;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import net.bither.BitherSetting;
import net.bither.R;
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
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogHDMServerUnsignedQRCode;
import net.bither.ui.base.dialog.DialogHdmKeychainAddHot;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.util.ExceptionUtil;
import net.bither.util.KeyUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.WalletUtils;
import net.bither.xrandom.HDMKeychainHotUEntropyActivity;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

public class HDMHotAddWithAndroid implements DialogPassword.PasswordGetter.PasswordGetterDelegate {

    public static final int XRandomRequestCode = 1552;
    public static final int ScanColdRequestCode = 1623;
    public static final int ServerQRCodeRequestCode = 1135;

    public interface IHDMHotAddDelegate {
        public void moveToCold(boolean anim);

        public void moveToServer(boolean anim);

        public void moveToFinal(boolean isFinal);

        public void callActivityForResult(final Intent intent, final int requestCode);

    }

    private HDMBId hdmBid;
    private byte[] coldRoot;
    private DialogPassword.PasswordGetter passwordGetter;
    private Activity activity;

    private DialogProgress dp;

    private boolean hdmKeychainLimit;
    private IHDMHotAddDelegate delegate;

    private boolean isServerClicked = false;

    public HDMHotAddWithAndroid(Activity activity, IHDMHotAddDelegate delegate) {
        this.activity = activity;
        this.delegate = delegate;
        passwordGetter = new DialogPassword.PasswordGetter(activity, this);
        dp = new DialogProgress(activity, R.string.please_wait);
        dp.setCancelable(false);
        hdmKeychainLimit = WalletUtils.isHDMKeychainLimit();
    }


    public void hotClick() {
        if (hdmKeychainLimit) {
            return;
        }
        new DialogHdmKeychainAddHot(this.activity, new DialogHdmKeychainAddHot
                .DialogHdmKeychainAddHotDelegate() {

            @Override
            public void addWithXRandom() {
                HDMKeychainHotUEntropyActivity.passwordGetter = passwordGetter;
                if (delegate != null) {
                    delegate.callActivityForResult(new Intent(activity,
                            HDMKeychainHotUEntropyActivity.class), XRandomRequestCode);
                }
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
                        KeyUtil.setHDKeyChain(keychain);
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dp.isShowing()) {
                                    dp.dismiss();
                                }
                                if (delegate != null) {
                                    delegate.moveToCold(true);
                                }
                            }
                        });
                    }
                }.start();
            }
        }).show();
    }

    public void coldClick() {
        if (hdmKeychainLimit) {
            return;
        }
        new DialogConfirmTask(activity, activity.getString(R.string.hdm_keychain_add_scan_cold),
                new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(activity, ScanActivity.class);
                        if (delegate != null) {
                            delegate.callActivityForResult(intent, ScanColdRequestCode);
                        }
                    }
                }).show();
    }

    public void serviceClick() {
        if (hdmKeychainLimit) {
            return;
        }
        if (coldRoot == null && hdmBid == null) {
            isServerClicked = true;
            //coldClick.onClick(llCold);
            coldClick();
            return;
        }
        if (dp == null) {
            dp = new DialogProgress(activity, R.string.please_wait);
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
                            new DialogHDMServerUnsignedQRCode(activity, preSign,
                                    new DialogHDMServerUnsignedQRCode
                                            .DialogHDMServerUnsignedQRCodeListener() {
                                        @Override
                                        public void scanSignedHDMServerQRCode() {
                                            if (delegate != null) {
                                                delegate.callActivityForResult(new Intent(activity,
                                                        ScanActivity.class), ServerQRCodeRequestCode);
                                            }

                                        }

                                        @Override
                                        public void scanSignedHDMServerQRCodeCancel() {

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
                    DropdownMessage.showDropdownMessage(activity, m);
                }
            }
        }.start();
    }

    private void initHDMBidFromColdRoot() {
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

    public void xrandomResult() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (delegate != null) {
                    delegate.moveToCold(true);
                }
            }
        }, 500);

    }

    public void scanColdResult(String result) {
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
                                    serviceClick();
                                } else {
                                    if (delegate != null) {
                                        delegate.moveToServer(true);
                                    }
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
                                DropdownMessage.showDropdownMessage(activity,
                                        R.string.hdm_keychain_add_scan_cold);
                            }
                        });
                    }
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
            coldRoot = null;
            DropdownMessage.showDropdownMessage(activity,
                    R.string.hdm_keychain_add_scan_cold);
        }

    }

    public void serverQRCode(final String result) {
        if (hdmBid == null) {
            return;
        }
        if (!dp.isShowing()) {
            dp.show();
        }
        final DialogProgress dd = dp;
        new ThreadNeedService(null, activity) {
            @Override
            public void runWithService(BlockchainService service) {
                try {
                    SecureCharSequence password = passwordGetter.getPassword();
                    if (password == null) {
                        return;
                    }
                    hdmBid.setSignature(result, password);
                    if (service != null) {
                        service.stopAndUnregister();
                    }
                    final HDMKeychain keychain = AddressManager.getInstance().getHdmKeychain();
                    final List<HDMAddress> as = keychain.completeAddresses(1, password,
                            new HDMKeychain.HDMFetchRemotePublicKeys() {
                                @Override
                                public void completeRemotePublicKeys(CharSequence password,
                                                                     List<HDMAddress.Pubs>
                                                                             partialPubs) {
                                    try {
                                        HDMKeychain.getRemotePublicKeys(hdmBid, password,
                                                partialPubs);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        int msg = R.string.network_or_connection_error;
                                        if (e instanceof Http400Exception) {
                                            msg = ExceptionUtil.getHDMHttpExceptionMessage((
                                                    (Http400Exception) e).getErrorCode());
                                        }
                                        final int m = msg;
                                        ThreadUtil.runOnMainThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (dp != null && dp.isShowing()) {
                                                    dp.dismiss();
                                                }
                                                DropdownMessage.showDropdownMessage
                                                        (activity, m);
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
                                    if (delegate != null) {
                                        delegate.moveToFinal(true);
                                    }

                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    final Exception finalE = e;
                    ThreadUtil.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dd.isShowing()) {
                                dd.dismiss();
                            }
                            int msg = R.string.hdm_keychain_add_sign_server_qr_code_error;
                            if (finalE instanceof Http400Exception) {
                                msg = ExceptionUtil.getHDMHttpExceptionMessage((
                                        (Http400Exception) finalE).getErrorCode());

                            }
                            DropdownMessage.showDropdownMessage(activity,
                                    msg);
                        }
                    });
                }
            }
        }.start();

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    public void wipe() {
        if (passwordGetter != null) {
            passwordGetter.wipe();
        }
        if (coldRoot != null) {
            Utils.wipeBytes(coldRoot);
        }
    }

    public boolean getHdmKeychainLimit() {
        return hdmKeychainLimit;
    }

    public void setHdmKeychainLimit(Boolean hdmKeychainLimit) {
        this.hdmKeychainLimit = hdmKeychainLimit;
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
