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

package net.bither.fragment.hot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.bither.R;
import net.bither.activity.hot.AddHotAddressHDMActivity;
import net.bither.activity.hot.AddHotAddressPrivateKeyActivity;

/**
 * Created by songchenwen on 15/4/16.
 */
public class AddAddressHotOtherFragment extends Fragment implements View.OnClickListener {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_address_hot_other, container, false);
        v.findViewById(R.id.btn_hdm).setOnClickListener(this);
        v.findViewById(R.id.btn_private_key).setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.btn_hdm:
                intent = new Intent(getActivity(), AddHotAddressHDMActivity.class);
                break;
            case R.id.btn_private_key:
                intent = new Intent(getActivity(), AddHotAddressPrivateKeyActivity.class);
                break;
        }
        if (intent == null) {
            return;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        getActivity().startActivity(intent);
        getActivity().finish();
        getActivity().overridePendingTransition(R.anim.activity_in_drop, R.anim.activity_out_back);
    }
}
