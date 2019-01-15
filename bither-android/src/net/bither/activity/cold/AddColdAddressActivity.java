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

package net.bither.activity.cold;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.hot.AddHotAddressActivity;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.fragment.cold.AddAddressBitpieColdHDAccountFragment;
import net.bither.fragment.cold.AddAddressBitpieColdHDAccountViewFragment;
import net.bither.fragment.cold.AddAddressColdHDAccountFragment;
import net.bither.fragment.cold.AddAddressColdHDAccountViewFragment;
import net.bither.fragment.cold.AddAddressColdOtherFragment;
import net.bither.fragment.hot.AddAddressPrivateKeyFragment;
import net.bither.ui.base.AddPrivateKeyActivity;
import net.bither.util.StringUtil;

import java.util.ArrayList;

public class AddColdAddressActivity extends AddPrivateKeyActivity {
    private ToggleButton tbtnHDAccount;
    private ToggleButton tbtnBitpieHDAccount;
    private ToggleButton tbtnOther;
    private ViewPager pager;
    private ImageButton ibtnCancel;

    private AddAddressBitpieColdHDAccountFragment bitpieHdAccountFragment;
    private AddAddressBitpieColdHDAccountViewFragment bitpieHdAccountViewFragment;
    private AddAddressColdHDAccountFragment hdAccountFragment;
    private AddAddressColdHDAccountViewFragment hdAccountViewFragment;
    private AddAddressColdOtherFragment otherFragment;

    private boolean shouldSuggestCheck = false;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_add_cold_address);
        if (!PasswordSeed.hasPasswordSeed()) {
            shouldSuggestCheck = true;
        } else {
            shouldSuggestCheck = false;
        }
        initView();
    }

    private void initView() {
        tbtnBitpieHDAccount = (ToggleButton) findViewById(R.id.tbtn_bitpie_account);
        tbtnHDAccount = (ToggleButton) findViewById(R.id.tbtn_hd_account);
        tbtnOther = (ToggleButton) findViewById(R.id.tbtn_other);
        pager = (ViewPager) findViewById(R.id.pager);
        ibtnCancel = (ImageButton) findViewById(R.id.ibtn_cancel);
        tbtnBitpieHDAccount.setOnClickListener(new IndicatorClick(0));
        tbtnHDAccount.setOnClickListener(new IndicatorClick(1));
        tbtnOther.setOnClickListener(new IndicatorClick(2));
        ibtnCancel.setOnClickListener(cancelClick);
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(pageChange);
        pager.setCurrentItem(0);
        tbtnBitpieHDAccount.setChecked(true);
    }

    private Fragment getBitpieHDAccountFragment() {
        if (AddressManager.getInstance().hasBitpieHDAccountCold()) {
            if (hdAccountViewFragment == null) {
                bitpieHdAccountViewFragment = new AddAddressBitpieColdHDAccountViewFragment();
            }
            return bitpieHdAccountViewFragment;
        }
        if (bitpieHdAccountFragment == null) {
            bitpieHdAccountFragment = new AddAddressBitpieColdHDAccountFragment();
        }
        return bitpieHdAccountFragment;
    }

    private Fragment getHDAccountFragment() {
        if (AddressManager.getInstance().hasHDAccountCold()) {
            if (hdAccountViewFragment == null) {
                hdAccountViewFragment = new AddAddressColdHDAccountViewFragment();
            }
            return hdAccountViewFragment;
        }
        if (hdAccountFragment == null) {
            hdAccountFragment = new AddAddressColdHDAccountFragment();
        }
        return hdAccountFragment;
    }

    private Fragment getOtherFragment() {
        if (otherFragment == null) {
            otherFragment = new AddAddressColdOtherFragment();
        }
        return otherFragment;
    }

    private OnClickListener cancelClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    };

    public void save() {
        Fragment f = getActiveFragment();
        if (f != null && f instanceof AddHotAddressActivity.AddAddress) {
            AddHotAddressActivity.AddAddress a = (AddHotAddressActivity.AddAddress) f;
            ArrayList<String> addresses = a.getAddresses();
            Intent intent = new Intent();
            intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG, addresses);
            if (f instanceof AddAddressPrivateKeyFragment) {
                intent.putExtra(BitherSetting.INTENT_REF.ADD_PRIVATE_KEY_SUGGEST_CHECK_TAG,
                        shouldSuggestCheck);
            }
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    private class IndicatorClick implements OnClickListener {
        private int position;

        public IndicatorClick(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            initPosition(position);
        }
    }

    private void initPosition(int position) {
        pager.setCurrentItem(position, true);
        if (position == 0) {
            tbtnBitpieHDAccount.setChecked(true);
            tbtnHDAccount.setChecked(false);
            tbtnOther.setChecked(false);
        } else if (position == 1) {
            tbtnBitpieHDAccount.setChecked(false);
            tbtnHDAccount.setChecked(true);
            tbtnOther.setChecked(false);
        } else if (position == 2) {
            tbtnBitpieHDAccount.setChecked(false);
            tbtnHDAccount.setChecked(false);
            tbtnOther.setChecked(true);
        }
    }

    private FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int index) {
            if (index == 0) {
                return getBitpieHDAccountFragment();
            }
            if (index == 1) {
                return getHDAccountFragment();
            }
            if (index == 2) {
                return getOtherFragment();
            }
            return null;
        }
    };

    private ViewPager.OnPageChangeListener pageChange = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int index) {
            if (index == 0) {
                tbtnBitpieHDAccount.setChecked(true);
                tbtnHDAccount.setChecked(false);
                tbtnOther.setChecked(false);
            } else if (index == 1) {
                tbtnBitpieHDAccount.setChecked(false);
                tbtnHDAccount.setChecked(true);
                tbtnOther.setChecked(false);
            } else if (index == 2) {
                tbtnBitpieHDAccount.setChecked(false);
                tbtnHDAccount.setChecked(false);
                tbtnOther.setChecked(true);
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    public Fragment getFragmentAtIndex(int i) {
        String str = StringUtil.makeFragmentName(this.pager.getId(), i);
        return getSupportFragmentManager().findFragmentByTag(str);
    }

    public Fragment getActiveFragment() {
        Fragment localFragment = null;
        if (this.pager == null) {
            return localFragment;
        }
        localFragment = getFragmentAtIndex(pager.getCurrentItem());
        return localFragment;
    }

    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }

}
