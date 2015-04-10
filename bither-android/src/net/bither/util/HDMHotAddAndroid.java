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

package net.bither.util;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import net.bither.R;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.api.http.Http400Exception;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.delegate.HDMHotAdd;
import net.bither.bitherj.delegate.HDMSingular;
import net.bither.bitherj.utils.Utils;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogHDMServerUnsignedQRCode;
import net.bither.ui.base.dialog.DialogHdmKeychainAddHot;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.xrandom.HDMKeychainHotUEntropyActivity;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

public class HDMHotAddAndroid extends HDMHotAdd {
    private Activity activity;
    private DialogProgress dp;

    public HDMHotAddAndroid(Activity activity, IHDMHotAddDelegate delegate, HDMSingular.HDMSingularDelegate hdmSingularUtilDelegate) {
        super(delegate);
        this.activity = activity;
        this.delegate = delegate;
        singular = new HDMSingularAndroid(activity, hdmSingularUtilDelegate);
        this.passwordGetter = new DialogPassword.PasswordGetter(activity, this);
        dp = new DialogProgress(activity, R.string.please_wait);
        dp.setCancelable(false);
        hdmKeychainLimit = AddressManager.isHDMKeychainLimit();

    }


    @Override
    public void hotClick() {
        if (hdmKeychainLimit) {
            return;
        }
        if (singular.isInSingularMode()) {
            return;
        }
        new DialogHdmKeychainAddHot(activity, new DialogHdmKeychainAddHot
                .DialogHdmKeychainAddHotDelegate() {

            @Override
            public void addWithXRandom() {
                HDMKeychainHotUEntropyActivity.passwordGetter = passwordGetter;
                if (singular.shouldGoSingularMode()) {
                    HDMKeychainHotUEntropyActivity.singularUtil = singular;
                } else {
                    singular.runningWithoutSingularMode();
                }
                if (delegate != null) {
                    delegate.callKeychainHotUEntropy();
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
                        if (singular.shouldGoSingularMode()) {
                            singular.setPassword(password);
                            singular.generateEntropy();
                        } else {
                            singular.runningWithoutSingularMode();
                            HDMKeychain keychain = new HDMKeychain(new SecureRandom(),
                                    password);

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
                    }
                }.start();
            }
        }).show();
    }

    @Override
    public void coldClick() {
        if (hdmKeychainLimit) {
            return;
        }
        if (singular.isInSingularMode()) {
            return;
        }
        new DialogConfirmTask(activity, activity.getString(R.string.hdm_keychain_add_scan_cold),
                new Runnable() {

                    @Override
                    public void run() {
                        if (delegate != null) {
                            delegate.callScanCold();
                        }
                    }
                }).show();

    }

    @Override
    public void serviceClick() {
        if (hdmKeychainLimit) {
            return;
        }
        if (singular.isInSingularMode()) {
            return;
        }
        if (coldRoot == null && hdmBid == null) {
            isServerClicked = true;
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
                                                delegate.callServerQRCode();
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


    public void xrandomResult() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (singular.isInSingularMode()) {
                    singular.xrandomFinished();
                } else if (AddressManager.getInstance().getHdmKeychain() != null) {
                    if (delegate != null) {
                        delegate.moveToCold(true);
                    }
                }

            }
        }, 500);

    }

    @Override
    public void scanColdResult(String result) {
        try {
            coldRoot = Utils.hexStringToByteArray(result);
            final int count = AbstractApp.bitherjSetting.hdmAddressPerSeedPrepareCount() -
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

    @Override
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

    @Override
    public void beforePasswordDialogShow() {
        if (dp != null && dp.isShowing()) {
            dp.dismiss();
        }
    }

    @Override
    public void afterPasswordDialogDismiss() {
        if (dp != null && dp.isShowing()) {
            dp.dismiss();
        }
    }
}
