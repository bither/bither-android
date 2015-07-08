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

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import net.bither.R;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.exception.ScriptException;
import net.bither.ui.base.SubtransactionLabelInHDAccountListItem;
import net.bither.ui.base.SubtransactionListItem;
import net.bither.ui.base.SubtransactionOfOwnInHDAccountListItem;
import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by songchenwen on 15/7/2.
 */
public class DialogAddressFullForHD extends DialogWithArrow {
    private static final int MaxHeight = UIUtil.getScreenHeight() - UIUtil.dip2pix(100);
    private Activity activity;
    private List<HDAccount.HDAccountAddress> ownAddresses;
    private LinkedHashMap<String, Long> foreignAddresses = new LinkedHashMap<String, Long>();
    private HashMap<String, Long> ownValues = new HashMap<String, Long>();
    private ListView lv;
    private BaseAdapter adapter = new BaseAdapter() {
        static final int ViewTypeLabel = 0;
        static final int ViewTypeOwn = 1;
        static final int ViewTypeForeign = 2;

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            if (ownAddresses != null && ownAddresses.size() > 0) {
                if (position == 0) {
                    return ViewTypeLabel;
                }
                if (position < ownAddresses.size() + 1) {
                    return ViewTypeOwn;
                }
                position -= (ownAddresses.size() + 1);
            }
            if (foreignAddresses != null && foreignAddresses.size() > 0) {
                if (position == 0) {
                    return ViewTypeLabel;
                }
            }
            return ViewTypeForeign;
        }

        @Override
        public int getCount() {
            int count = 0;
            if (ownAddresses != null && ownAddresses.size() > 0) {
                count += (ownAddresses.size() + 1);
            }
            if (foreignAddresses != null && foreignAddresses.size() > 0) {
                count += (foreignAddresses.size() + 1);
            }
            return count;
        }

        @Override
        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            switch (getItemViewType(position)) {
                case ViewTypeLabel:
                    return getViewForLabel(position, convertView, parent);
                case ViewTypeForeign:
                    return getViewForForeign(position, convertView, parent);
                case ViewTypeOwn:
                    return getViewForOwn(position, convertView, parent);
            }
            return convertView;
        }

        private View getViewForLabel(int position, View convertView, ViewGroup parent) {
            SubtransactionLabelInHDAccountListItem view;
            if (convertView == null || !(convertView instanceof
                    SubtransactionLabelInHDAccountListItem)) {
                convertView = new SubtransactionLabelInHDAccountListItem(activity);
            }
            boolean own = ownAddresses != null && ownAddresses.size() > 0 && position == 0;
            view = (SubtransactionLabelInHDAccountListItem) convertView;
            view.setTextColor(Color.WHITE);
            view.setContent(own);
            return convertView;
        }

        private View getViewForOwn(int position, View convertView, ViewGroup parent) {
            SubtransactionOfOwnInHDAccountListItem view;
            if (convertView == null || !(convertView instanceof
                    SubtransactionOfOwnInHDAccountListItem)) {
                convertView = new SubtransactionOfOwnInHDAccountListItem(activity);
            }
            int index = position - 1;
            view = (SubtransactionOfOwnInHDAccountListItem) convertView;
            view.setTextColor(Color.WHITE);
            view.setContent(ownAddresses.get(index).getAddress(), ownValues.get(ownAddresses.get
                    (index).getAddress()), ownAddresses.get(index).getPathType());
            return convertView;
        }

        private View getViewForForeign(int position, View convertView, ViewGroup parent) {
            SubtransactionListItem view;
            if (convertView == null || !(convertView instanceof SubtransactionListItem)) {
                convertView = new SubtransactionListItem(activity);
            }
            int index = position - 1 - (ownAddresses != null && ownAddresses.size() > 0 ?
                    ownAddresses.size() + 1 : 0);
            view = (SubtransactionListItem) convertView;
            view.setTextColor(Color.WHITE);
            view.setContent((String) foreignAddresses.keySet().toArray()[index], (Long)
                    foreignAddresses.values().toArray()[index]);
            return convertView;
        }
    };

    public DialogAddressFullForHD(Activity context, Tx tx, HDAccount account) {
        super(context);
        activity = context;
        List<String> inAddresses = tx.getInAddresses();
        List<String> outAddresses = tx.getOutAddressList();
        ownAddresses = account.getRelatedAddressesForTx(tx, inAddresses);
        Collections.sort(ownAddresses, new Comparator<HDAccount.HDAccountAddress>() {
            @Override
            public int compare(HDAccount.HDAccountAddress lhs, HDAccount.HDAccountAddress rhs) {
                if (lhs.getPathType() != rhs.getPathType()) {
                    return lhs.getPathType().getValue() - rhs.getPathType().getValue();
                }
                return lhs.getIndex() - rhs.getIndex();
            }
        });
        boolean isIncoming = true;
        try {
            isIncoming = tx.deltaAmountFrom(account) >= 0;
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        List<String> foreign = foreignAddresses(isIncoming ? inAddresses : outAddresses);
        initForeignAddresses(isIncoming, tx, foreign);
        initView();
    }

    private void initForeignAddresses(boolean isIncoming, Tx tx, List<String> foreign) {
        String subAddress;
        long value;
        for (int i = 0;
             i < tx.getIns().size();
             i++) {
            subAddress = null;
            if (tx.isCoinBase()) {
                subAddress = getContext().getResources().getString(R.string.input_coinbase);
            } else {
                try {
                    subAddress = tx.getIns().get(i).getFromAddress();
                    if (subAddress != null && !foreign.contains(subAddress)) {
                        ownValues.put(subAddress, 0 - tx.getIns().get(i).getValue());
                        continue;
                    }
                } catch (ScriptException e) {
                    e.printStackTrace();
                }
            }
            if (isIncoming) {
                value = tx.getIns().get(i).getValue();
                if (subAddress == null) {
                    subAddress = getContext().getResources().getString(R.string
                            .address_cannot_be_parsed);
                }
                foreignAddresses.put(subAddress, value);
            }
        }
        for (int i = 0;
             i < tx.getOuts().size();
             i++) {
            subAddress = null;
            Out out = tx.getOuts().get(i);
            value = out.getOutValue();
            try {
                subAddress = out.getOutAddress();
                if (subAddress != null && !foreign.contains(subAddress)) {
                    ownValues.put(subAddress, value);
                    continue;
                }
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            if (!isIncoming) {
                if (subAddress == null) {
                    subAddress = getContext().getResources().getString(R.string
                            .address_cannot_be_parsed);
                }
                foreignAddresses.put(subAddress, value);
            }
        }
    }

    private void initView() {
        setContentView(R.layout.dialog_address_full);
        lv = (ListView) findViewById(R.id.lv);
        lv.getLayoutParams().height = Math.min(caculateHeight(), MaxHeight);
        lv.getLayoutParams().width = Math.min(UIUtil.getScreenWidth() - UIUtil.dip2pix(100),
                UIUtil.dip2pix(220));
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                dismiss();
            }
        });
    }

    private int caculateHeight() {
        if (foreignAddresses == null && ownAddresses == null) {
            return super.getSuggestHeight();
        }
        int height = 0;
        for (String address : foreignAddresses.keySet()) {
            if (SubtransactionListItem.isMessage(address)) {
                height += SubtransactionListItem.MessageHeight;
            } else {
                height += SubtransactionListItem.Height;
            }
        }
        height += ownAddresses.size() * SubtransactionOfOwnInHDAccountListItem.Height;
        if (ownAddresses.size() > 0) {
            height += SubtransactionLabelInHDAccountListItem.MessageHeight;
        }
        if (foreignAddresses.size() > 0) {
            height += SubtransactionLabelInHDAccountListItem.MessageHeight;
        }
        return height;
    }

    @Override
    public int getSuggestHeight() {
        if (foreignAddresses == null && ownAddresses == null) {
            return super.getSuggestHeight();
        }
        return Math.min(caculateHeight(), MaxHeight) + UIUtil.dip2pix(20);
    }

    private List<String> foreignAddresses(List<String> addresses) {
        ArrayList<String> result = new ArrayList<String>(addresses);
        for (HDAccount.HDAccountAddress a : ownAddresses) {
            if (result.contains(a.getAddress())) {
                result.remove(a.getAddress());
            }
        }
        return result;
    }
}
