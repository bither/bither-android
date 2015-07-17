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

package net.bither.fragment.cold;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ListView;

import net.bither.R;
import net.bither.adapter.cold.AddressOfColdFragmentListAdapter;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.ColdAddressFragmentHDMListItemView;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SmoothScrollListRunnable;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogSimpleQr;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

public class ColdAddressFragment extends Fragment implements Refreshable,
        Selectable, ColdAddressFragmentHDMListItemView.RequestHDMServerQrCodeDelegate {
    private static final int HDMServerQrCodeRequestCode = 1320;
    private ListView lvPrivate;
    private View ivNoAddress;
    private AddressOfColdFragmentListAdapter mAdapter;
    private List<Address> privates;
    private List<String> addressesToShowAdded;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_cold_address, container,
                false);
        lvPrivate = (ListView) view.findViewById(R.id.lv_address);
        ivNoAddress = view.findViewById(R.id.iv_no_address);
        privates = new ArrayList<Address>();
        mAdapter = new AddressOfColdFragmentListAdapter(getActivity(), privates, this);
        lvPrivate.setAdapter(mAdapter);
        if (AbstractApp.addressIsReady) {
            List<Address> ps = AddressManager.getInstance().getPrivKeyAddresses();
            if (ps != null) {
                privates.addAll(ps);
                mAdapter.notifyDataSetChanged();
            }
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        if (AbstractApp.addressIsReady) {
            List<Address> ps = AddressManager.getInstance().getPrivKeyAddresses();
            if (ps != null) {
                privates.clear();
                privates.addAll(ps);
            }
            mAdapter.notifyDataSetChanged();
            if (privates.size() == 0 && !AddressManager.getInstance().hasHDMKeychain() &&
                    !AddressManager.getInstance().hasHDAccountCold()) {
                ivNoAddress.setVisibility(View.VISIBLE);
                lvPrivate.setVisibility(View.GONE);
            } else {
                ivNoAddress.setVisibility(View.GONE);
                lvPrivate.setVisibility(View.VISIBLE);
            }
            if (addressesToShowAdded != null) {
                lvPrivate.postDelayed(showAddressesAddedRunnable, 600);
            }
        }
    }

    public void showAddressesAdded(List<String> addresses) {
        addressesToShowAdded = addresses;
    }

    @Override
    public void onSelected() {

    }

    @Override
    public void doRefresh() {
        if (lvPrivate == null) {
            return;
        }
        if (lvPrivate.getFirstVisiblePosition() != 0) {
            lvPrivate.post(new SmoothScrollListRunnable(lvPrivate, 0,
                    new Runnable() {
                        @Override
                        public void run() {
                            refresh();
                        }
                    }));
        } else {
            refresh();
        }
    }

    private Runnable showAddressesAddedRunnable = new Runnable() {
        @Override
        public void run() {
            if (addressesToShowAdded == null
                    || addressesToShowAdded.size() == 0) {
                return;
            }
            int startIndex = 0;
            if(AddressManager.getInstance().hasHDMKeychain()){
                startIndex = 1;
                if(addressesToShowAdded.contains(AddAddressColdHDMFragment.HDMSeedAddressPlaceHolder)){
                    startIndex = 0;
                }
            }
            if (AddressManager.getInstance().hasHDAccountCold()) {
                startIndex = 1;
                if (addressesToShowAdded.contains(HDAccount.HDAccountPlaceHolder)) {
                    startIndex = 0;
                }
            }
            for (int i = 0; i < addressesToShowAdded.size()
                    && i + startIndex < lvPrivate.getChildCount(); i++) {
                View v = lvPrivate.getChildAt(i + startIndex);
                v.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        R.anim.address_notification));
            }
            addressesToShowAdded = null;
        }
    };

    @Override
    public void requestHDMServerQrCode(HDMKeychain keychain) {
        startActivityForResult(new Intent(getActivity(), ScanActivity.class),
                HDMServerQrCodeRequestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (HDMServerQrCodeRequestCode == requestCode && resultCode == Activity.RESULT_OK) {
            final String result = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            if (Utils.isEmpty(result)) {
                return;
            }
            new DialogPassword(getActivity(), new IDialogPasswordListener() {
                @Override
                public void onPasswordEntered(final SecureCharSequence password) {
                    final DialogProgress dp = new DialogProgress(getActivity(),
                            R.string.please_wait);
                    dp.setCancelable(false);
                    dp.show();
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                final String signed = AddressManager.getInstance().getHdmKeychain
                                        ().signHDMBId(result, password);
                                password.wipe();
                                ThreadUtil.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (dp.isShowing()) {
                                            dp.dismiss();
                                        }
                                        new DialogSimpleQr(getActivity(), signed,
                                                R.string.hdm_keychain_add_signed_server_qr_code_title).show();
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                ThreadUtil.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (dp.isShowing()) {
                                            dp.dismiss();
                                        }
                                        DropdownMessage.showDropdownMessage(getActivity(),
                                                R.string.hdm_keychain_add_sign_server_qr_code_error);
                                    }
                                });
                            }
                        }
                    }.start();
                }
            }).show();
        }
    }
}
