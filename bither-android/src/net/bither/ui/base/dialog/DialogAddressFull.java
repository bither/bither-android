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
import android.graphics.Color;
import android.support.v4.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

import net.bither.R;
import net.bither.ui.base.SubtransactionListItem;
import net.bither.util.UIUtil;

public class DialogAddressFull extends DialogWithArrow {
    private static final int MaxHeight = UIUtil.getScreenHeight()
            - UIUtil.dip2pix(100);
    private Activity activity;
    private ArrayMap<String, Long> addresses;
    private ListView lv;
    private BaseAdapter adapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return addresses.size();
        }

        @Override
        public Object getItem(int position) {
            return addresses.keyAt(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SubtransactionListItem view;
            if (convertView == null
                    || !(convertView instanceof SubtransactionListItem)) {
                convertView = new SubtransactionListItem(activity);
            }
            view = (SubtransactionListItem) convertView;
            view.setTextColor(Color.WHITE);
            view.setContent(addresses.keyAt(position),
                    addresses.valueAt(position));
            return convertView;
        }
    };

    public DialogAddressFull(Activity context,
                             ArrayMap<String, Long> addresses) {
        super(context);
        activity = context;
        this.addresses = addresses;
        setContentView(R.layout.dialog_address_full);
        lv = (ListView) findViewById(R.id.lv);
        int width = UIUtil.dip2pix(140);
        for (int i = 0;
             i < addresses.size();
             i++) {
            if (addresses.valueAt(i) != null) {
                width = Math.min(UIUtil.getScreenWidth() - UIUtil.dip2pix(100),
                        UIUtil.dip2pix(220));
                break;
            }
        }
        lv.getLayoutParams().height = Math.min(caculateHeight(), MaxHeight);
        lv.getLayoutParams().width = width;
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                dismiss();
            }
        });
    }

    private int caculateHeight() {
        if (addresses == null) {
            return super.getSuggestHeight();
        }
        int height = 0;
        for (String address : addresses.keySet()) {
            if (SubtransactionListItem.isMessage(address)) {
                height += SubtransactionListItem.MessageHeight;
            } else {
                height += SubtransactionListItem.Height;
            }
        }
        return height;
    }

    @Override
    public int getSuggestHeight() {
        if (addresses == null) {
            return super.getSuggestHeight();
        }
        return Math.min(caculateHeight(), MaxHeight)
                + UIUtil.dip2pix(20);
    }

}
