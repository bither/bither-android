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
import net.bither.bitherj.api.http.Http400Exception;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMBId;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.delegate.IPasswordGetterDelegate;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogHDMServerUnsignedQRCode;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by songchenwen on 15/2/11.
 */
public class HDMResetServerPasswordUtil implements IPasswordGetterDelegate {
    private int ServerQRCodeRequestCode = 1651;

    private DialogPassword.PasswordGetter passwordGetter;

    private DialogProgress dp;
    private Activity context;
    private ReentrantLock lock = new ReentrantLock();
    private Condition hdmIdCondiction = lock.newCondition();

    private HDMBId hdmBid;
    private String serverSignature;

    public HDMResetServerPasswordUtil(Activity context) {
        this(context, null, null);
    }

    public HDMResetServerPasswordUtil(Activity context, DialogProgress dp) {
        this(context, dp, null);
    }


    public HDMResetServerPasswordUtil(Activity context, CharSequence password) {
        this(context, null, password);
    }

    public HDMResetServerPasswordUtil(Activity context, DialogProgress dp, CharSequence password) {
        if (dp == null) {
            this.dp = new DialogProgress(context, R.string.please_wait);
        } else {
            this.dp = dp;
        }
        this.context = context;
        passwordGetter = new DialogPassword.PasswordGetter(context, this);
        setPassword(password);
    }

    public void setPassword(CharSequence password) {
        if (password != null) {
            passwordGetter.setPassword(new SecureCharSequence(password));
        } else {
            passwordGetter.setPassword(null);
        }
    }

    public boolean changePassword() {
        hdmBid = HDMBId.getHDMBidFromDb();
        serverSignature = null;
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dp.show();
            }
        });
        String pre;
        try {
            pre = hdmBid.getPreSignString();
        } catch (Http400Exception ex400) {
            ex400.printStackTrace();
            showMsg(ExceptionUtil.getHDMHttpExceptionMessage(ex400.getErrorCode()));
            passwordGetter.wipe();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            showMsg(R.string.network_or_connection_error);
            passwordGetter.wipe();
            return false;
        }
        final String preSign = pre;
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dp.isShowing()) {
                    dp.dismiss();
                }
                serverSignature = null;
                new DialogHDMServerUnsignedQRCode(context, preSign,
                        new DialogHDMServerUnsignedQRCode.DialogHDMServerUnsignedQRCodeListener() {

                            @Override
                            public void scanSignedHDMServerQRCode() {
                                context.startActivityForResult(new Intent(context, ScanActivity.class),
                                        ServerQRCodeRequestCode);
                            }

                            @Override
                            public void scanSignedHDMServerQRCodeCancel() {
                                serverSignature = null;
                                try {
                                    lock.lock();
                                    hdmIdCondiction.signal();
                                } finally {
                                    lock.unlock();
                                }
                            }
                        }).show();
            }
        });
        try {
            lock.lock();
            hdmIdCondiction.awaitUninterruptibly();
        } finally {
            lock.unlock();
        }
        if (serverSignature == null) {
            passwordGetter.wipe();
            return false;
        }
        SecureCharSequence password = passwordGetter.getPassword();
        if (password == null) {
            passwordGetter.wipe();
            return false;
        }
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!dp.isShowing()) {
                    dp.show();
                }
            }
        });
        try {
            if (AddressManager.getInstance().getHdmKeychain() != null && AddressManager.getInstance().getHdmKeychain().isInRecovery()) {
                hdmBid.recoverHDM(serverSignature, password);
            } else {
                hdmBid.setSignature(serverSignature, password);
            }
        } catch (Http400Exception ex400) {
            ex400.printStackTrace();
            showMsg(ExceptionUtil.getHDMHttpExceptionMessage(ex400.getErrorCode()));
            passwordGetter.wipe();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            showMsg(R.string.hdm_keychain_add_sign_server_qr_code_error);
            passwordGetter.wipe();
            return false;
        }
        passwordGetter.wipe();
        return true;
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ServerQRCodeRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                serverSignature = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            } else {
                serverSignature = null;
            }
            try {
                lock.lock();
                hdmIdCondiction.signal();
            } finally {
                lock.unlock();
            }
            return true;
        }
        return false;
    }

    private void showMsg(final int msg) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DropdownMessage.showDropdownMessage(context, msg);
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
