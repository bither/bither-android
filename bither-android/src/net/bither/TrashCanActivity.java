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

package net.bither;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.fragment.cold.ColdAddressFragment;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.OverScrollableListView;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.StringUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.WalletUtils;

import java.util.ArrayList;

/**
 * Created by songchenwen on 14-11-3.
 */
public class TrashCanActivity extends SwipeRightFragmentActivity {
    private OverScrollableListView lv;
    private TextView tvEmpty;
    private ArrayList<Address> addresses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash_can);
        addresses = new ArrayList<Address>();
        initView();
        refresh();
    }

    private void refresh() {
        addresses.clear();
        addresses.addAll(AddressManager.getInstance().getTrashAddresses());
        adapter.notifyDataSetChanged();
        if (addresses.size() > 0) {
            tvEmpty.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        lv = (OverScrollableListView) findViewById(R.id.lv);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);
        lv.setAdapter(adapter);

    }

    private BaseAdapter adapter = new BaseAdapter() {
        private LayoutInflater inflater;
        private DialogProgress dp;

        @Override
        public int getCount() {
            return addresses.size();
        }

        @Override
        public Address getItem(int position) {
            return addresses.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder h;
            if (convertView == null) {
                if (inflater == null) {
                    inflater = LayoutInflater.from(TrashCanActivity.this);
                }
                convertView = inflater.inflate(R.layout.list_item_trash_can, null);
                h = new ViewHolder(convertView);
            } else {
                h = (ViewHolder) convertView.getTag();
            }
            Address address = getItem(position);
            h.tv.setText(WalletUtils.formatHash(address.getAddress(), 4, 12));
            h.flAddress.setOnClickListener(new CopyClick(address.getAddress()));
            h.ibtnViewOnNet.setOnClickListener(new ViewOnNetClick(address.getAddress()));
            if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.HOT) {
                h.ibtnViewOnNet.setVisibility(View.VISIBLE);
            } else {
                h.ibtnViewOnNet.setVisibility(View.GONE);
            }
            convertView.setOnClickListener(new ItemClick(address));
            return convertView;
        }

        class ViewHolder {
            TextView tv;
            FrameLayout flAddress;
            ImageButton ibtnViewOnNet;

            public ViewHolder(View v) {
                tv = (TextView) v.findViewById(R.id.tv_address);
                flAddress = (FrameLayout) v.findViewById(R.id.fl_address);
                ibtnViewOnNet = (ImageButton) v.findViewById(R.id.ibtn_view_on_net);
                v.setTag(this);
            }
        }

        class CopyClick implements View.OnClickListener {
            private String address;

            public CopyClick(String address) {
                this.address = address;
            }

            @Override
            public void onClick(View v) {
                StringUtil.copyString(address);
                DropdownMessage.showDropdownMessage(TrashCanActivity.this,
                        R.string.copy_address_success);
            }
        }

        class ViewOnNetClick implements View.OnClickListener {
            private String address;

            ViewOnNetClick(String address) {
                this.address = address;
            }

            @Override
            public void onClick(View v) {
                DialogConfirmTask dialog = new DialogConfirmTask(v.getContext(),
                        getString(R.string.address_option_view_on_blockchain_info), new Runnable() {
                    @Override
                    public void run() {
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http" +
                                        "://blockchain.info/address/" + address)).addFlags(Intent
                                        .FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    startActivity(intent);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    DropdownMessage.showDropdownMessage(TrashCanActivity.this,
                                            R.string.find_browser_error);
                                }
                            }
                        });
                    }
                });
                dialog.show();
            }
        }

        class ItemClick implements View.OnClickListener {
            private Address address;

            ItemClick(Address address) {
                this.address = address;
            }

            @Override
            public void onClick(View v) {
                if (dp == null) {
                    dp = new DialogProgress(TrashCanActivity.this, R.string.please_wait);
                }
                new DialogConfirmTask(TrashCanActivity.this,
                        getString(R.string.trash_address_restore), new Runnable() {
                    @Override
                    public void run() {
                        new ThreadNeedService(dp, TrashCanActivity.this){
                            @Override
                            public void runWithService(BlockchainService service) {
                                if (service != null) {
                                    service.stopAndUnregister();
                                }
                                AddressManager.getInstance().restorePrivKey(address);
                                if(service != null) {
                                    service.startAndRegister();
                                }
                                ThreadUtil.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(dp.isShowing()) {
                                            dp.dismiss();
                                        }
                                        refresh();
                                        notifyDataSetChanged();
                                        if (AppSharedPreference.getInstance().getAppMode() ==
                                                BitherjSettings.AppMode.HOT) {
                                            Fragment f = BitherApplication.hotActivity.getFragmentAtIndex
                                                    (1);
                                            if (f instanceof HotAddressFragment) {
                                                HotAddressFragment hotAddressFragment =
                                                        (HotAddressFragment) f;
                                                hotAddressFragment.refresh();
                                            }
                                        } else {
                                            Fragment f = BitherApplication.coldActivity
                                                    .getFragmentAtIndex(1);
                                            if (f instanceof ColdAddressFragment) {
                                                ColdAddressFragment coldAddressFragment =
                                                        (ColdAddressFragment) f;
                                                coldAddressFragment.refresh();
                                            }
                                        }
                                    }
                                });
                            }
                        }.start();
                    }
                }).show();
            }
        }
    };
}
