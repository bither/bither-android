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

import android.os.Bundle;
import android.view.View;

import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccountMonitored;
import net.bither.bitherj.utils.Utils;

/**
 * Created by songchenwen on 15/4/17.
 */
public class HDAccountMonitoredDetailActivity extends AddressDetailActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.ibtn_option).setVisibility(View.GONE);
    }

    @Override
    protected void initAddress() {
        address = AddressManager.getInstance().getHdAccountMonitored();
        addressPosition = 0;
    }

    @Override
    protected void optionClicked() {

    }

    @Override
    protected void notifyAddressBalanceChange(String address) {
        if (Utils.compareString(address, HDAccountMonitored.HDAccountMonitoredPlaceHolder)) {
            loadData();
        }
    }
}
