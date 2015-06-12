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

package net.bither.activity.hot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import net.bither.R;
import net.bither.adapter.hot.EnterpriseHDMKeychainAdapter;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.EnterpriseHDMAddress;
import net.bither.bitherj.core.EnterpriseHDMKeychain;
import net.bither.ui.base.SwipeRightFragmentActivity;

import java.util.ArrayList;

/**
 * Created by songchenwen on 15/6/9.
 */
public class EnterpriseHDMKeychainActivity extends SwipeRightFragmentActivity {
    private static final int AddCode = 1123;

    private ArrayList<EnterpriseHDMAddress> addresses = new ArrayList<EnterpriseHDMAddress>();

    private ListView lv;
    private EnterpriseHDMKeychainAdapter adapter;
    private EnterpriseHDMKeychain keychain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enterprise_hdm_keychain);
        initView();
        keychain = AddressManager.getInstance().getEnterpriseHDMKeychain();
        if (keychain == null) {
            startActivityForResult(new Intent(this, AddEnterpriseHDMKeychainActivity.class),
                    AddCode);
            return;
        }
    }

    private void initView() {
        lv = (ListView) findViewById(R.id.lv);
        adapter = new EnterpriseHDMKeychainAdapter(this, addresses);
        lv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (keychain != null) {
            addresses.clear();
            addresses.addAll(keychain.getAddresses());
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AddCode) {
            if (resultCode != RESULT_OK) {
                finish();
                return;
            }
            keychain = AddressManager.getInstance().getEnterpriseHDMKeychain();
            if (keychain == null) {
                finish();
                return;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
