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

package net.bither.rawprivatekey;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ToggleButton;

import net.bither.R;
import net.bither.ui.base.OverScrollableViewPager;
import net.bither.ui.base.listener.IBackClickListener;

/**
 * Created by songchenwen on 14/12/4.
 */
public class RawPrivateKeyActivity extends FragmentActivity implements View.OnClickListener,
        ViewPager.OnPageChangeListener {
    private ToggleButton tbtnDice;
    private ToggleButton tbtnBinary;
    private OverScrollableViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_add_raw_private_key);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        pager = (OverScrollableViewPager) findViewById(R.id.pager);
        tbtnDice = (ToggleButton) findViewById(R.id.tbtn_dice);
        tbtnBinary = (ToggleButton) findViewById(R.id.tbtn_binary);
        pager.setAdapter(adapter);
        pager.setCurrentItem(0);
        tbtnDice.setChecked(true);
        tbtnBinary.setChecked(false);
        tbtnDice.setOnClickListener(this);
        tbtnBinary.setOnClickListener(this);
        pager.setOnPageChangeListener(this);
    }

    private FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new RawPrivateKeyBinaryFragment();
            } else {
                return new RawPrivateKeyBinaryFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tbtn_dice:
                if (pager.getCurrentItem() != 0) {
                    pager.setCurrentItem(0, true);
                    tbtnDice.setChecked(true);
                    tbtnBinary.setChecked(false);
                }
                break;
            case R.id.tbtn_binary:
                if (pager.getCurrentItem() != 1) {
                    pager.setCurrentItem(1, true);
                    tbtnDice.setChecked(false);
                    tbtnBinary.setChecked(true);
                }
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (position == 0) {
            tbtnDice.setChecked(true);
            tbtnBinary.setChecked(false);
        } else {
            tbtnDice.setChecked(false);
            tbtnBinary.setChecked(true);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
