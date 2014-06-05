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

package net.bither.fragment.hot;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.bither.activity.hot.AddHotAddressActivity.AddAddress;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.ui.base.AddAddressPrivateKeyView;
import net.bither.ui.base.AddPrivateKeyActivity;

import java.util.ArrayList;

public class AddAddressPrivateKeyFragment extends Fragment implements AddAddress {
    private AddAddressPrivateKeyView v;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = new AddAddressPrivateKeyView((AddPrivateKeyActivity) getActivity());
        return v;
    }

    public ArrayList<String> getAddresses() {
        ArrayList<BitherAddressWithPrivateKey> addresses = v.getAddresses();
        ArrayList<String> as = new ArrayList<String>();
        if (addresses.size() > 0) {
            for (BitherAddressWithPrivateKey address : addresses) {
                as.add(address.getAddress());
            }
        }
        return as;
    }

}
