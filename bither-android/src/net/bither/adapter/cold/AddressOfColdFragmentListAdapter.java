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
import net.bither.bitherj.core.EnterpriseHDMSeed;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.ui.base.ColdAddressFragmentHDAccountColdListItemView;
import net.bither.ui.base.ColdAddressFragmentHDMEnterpriseListItemView;
import net.bither.ui.base.ColdAddressFragmentHDMListItemView;
import net.bither.ui.base.ColdAddressFragmentListItemView;

import java.util.List;

public class AddressOfColdFragmentListAdapter extends BaseAdapter {
    private static final Object HDMKeychainPlaceHolder = new Object();
    private final int ItemTypePrivateKey = 0;
    private int ItemTypeHDMKeychain = 1;
    private int ItemTypeHDAccountCold = 1;
    private int ItemTypeEnterpriseHDM = 1;

    private FragmentActivity activity;
    private List<Address> privates;
    private HDAccountCold hdAccountCold;
    private EnterpriseHDMSeed enterpriseHDMSeed;
    private ColdAddressFragmentHDMListItemView.RequestHDMServerQrCodeDelegate requestHDMServerQrCodeDelegate;

    public AddressOfColdFragmentListAdapter(FragmentActivity activity,
                                            List<Address> privates, ColdAddressFragmentHDMListItemView.RequestHDMServerQrCodeDelegate requestHDMServerQrCodeDelegate) {
        this.activity = activity;
        this.privates = privates;
        this.requestHDMServerQrCodeDelegate = requestHDMServerQrCodeDelegate;
        if (AddressManager.getInstance().hasHDAccountCold()) {
            hdAccountCold = AddressManager.getInstance().getHDAccountCold();
        } else {
            hdAccountCold = null;
        }
        if (EnterpriseHDMSeed.hasSeed()) {
            enterpriseHDMSeed = EnterpriseHDMSeed.seed();
        } else {
            enterpriseHDMSeed = null;
        }
    }

    @Override
    public int getViewTypeCount() {
        int count = 1;
        if(hasHDMKeychain()){
            count++;
        }
        if (hdAccountCold != null) {
            count++;
        }
        if (enterpriseHDMSeed != null) {
            count++;
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        Object o = getItem(position);
        if(o == HDMKeychainPlaceHolder){
            return ItemTypeHDMKeychain;
        }
        if (o instanceof HDAccountCold) {
            return ItemTypeHDAccountCold;
        }
        if (o instanceof EnterpriseHDMSeed) {
            return ItemTypeEnterpriseHDM;
        }
        return ItemTypePrivateKey;
    }

    @Override
    public int getCount() {
        return privates.size() + (hasHDMKeychain() ? 1 : 0) + (hdAccountCold != null ? 1 : 0) +
                (enterpriseHDMSeed != null ? 1 : 0);
    }

    @Override
    public Object getItem(int position) {
        if (position == 0) {
            if (enterpriseHDMSeed != null) {
                return enterpriseHDMSeed;
            }
            if (hdAccountCold != null) {
                return hdAccountCold;
            }
            if (hasHDMKeychain()) {
                return HDMKeychainPlaceHolder;
            }
        }
        if (position == 1) {
            if (enterpriseHDMSeed != null) {
                if (hdAccountCold != null) {
                    return hdAccountCold;
                }
                if (hasHDMKeychain()) {
                    return HDMKeychainPlaceHolder;
                }
            } else {
                if (hasHDMKeychain() && hdAccountCold != null) {
                    return HDMKeychainPlaceHolder;
                }
            }
        }
        if (position == 2) {
            if (enterpriseHDMSeed != null && hasHDMKeychain() && hdAccountCold != null) {
                return HDMKeychainPlaceHolder;
            }
        }
        position = position - getViewTypeCount() + 1;
        return privates.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == ItemTypeEnterpriseHDM) {
            return getViewForEnterpriseHDM(position, convertView, parent);
        }
        if(getItemViewType(position) == ItemTypeHDMKeychain){
            return getViewForHDMKeychain(position, convertView, parent);
        }
        if (getItemViewType(position) == ItemTypeHDAccountCold) {
            return getViewForHDAccountCold(position, convertView, parent);
        }
        return getViewForPrivateKey(position, convertView, parent);
    }

    private View getViewForEnterpriseHDM(int position, View convertView, ViewGroup parent) {
        ColdAddressFragmentHDMEnterpriseListItemView view;
        if (convertView == null || !(convertView instanceof
                ColdAddressFragmentHDMEnterpriseListItemView)) {
            convertView = new ColdAddressFragmentHDMEnterpriseListItemView(activity);
        }
        view = (ColdAddressFragmentHDMEnterpriseListItemView) convertView;
        view.setSeed(enterpriseHDMSeed);
        return convertView;
    }

    private View getViewForHDMKeychain(int position, View convertView, ViewGroup parent){
        ColdAddressFragmentHDMListItemView view;
        if (convertView == null
                || !(convertView instanceof ColdAddressFragmentHDMListItemView)) {
            convertView = new ColdAddressFragmentHDMListItemView(activity, requestHDMServerQrCodeDelegate);
        }
        view = (ColdAddressFragmentHDMListItemView) convertView;
        view.setKeychain(AddressManager.getInstance().getHdmKeychain());
        return convertView;
    }

    private View getViewForHDAccountCold(int position, View convertView, ViewGroup parent) {
        if (convertView == null || !(convertView instanceof
                ColdAddressFragmentHDAccountColdListItemView)) {
            convertView = new ColdAddressFragmentHDAccountColdListItemView(activity);
        }
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

    @Override
    public void notifyDataSetChanged() {
        ItemTypeHDMKeychain = -1;
        ItemTypeHDAccountCold = -1;
        ItemTypeEnterpriseHDM = -1;
        if (hasHDMKeychain()) {
            ItemTypeHDMKeychain = 1;
        }
        if (AddressManager.getInstance().hasHDAccountCold()) {
            ItemTypeHDAccountCold = ItemTypeHDMKeychain == 1 ? 2 : 1;
            hdAccountCold = AddressManager.getInstance().getHDAccountCold();
        } else {
            hdAccountCold = null;
        }
        if (EnterpriseHDMSeed.hasSeed()) {
            ItemTypeEnterpriseHDM = 1;
            if (ItemTypeHDMKeychain >= 0) {
                ItemTypeEnterpriseHDM++;
            }
            if (ItemTypeHDAccountCold >= 0) {
                ItemTypeEnterpriseHDM++;
            }
            enterpriseHDMSeed = EnterpriseHDMSeed.seed();
        } else {
            enterpriseHDMSeed = null;
        }
        super.notifyDataSetChanged();
    }

    private boolean hasHDMKeychain(){
        return AddressManager.getInstance().hasHDMKeychain();
    }
}
