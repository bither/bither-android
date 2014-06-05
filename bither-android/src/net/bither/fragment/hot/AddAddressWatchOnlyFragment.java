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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.bither.activity.hot.AddHotAddressActivity.AddAddress;
import net.bither.ui.base.AddAddressWatchOnlyView;
import net.bither.ui.base.AddPrivateKeyActivity;

import java.util.ArrayList;

public class AddAddressWatchOnlyFragment extends Fragment implements AddAddress {
    private AddAddressWatchOnlyView v;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = new AddAddressWatchOnlyView((AddPrivateKeyActivity) getActivity(), this);
        return v;
    }

    public ArrayList<String> getAddresses() {
        return v.getAddresses();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!v.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
