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

package net.bither.adapter.hot;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import net.bither.BitherSetting;
import net.bither.activity.hot.EnterpriseHDMAddressDetailActivity;
import net.bither.bitherj.core.EnterpriseHDMAddress;
import net.bither.ui.base.AddressFragmentListItemView;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * Created by songchenwen on 15/6/12.
 */
public class EnterpriseHDMKeychainAdapter extends BaseAdapter {
    private List<EnterpriseHDMAddress> addresses;
    private FragmentActivity activity;

    public EnterpriseHDMKeychainAdapter(@Nonnull FragmentActivity activity, @Nonnull
    List<EnterpriseHDMAddress> addresses) {
        this.addresses = addresses;
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return addresses.size();
    }

    @Override
    public EnterpriseHDMAddress getItem(int position) {
        return addresses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getIndex();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AddressFragmentListItemView view;
        if (convertView == null || !(convertView instanceof AddressFragmentListItemView)) {
            convertView = new AddressFragmentListItemView(activity);
        }
        view = (AddressFragmentListItemView) convertView;
        EnterpriseHDMAddress a = getItem(position);
        view.setAddress(a);
        view.setOnClickListener(new AddressDetailClick(a.getIndex()));
        return convertView;
    }

    private class AddressDetailClick implements View.OnClickListener {
        private int position;
        private boolean clicked = false;

        public AddressDetailClick(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            if (!clicked) {
                clicked = true;
                Intent intent = new Intent(activity, EnterpriseHDMAddressDetailActivity.class);
                intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG, position);
                activity.startActivity(intent);
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clicked = false;
                    }
                }, 500);
            }
        }
    }
}
