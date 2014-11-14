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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
import net.bither.ui.base.OverScrollableListView;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogAddressFull;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;

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
            h.tv.setText(address.getShortAddress());
            h.ibtnFull.setOnClickListener(new AddressFullClick(address.getAddress()));
            convertView.setOnClickListener(new ItemClick(address));
            return convertView;
        }

        class ViewHolder {
            TextView tv;
            ImageButton ibtnFull;

            public ViewHolder(View v) {
                tv = (TextView) v.findViewById(R.id.tv_address);
                ibtnFull = (ImageButton) v.findViewById(R.id.ibtn_address_full);
                v.setTag(this);
            }
        }

        class AddressFullClick implements View.OnClickListener {
            private String address;

            public AddressFullClick(String address) {
                this.address = address;
            }

            @Override
            public void onClick(View v) {
                LinkedHashMap<String, Long> map = new LinkedHashMap<String, Long>();
                map.put(address, 0L);
                DialogAddressFull dialog = new DialogAddressFull(TrashCanActivity.this, map);
                dialog.show(v);
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
