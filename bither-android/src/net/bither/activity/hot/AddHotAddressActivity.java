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

package net.bither.activity.hot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.fragment.hot.AddAddressHotHDMFragment;
import net.bither.fragment.hot.AddAddressPrivateKeyFragment;
import net.bither.fragment.hot.AddAddressWatchOnlyFragment;
import net.bither.ui.base.AddPrivateKeyActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.util.StringUtil;

import java.util.ArrayList;

public class AddHotAddressActivity extends AddPrivateKeyActivity {
    private ToggleButton tbtnWatchOnly;
    private ToggleButton tbtnPrivateKey;
    private ToggleButton tbtnHDM;
    private ViewPager pager;
    private ImageButton ibtnCancel;

    private AddAddressWatchOnlyFragment vWatchOnly;
    private AddAddressPrivateKeyFragment vPrivateKey;
    private AddAddressHotHDMFragment vHDM;

    private boolean privateLimit;
    private boolean watchOnlyLimit;
    private boolean hdmLimit;

    private boolean shouldSuggestCheck = false;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_add_hot_address);
        if (!PasswordSeed.hasPasswordSeed()) {
            shouldSuggestCheck = true;
        } else {
            shouldSuggestCheck = false;
        }
        privateLimit = AddressManager.isPrivateLimit();
        watchOnlyLimit = AddressManager.isWatchOnlyLimit();
        hdmLimit = false;
        initView();
    }

    private void initView() {
        tbtnPrivateKey = (ToggleButton) findViewById(R.id.tbtn_private_key);
        tbtnWatchOnly = (ToggleButton) findViewById(R.id.tbtn_watch_only);
        tbtnHDM = (ToggleButton) findViewById(R.id.tbtn_hdm);
        pager = (ViewPager) findViewById(R.id.pager);
        ibtnCancel = (ImageButton) findViewById(R.id.ibtn_cancel);
        tbtnPrivateKey.setOnClickListener(new IndicatorClick(0));
        tbtnWatchOnly.setOnClickListener(new IndicatorClick(1));
        tbtnHDM.setOnClickListener(new IndicatorClick(2));
        ibtnCancel.setOnClickListener(cancelClick);
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(pageChange);
        pager.setCurrentItem(0);
        if (adapter.getCount() > 2) {
            tbtnPrivateKey.setChecked(true);
        } else {
            if (privateLimit) {
                if (!watchOnlyLimit) {
                    tbtnWatchOnly.setChecked(true);
                } else {
                    tbtnHDM.setChecked(true);
                }
            } else {
                tbtnPrivateKey.setChecked(true);
            }
        }
    }

    private AddAddressWatchOnlyFragment getWatchOnlyView() {
        if (vWatchOnly == null) {
            vWatchOnly = new AddAddressWatchOnlyFragment();
        }
        return vWatchOnly;
    }

    private AddAddressPrivateKeyFragment getPrivateKeyView() {
        if (vPrivateKey == null) {
            vPrivateKey = new AddAddressPrivateKeyFragment();
        }
        return vPrivateKey;
    }

    private AddAddressHotHDMFragment getHDMView() {
        if (vHDM == null) {
            vHDM = new AddAddressHotHDMFragment();
        }
        return vHDM;
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
        if (f != null && f instanceof AddAddress) {
            AddAddress a = (AddAddress) f;
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
        if (position == 0) {
            if (privateLimit) {
                DropdownMessage.showDropdownMessage(AddHotAddressActivity.this,
                        R.string.private_key_count_limit);
                tbtnPrivateKey.setChecked(false);
            } else {
                if (tbtnWatchOnly.isChecked() || tbtnHDM.isChecked()) {
                    pager.setCurrentItem(0, true);
                }
                tbtnHDM.setChecked(false);
                tbtnWatchOnly.setChecked(false);
                tbtnPrivateKey.setChecked(true);
            }
        } else if (position == 1) {
            if (watchOnlyLimit) {
                DropdownMessage.showDropdownMessage(AddHotAddressActivity.this,
                        R.string.watch_only_address_count_limit);
                tbtnWatchOnly.setChecked(false);
            } else {
                if (tbtnPrivateKey.isChecked() || tbtnHDM.isChecked()) {
                    pager.setCurrentItem(getWatchOnlyIndex(), true);
                }
                tbtnWatchOnly.setChecked(true);
                tbtnPrivateKey.setChecked(false);
                tbtnHDM.setChecked(false);
            }
        } else if (position == 2) {
            if (hdmLimit) {
                DropdownMessage.showDropdownMessage(AddHotAddressActivity.this,
                        R.string.hdm_cold_seed_count_limit);
                tbtnHDM.setChecked(false);
            } else {
                if (tbtnPrivateKey.isChecked() || tbtnWatchOnly.isChecked()) {
                    pager.setCurrentItem(getHDMIndex(), true);
                }
                tbtnHDM.setChecked(true);
                tbtnWatchOnly.setChecked(false);
                tbtnPrivateKey.setChecked(false);
            }
        }
    }

    private FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {

        @Override
        public int getCount() {
            int count = 0;
            if (!privateLimit) {
                count++;
            }
            if (!watchOnlyLimit) {
                count++;
            }
            if (!hdmLimit) {
                count++;
            }
            return count;
        }

        @Override
        public Fragment getItem(int index) {
            if (index == getPrivateIndex()) {
                return getPrivateKeyView();
            }
            if (index == getWatchOnlyIndex()) {
                return getWatchOnlyView();
            }
            if (index == getHDMIndex()) {
                return getHDMView();
            }
            return null;
        }
    };

    private int getPrivateIndex() {
        if (privateLimit) {
            return -1;
        }
        return 0;
    }

    private int getWatchOnlyIndex() {
        if (watchOnlyLimit) {
            return -1;
        }
        int index = 0;
        if (!privateLimit) {
            index++;
        }
        return index;
    }

    private int getHDMIndex() {
        if (hdmLimit) {
            return -1;
        }
        int index = 0;
        if (!privateLimit) {
            index++;
        }
        if (!watchOnlyLimit) {
            index++;
        }
        return index;
    }

    private OnPageChangeListener pageChange = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int index) {
            if (index == getPrivateIndex()) {
                tbtnPrivateKey.setChecked(true);
                tbtnWatchOnly.setChecked(false);
                tbtnHDM.setChecked(false);
                return;
            }
            if (index == getWatchOnlyIndex()) {
                tbtnPrivateKey.setChecked(false);
                tbtnWatchOnly.setChecked(true);
                tbtnHDM.setChecked(false);
                return;
            }
            if (index == getHDMIndex()) {
                tbtnPrivateKey.setChecked(false);
                tbtnWatchOnly.setChecked(false);
                tbtnHDM.setChecked(true);
                return;
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
        if (vHDM != null && !vHDM.canCancel()) {
            DropdownMessage.showDropdownMessage(this, R.string.hdm_singular_mode_cancel_warn);
            return;
        }
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }

    public static interface AddAddress {
        public ArrayList<String> getAddresses();
    }
}
