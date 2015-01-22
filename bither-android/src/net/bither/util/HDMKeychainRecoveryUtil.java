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

package net.bither.util;

import android.app.Activity;
import android.content.Intent;

import net.bither.R;
import net.bither.bitherj.api.RecoveryHDMApi;
import net.bither.bitherj.api.http.Http400Exception;
import net.bither.bitherj.api.http.HttpException;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.HDMBId;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.hd.DeterministicKey;
import net.bither.bitherj.crypto.hd.HDKeyDerivation;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogHDMServerUnsignedQRCode;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by songchenwen on 15/1/21.
 */
public class HDMKeychainRecoveryUtil implements DialogPassword.PasswordGetter
        .PasswordGetterDelegate {
    private static final int ColdRootRequestCode = 1306;
    private static final int ServerQRCodeRequestCode = 1731;
    private DialogProgress dp;
    private Activity context;
    private ReentrantLock lock = new ReentrantLock();
    private Condition coldRootCondition = lock.newCondition();
    private Condition hdmIdCondiction = lock.newCondition();

    private byte[] coldRoot;
    private HDMBId hdmBid;
    private String hdmBidSignature;

    public HDMKeychainRecoveryUtil(Activity context) {
        this(context, null);
    }

    public HDMKeychainRecoveryUtil(Activity context, DialogProgress dp) {
        if (dp == null) {
            this.dp = new DialogProgress(context, R.string.please_wait);
        } else {
            this.dp = dp;
        }
        this.context = context;
    }

    public boolean canRecover() {
        return AddressManager.getInstance().getHdmKeychain() == null;
    }

    public int recovery() {
        if (AddressManager.getInstance().getHdmKeychain() != null) {
            throw new RuntimeException("Already has hdm keychain can not recover");
        }
        DialogPassword.PasswordGetter passwordGetter = new DialogPassword.PasswordGetter(context,
                this);
        if (getColdRoot() == null) {
            return 0;
        }
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (!dp.isShowing()) {
                    dp.show();
                }
            }
        });
        String preSign;
        try {
            preSign = getHDMIdPresign();
        } catch (Exception e) {
            e.printStackTrace();
            int msg = R.string.network_or_connection_error;
            if (e instanceof Http400Exception) {
                msg = ExceptionUtil.getHDMHttpExceptionMessage(((Http400Exception) e)
                        .getErrorCode());
            }
            dismissDp();
            return msg;
        }
        getHDMSignature(preSign);
        if (hdmBidSignature == null) {
            dismissDp();
            return 0;
        }
        SecureCharSequence password = passwordGetter.getPassword();
        if (password == null) {
            dismissDp();
            return 0;
        }

        HDMKeychain.HDMKeychainRecover keychain;
        try {
            keychain = new HDMKeychain.HDMKeychainRecover(coldRoot, password,
                    new HDMKeychain.HDMFetchRemoteAddresses() {
                        @Override
                        public List<HDMAddress.Pubs> getRemoteExistsPublicKeys(CharSequence password) {
                            try {
                                return hdmBid.recoverHDM(hdmBidSignature, password);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            int msg = R.string.network_or_connection_error;
            if (e instanceof Http400Exception) {
                msg = ExceptionUtil.getHDMHttpExceptionMessage(((Http400Exception) e)
                        .getErrorCode());
            }
            dismissDp();
            return msg;
        }


        if (keychain.getAllCompletedAddresses().size() > 0) {
            KeyUtil.setHDKeyChain(keychain, password);
        } else {
            dismissDp();
            return R.string.hdm_keychain_recovery_no_addresses;
        }

        dismissDp();
        return R.string.hdm_keychain_recovery_message;
    }

    private void getHDMSignature(final String presign) {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (dp.isShowing()) {
                    dp.dismiss();
                }
                new DialogHDMServerUnsignedQRCode(context, presign,
                        new DialogHDMServerUnsignedQRCode.DialogHDMServerUnsignedQRCodeListener() {
                            @Override
                            public void scanSignedHDMServerQRCode() {
                                context.startActivityForResult(new Intent(context, ScanActivity.class),
                                        ServerQRCodeRequestCode);
                            }
                        }).show();
            }
        });
        try {
            lock.lockInterruptibly();
            coldRootCondition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private String getHDMIdPresign() throws Exception {
        initHDMBidFromColdRoot();
        return hdmBid.getPreSignString();
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

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ColdRootRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                coldRoot = Utils.hexStringToByteArray(result);
            } else {
                coldRoot = null;
            }
            try {
                lock.lock();
                coldRootCondition.signal();
            } finally {
                lock.unlock();
            }
            return true;
        }
        return false;
    }


    private byte[] getColdRoot() {
        if (coldRoot == null) {
            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    new DialogConfirmTask(context, context.getString(R.string
                            .hdm_keychain_add_scan_cold), new Runnable() {
                        @Override
                        public void run() {
                            ThreadUtil.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(context, ScanActivity.class);
                                    context.startActivityForResult(intent, ColdRootRequestCode);
                                }
                            });
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                            coldRoot = null;
                            try {
                                lock.lock();
                                coldRootCondition.signal();
                            } finally {
                                lock.unlock();
                            }
                        }
                    }).show();
                }
            });
            try {
                lock.lockInterruptibly();
                coldRootCondition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
        return coldRoot;
    }

    private void dismissDp() {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (dp.isShowing()) {
                    dp.dismiss();
                }
            }
        });
    }

    @Override
    public void beforePasswordDialogShow() {
        if (dp.isShowing()) {
            dp.dismiss();
        }
    }

    @Override
    public void afterPasswordDialogDismiss() {
        if (!dp.isShowing()) {
            dp.show();
        }
    }
}
