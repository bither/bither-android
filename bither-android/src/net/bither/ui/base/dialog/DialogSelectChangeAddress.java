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

package net.bither.ui.base.dialog;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 14/12/15.
 */
public class DialogSelectChangeAddress extends CenterDialog {
    private static final int ListItemHeight = UIUtil.dip2pix(45);
    private static final int MinListHeight = UIUtil.dip2pix(100);
    private static final int MaxListHeight = Math.min(UIUtil.dip2pix(360),
            UIUtil.getScreenHeight() - UIUtil.dip2pix(70));

    private Address fromAddress;
    private Address changeAddress;
    private FrameLayout fl;
    private ListView lv;
    private ArrayList<Address> addresses = new ArrayList<Address>();

    public DialogSelectChangeAddress(Context context, Address fromAddress) {
        super(context);
        this.fromAddress = fromAddress;
        this.changeAddress = fromAddress;
        setContentView(R.layout.dialog_select_change_address);
        fl = (FrameLayout) findViewById(R.id.fl);
        lv = (ListView) findViewById(R.id.lv);
        lv.setAdapter(adapter);
    }

    private void loadData() {
        addresses.clear();
        addresses.add(fromAddress);
        List<Address> all = AddressManager.getInstance().getAllAddresses();
        for (Address a : all) {
            if (!a.equals(fromAddress)) {
                addresses.add(a);
            }
        }
        fl.getLayoutParams().height = getFlHeight();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void show() {
        loadData();
        super.show();
    }

    private BaseAdapter adapter = new BaseAdapter() {
        private LayoutInflater inflater;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getContext());
            }
            ViewHolder h;
            if (convertView != null && convertView.getTag() != null && convertView.getTag()
                    instanceof ViewHolder) {
                h = (ViewHolder) convertView.getTag();
            } else {
                convertView = inflater.inflate(R.layout.list_item_select_change_address, null);
                h = new ViewHolder(convertView);
                convertView.setTag(h);
            }
            Address a = getItem(position);
            h.tvAddress.setText(a.getShortAddress());
            if (a.hasPrivKey()) {
                h.ivType.setImageResource(R.drawable.address_type_private);
            } else {
                h.ivType.setImageResource(R.drawable.address_type_watchonly);
            }
            if (a.equals(changeAddress)) {
                h.ivCheck.setVisibility(View.VISIBLE);
            } else {
                h.ivCheck.setVisibility(View.INVISIBLE);
            }
            convertView.setOnClickListener(new ListItemClick(a));
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Address getItem(int position) {
            return addresses.get(position);
        }

        @Override
        public int getCount() {
            return addresses.size();
        }

        class ViewHolder {
            TextView tvAddress;
            ImageView ivType;
            ImageView ivCheck;

            public ViewHolder(View v) {
                tvAddress = (TextView) v.findViewById(R.id.tv_address);
                ivType = (ImageView) v.findViewById(R.id.iv_type);
                ivCheck = (ImageView) v.findViewById(R.id.iv_check);
                tvAddress.setTextColor(Color.WHITE);
            }
        }
    };

    private Runnable dismissRunnable = new Runnable() {
        @Override
        public void run() {
            dismiss();
        }
    };

    private class ListItemClick implements View.OnClickListener {
        private Address address;

        public ListItemClick(Address address) {
            this.address = address;
        }

        @Override
        public void onClick(View v) {
            container.removeCallbacks(dismissRunnable);
            changeAddress = address;
            adapter.notifyDataSetChanged();
            container.postDelayed(dismissRunnable, 300);
        }
    }

    public Address getChangeAddress() {
        return changeAddress;
    }

    private int getFlHeight() {
        int listHeight = addresses.size() * ListItemHeight + (addresses.size() - 1) * lv
                .getDividerHeight();
        return Math.min(MaxListHeight, Math.max(listHeight, MinListHeight));
    }
}
