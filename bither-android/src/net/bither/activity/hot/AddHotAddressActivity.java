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
import net.bither.fragment.hot.AddAddressPrivateKeyFragment;
import net.bither.fragment.hot.AddAddressWatchOnlyFragment;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.AddPrivateKeyActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;

import java.util.ArrayList;

public class AddHotAddressActivity extends AddPrivateKeyActivity {
    private ToggleButton tbtnWatchOnly;
    private ToggleButton tbtnPrivateKey;
    private ViewPager pager;
    private ImageButton ibtnCancel;

    private AddAddressWatchOnlyFragment vWatchOnly;
    private AddAddressPrivateKeyFragment vPrivateKey;

    private boolean shouldSuggestCheck = false;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_add_hot_address);
        if (AppSharedPreference.getInstance().getPasswordSeed() == null) {
            shouldSuggestCheck = true;
        } else {
            shouldSuggestCheck = false;
        }
        initView();
    }

    private void initView() {
        tbtnPrivateKey = (ToggleButton) findViewById(R.id.tbtn_private_key);
        tbtnWatchOnly = (ToggleButton) findViewById(R.id.tbtn_watch_only);
        pager = (ViewPager) findViewById(R.id.pager);
        ibtnCancel = (ImageButton) findViewById(R.id.ibtn_cancel);
        tbtnPrivateKey.setOnClickListener(new IndicatorClick(0));
        tbtnWatchOnly.setOnClickListener(new IndicatorClick(1));
        ibtnCancel.setOnClickListener(cancelClick);
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(pageChange);
        pager.setCurrentItem(0);
        if (adapter.getCount() > 1) {
            tbtnPrivateKey.setChecked(true);
        } else {
            if (WalletUtils.isPrivateLimit()) {
                tbtnWatchOnly.setChecked(true);
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
        if (position == 1) {
            if (WalletUtils.isWatchOnlyLimit()) {
                DropdownMessage.showDropdownMessage(AddHotAddressActivity.this,
                        R.string.watch_only_address_count_limit);
                tbtnWatchOnly.setChecked(false);
                tbtnPrivateKey.setChecked(true);
            } else {
                if (tbtnPrivateKey.isChecked()) {
                    pager.setCurrentItem(adapter.getCount() - 1, true);
                }
                tbtnWatchOnly.setChecked(true);
                tbtnPrivateKey.setChecked(false);
            }
        } else {
            if (WalletUtils.isPrivateLimit()) {
                DropdownMessage.showDropdownMessage(AddHotAddressActivity.this,
                        R.string.private_key_count_limit);
                tbtnWatchOnly.setChecked(true);
                tbtnPrivateKey.setChecked(false);
            } else {
                if (tbtnWatchOnly.isChecked()) {
                    pager.setCurrentItem(0, true);
                }
                tbtnWatchOnly.setChecked(false);
                tbtnPrivateKey.setChecked(true);
            }
        }
    }

    private FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {

        @Override
        public int getCount() {
            int count = 0;
            if (!WalletUtils.isPrivateLimit()) {
                count++;
            }
            if (!WalletUtils.isWatchOnlyLimit()) {
                count++;
            }
            return count;
        }

        @Override
        public Fragment getItem(int index) {
            if (getCount() > 1) {
                switch (index) {
                    case 0:
                        return getPrivateKeyView();
                    case 1:
                        return getWatchOnlyView();
                    default:
                        return null;
                }
            } else if (getCount() == 1) {
                if (index == 0) {
                    if (!WalletUtils.isPrivateLimit()) {
                        return getPrivateKeyView();
                    } else if (!WalletUtils.isWatchOnlyLimit()) {
                        return getWatchOnlyView();
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    };

    private OnPageChangeListener pageChange = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int index) {
            if (adapter.getCount() > 1) {
                if (index == 0) {
                    tbtnPrivateKey.setChecked(true);
                    tbtnWatchOnly.setChecked(false);
                } else {
                    tbtnWatchOnly.setChecked(true);
                    tbtnPrivateKey.setChecked(false);
                }
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

    ;

    public static interface AddAddress {
        public ArrayList<String> getAddresses();
    }
}
