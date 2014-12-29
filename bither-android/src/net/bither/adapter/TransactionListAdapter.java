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

package net.bither.adapter;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.common.primitives.Longs;

import net.bither.activity.hot.AddressDetailActivity;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.Tx;
import net.bither.ui.base.TransactionListItem;

import java.util.List;


public class TransactionListAdapter extends BaseAdapter {
    private AddressDetailActivity activity;
    private List<Tx> transactions;
    private Address address;

    public TransactionListAdapter(AddressDetailActivity activity,
                                  List<Tx> transactions, Address address) {
        this.activity = activity;
        this.transactions = transactions;
        this.address = address;
    }

    @Override
    public int getCount() {
        return transactions.size();
    }

    @Override
    public Tx getItem(int position) {
        return transactions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return Longs.fromByteArray(transactions.get(position).getTxHash());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TransactionListItem view;
        if (convertView == null
                || !(convertView instanceof TransactionListItem)) {
            convertView = new TransactionListItem(activity);
        }
        view = (TransactionListItem) convertView;
        view.setTransaction(transactions.get(position), address);
        return convertView;
    }
}
