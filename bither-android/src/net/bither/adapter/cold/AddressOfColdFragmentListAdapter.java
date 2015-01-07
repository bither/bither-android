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

package net.bither.adapter.cold;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.ui.base.ColdAddressFragmentHDMListItemView;
import net.bither.ui.base.ColdAddressFragmentListItemView;

import java.util.List;

public class AddressOfColdFragmentListAdapter extends BaseAdapter {
    private static final Object HDMKeychainPlaceHolder = new Object();
    private static final int ItemTypePrivateKey = 0;
    private static final int ItemTypeHDMKeychain = 1;

    private FragmentActivity activity;

    private List<Address> privates;

    public AddressOfColdFragmentListAdapter(FragmentActivity activity,
                                            List<Address> privates) {
        this.activity = activity;
        this.privates = privates;
    }

    @Override
    public int getViewTypeCount() {
        int count = 0;
        if(hasHDMKeychain()){
            count ++;
        }
        if(privates.size() > 0){
            count ++;
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        Object o = getItem(position);
        if(o == HDMKeychainPlaceHolder){
            return ItemTypeHDMKeychain;
        }
        return ItemTypePrivateKey;
    }

    @Override
    public int getCount() {
        return privates.size() + (hasHDMKeychain() ? 1 : 0);
    }

    @Override
    public Object getItem(int position) {
        if(hasHDMKeychain()){
            if(position == 0){
                return HDMKeychainPlaceHolder;
            }
            position--;
        }
        return privates.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(getItemViewType(position) == ItemTypeHDMKeychain){
            return getViewForHDMKeychain(position, convertView, parent);
        }
        return getViewForPrivateKey(position, convertView, parent);
    }

    private View getViewForHDMKeychain(int position, View convertView, ViewGroup parent){
        ColdAddressFragmentHDMListItemView view;
        if (convertView == null
                || !(convertView instanceof ColdAddressFragmentHDMListItemView)) {
            convertView = new ColdAddressFragmentHDMListItemView(activity);
        }
        view = (ColdAddressFragmentHDMListItemView) convertView;
        view.setKeychain(AddressManager.getInstance().getHdmKeychain());
        return convertView;
    }

    private View getViewForPrivateKey(int position, View convertView, ViewGroup parent){
        ColdAddressFragmentListItemView view;
        if (convertView == null
                || !(convertView instanceof ColdAddressFragmentListItemView)) {
            convertView = new ColdAddressFragmentListItemView(activity);
        }
        view = (ColdAddressFragmentListItemView) convertView;
        Address a;

        a = (Address) getItem(position);

        view.showAddress(a);
        return convertView;
    }

    private boolean hasHDMKeychain(){
        return AddressManager.getInstance().getHdmKeychain() != null;
    }
}
