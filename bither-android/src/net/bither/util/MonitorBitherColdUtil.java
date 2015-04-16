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
import android.support.v4.app.Fragment;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.qrcode.QRCodeEnodeUtil;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.ScanActivity;
import net.bither.qrcode.ScanQRCodeTransportActivity;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/4/16.
 */
public class MonitorBitherColdUtil {
    public interface MonitorBitherColdUtilDelegate {
        void onAddressMonitored(ArrayList<String> addresses);
    }

    private Activity activity;
    private Fragment fragment;
    private DialogProgress dp;
    private MonitorBitherColdUtilDelegate delegate;

    public MonitorBitherColdUtil(Fragment fragment, MonitorBitherColdUtilDelegate delegate) {
        this(null, fragment, delegate);
    }

    public MonitorBitherColdUtil(Activity activity, MonitorBitherColdUtilDelegate delegate) {
        this(activity, null, delegate);
    }

    public MonitorBitherColdUtil(Activity activity, Fragment fragment,
                                 MonitorBitherColdUtilDelegate delegate) {
        this.fragment = fragment;
        if (fragment != null && activity == null) {
            activity = fragment.getActivity();
        }
        this.activity = activity;
        this.delegate = delegate;
        dp = new DialogProgress(activity, R.string.please_wait);
        dp.setCancelable(false);
    }


    public void scan() {
        Intent intent = new Intent(activity, ScanQRCodeTransportActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING, activity.getString(R.string
                .scan_for_all_addresses_in_bither_cold_title));
        if (fragment != null) {
            fragment.startActivityForResult(intent, BitherSetting.INTENT_REF
                    .SCAN_ALL_IN_BITHER_COLD_REUEST_CODE);
        } else {
            activity.startActivityForResult(intent, BitherSetting.INTENT_REF
                    .SCAN_ALL_IN_BITHER_COLD_REUEST_CODE);
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BitherSetting.INTENT_REF.SCAN_ALL_IN_BITHER_COLD_REUEST_CODE &&
                resultCode == Activity.RESULT_OK) {
            if (data.getExtras().containsKey(ScanActivity.INTENT_EXTRA_RESULT)) {
                final String content = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                if (Utils.isEmpty(content) || !checkQrCodeContent(content)) {
                    DropdownMessage.showDropdownMessage(activity, R.string
                            .scan_for_all_addresses_in_bither_cold_failed);
                    return true;
                }
                ThreadNeedService thread = new ThreadNeedService(dp, activity) {

                    @Override
                    public void runWithService(BlockchainService service) {
                        processQrCodeContent(content, service);
                    }
                };
                thread.start();
            }
            return true;
        }
        return false;
    }

    private boolean checkQrCodeContent(String content) {
        String[] strs = QRCodeUtil.splitString(content);
        for (String str : strs) {
            boolean checkCompressed = str.length() == 66 || ((str.length() == 67) && (str.indexOf
                    (QRCodeUtil.XRANDOM_FLAG) == 0));
            boolean checkUnCompressed = str.length() == 130 || ((str.length() == 131) && (str
                    .indexOf(QRCodeUtil.XRANDOM_FLAG) == 0));
            if (!checkCompressed && !checkUnCompressed) {
                return false;
            }
        }
        return true;
    }

    private void processQrCodeContent(String content, BlockchainService service) {
        try {
            List<Address> wallets = QRCodeEnodeUtil.formatPublicString(content);
            addAddress(service, wallets);
            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                }
            });
        } catch (Exception e) {
            if (dp.isShowing()) {
                dp.setThread(null);
                dp.dismiss();
            }
            DropdownMessage.showDropdownMessage(activity, R.string
                    .scan_for_all_addresses_in_bither_cold_failed);

        }
    }

    private void addAddress(final BlockchainService service, final List<Address> wallets) {
        try {
            final ArrayList<String> addresses = new ArrayList<String>();
            for (Address address : wallets) {
                if (!AddressManager.getInstance().getAllAddresses().contains(address)) {
                    addresses.add(address.getAddress());
                }
            }
            KeyUtil.addAddressListByDesc(service, wallets);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (delegate != null) {
                        delegate.onAddressMonitored(addresses);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            DropdownMessage.showDropdownMessage(activity, R.string.network_or_connection_error);
        }
    }
}
