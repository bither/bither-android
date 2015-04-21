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

package net.bither.ui.base.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.SendActivity;
import net.bither.activity.hot.GenerateUnsignedTxActivity;
import net.bither.activity.hot.HdmSendActivity;
import net.bither.activity.hot.SelectAddressToSendActivity;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.util.UIUtil;
import net.bither.util.UnitUtilWrapper;

import java.util.ArrayList;
import java.util.List;

public class DialogDonate extends CenterDialog implements OnDismissListener, OnShowListener {
    private static final int ListItemHeight = UIUtil.dip2pix(45);
    private static final int MinListHeight = UIUtil.dip2pix(100);
    private static final int MaxListHeight = Math.min(UIUtil.dip2pix(360),
            UIUtil.getScreenHeight() - UIUtil.dip2pix(70));

    private ListView lv;
    private ProgressBar pb;
    private TextView tvNoAddress;
    private FrameLayout fl;
    private ArrayList<AddressBalance> addresses = new ArrayList<AddressBalance>();
    private Intent intent;

    public DialogDonate(Context context) {
        super(context);
        setContentView(R.layout.dialog_donate);
        setOnDismissListener(this);
        setOnShowListener(this);
        tvNoAddress = (TextView) findViewById(R.id.tv_no_address);
        lv = (ListView) findViewById(R.id.lv);
        pb = (ProgressBar) findViewById(R.id.pb);
        fl = (FrameLayout) findViewById(R.id.fl);
        lv.setAdapter(adapter);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        loadData();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (intent != null) {
            if (getContext() instanceof Activity) {
                Activity a = (Activity) getContext();
                a.startActivityForResult(intent, SelectAddressToSendActivity.SEND_REQUEST_CODE);
            } else {
                getContext().startActivity(intent);
            }
        }
    }

    private void loadData() {
        pb.setVisibility(View.VISIBLE);
        lv.setVisibility(View.INVISIBLE);
        tvNoAddress.setVisibility(View.GONE);
        addresses.clear();
        adapter.notifyDataSetChanged();
        new Thread() {
            public void run() {
                List<Address> as = AddressManager.getInstance().getAllAddresses();
                final ArrayList<AddressBalance> availableAddresses = new ArrayList<AddressBalance>();
                for (Address a : as) {
                    long balance = a.getBalance();
                    if (balance > 0) {
                        availableAddresses.add(new AddressBalance(a, balance));
                    }
                }
                lv.post(new Runnable() {
                    @Override
                    public void run() {
                        addresses.addAll(availableAddresses);
                        adapter.notifyDataSetChanged();
                        fl.getLayoutParams().height = getFlHeight();
                        if (addresses.size() > 0) {
                            lv.setVisibility(View.VISIBLE);
                            tvNoAddress.setVisibility(View.GONE);
                        } else {
                            lv.setVisibility(View.GONE);
                            tvNoAddress.setVisibility(View.VISIBLE);
                        }
                        pb.setVisibility(View.GONE);
                    }
                });
            }
        }.start();
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
                convertView = inflater.inflate(R.layout.list_item_select_address_to_send, null);
                h = new ViewHolder(convertView);
                convertView.setTag(h);
            }
            AddressBalance a = getItem(position);
            h.tvAddress.setText(a.address.getShortAddress());
            h.tvBalance.setText(UnitUtilWrapper.formatValueWithBold(a.balance));
            h.ivSymbol.setImageBitmap(UnitUtilWrapper.getBtcSlimSymbol(h.tvBalance));
            if (a.address.isHDM()) {
                h.ivType.setImageResource(R.drawable.address_type_hdm);
            } else if (a.address.hasPrivKey()) {
                h.ivType.setImageResource(R.drawable.address_type_private);
            } else {
                h.ivType.setImageResource(R.drawable.address_type_watchonly);
            }
            h.ibtnAddressFull.setVisibility(View.GONE);
            convertView.setOnClickListener(new ListItemClick(a));
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public AddressBalance getItem(int position) {
            return addresses.get(position);
        }

        @Override
        public int getCount() {
            return addresses.size();
        }

        class ViewHolder {
            TextView tvAddress;
            ImageView ivSymbol;
            TextView tvBalance;
            ImageView ivType;
            ImageButton ibtnAddressFull;

            public ViewHolder(View v) {
                tvAddress = (TextView) v.findViewById(R.id.tv_address);
                tvBalance = (TextView) v.findViewById(R.id.tv_balance);
                ivType = (ImageView) v.findViewById(R.id.iv_type);
                ivSymbol = (ImageView) v.findViewById(R.id.iv_symbol);
                ibtnAddressFull = (ImageButton) v.findViewById(R.id.ibtn_address_full);
                tvAddress.setTextColor(Color.WHITE);
                tvBalance.setTextColor(Color.WHITE);
            }
        }
    };

    private class ListItemClick implements View.OnClickListener {
        private AddressBalance address;

        public ListItemClick(AddressBalance address) {
            this.address = address;
        }

        @Override
        public void onClick(View v) {
            int position;
            Class<?> target;
            if (address.address.isHDM()) {
                position = AddressManager.getInstance().getHdmKeychain().getAddresses().indexOf
                        (address.address);
                target = HdmSendActivity.class;
            } else if (address.address.hasPrivKey()) {
                position = AddressManager.getInstance().getPrivKeyAddresses().indexOf(address.address);
                target = SendActivity.class;
            } else {
                position = AddressManager.getInstance().getWatchOnlyAddresses().indexOf(address.address);
                target = GenerateUnsignedTxActivity.class;
            }
            intent = new Intent(getContext(), target);
            intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG, position);
            intent.putExtra(SelectAddressToSendActivity.INTENT_EXTRA_ADDRESS, BitherjSettings.DONATE_ADDRESS);
            intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_IS_HDM_KEY_PASS_VALUE_TAG,
                    address.address.isHDM());
            if (address.balance > BitherSetting.DONATE_AMOUNT) {
                intent.putExtra(SelectAddressToSendActivity.INTENT_EXTRA_AMOUNT, BitherSetting.DONATE_AMOUNT);
            } else {
                intent.putExtra(SelectAddressToSendActivity.INTENT_EXTRA_AMOUNT, address.balance);
            }
            dismiss();
        }
    }

    private static final class AddressBalance implements Comparable<AddressBalance> {
        public Address address;
        public long balance;

        public AddressBalance(Address address, long balance) {
            this.address = address;
            this.balance = balance;
        }

        @Override
        public int compareTo(AddressBalance another) {
            if (address.isHDM() && !another.address.isHDM()) {
                return 1;
            }
            if (address.hasPrivKey() && !another.address.hasPrivKey()) {
                return 1;
            }
            if (!address.hasPrivKey() && another.address.hasPrivKey()) {
                return -1;
            }
            long gap = balance - another.balance;
            return gap == 0 ? 0 : (int) (gap / Math.abs(gap));
        }
    }

    private int getFlHeight() {
        int listHeight = addresses.size() * ListItemHeight + (addresses.size() - 1) * lv.getDividerHeight();
        return Math.min(MaxListHeight, Math.max(listHeight, MinListHeight));
    }
}
