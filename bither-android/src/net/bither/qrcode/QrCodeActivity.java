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

package net.bither.qrcode;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.fragment.QrCodeFragment;
import net.bither.fragment.QrCodeFragment.QrCodeFragmentDelegate;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.StringUtil;

import java.util.List;

public class QrCodeActivity extends SwipeRightFragmentActivity {
    private List<String> contents;

    private ViewPager pager;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);
        Intent intent = getIntent();
        String codeString = null;
        if (intent != null && intent.getExtras() != null) {
            codeString = intent.getExtras().getString(BitherSetting.INTENT_REF.QR_CODE_STRING);
        }
        if (StringUtil.isEmpty(codeString)) {
            super.finish();
            overridePendingTransition(0, 0);
        } else {
            contents = QRCodeUtil.getQrCodeStringList(QRCodeUtil.encodeQrCodeString(codeString));
            initView();
            String title = getTitleString();
            if (!StringUtil.isEmpty(title)) {
                tvTitle.setText(title);
            }
        }
    }

    private void initView() {
        findViewById(R.id.ibtn_cancel).setOnClickListener(new IBackClickListener());
        pager = (ViewPager) findViewById(R.id.pager);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        mTouchView.addIgnoreView(pager);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
    }

    protected void complete() {
        super.finish();
    }

    protected String getCompleteButtonTitle() {
        return getString(R.string.complete);
    }

    protected String getTitleString() {
        String title = null;
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            title = intent.getExtras().getString(BitherSetting.INTENT_REF.TITLE_STRING);
        }
        return title;
    }

    private class FragmentDelegate implements QrCodeFragmentDelegate {
        private int index;

        public FragmentDelegate(int index) {
            this.index = index;
        }

        @Override
        public String getContent() {
            return contents.get(index);
        }

        @Override
        public void btnPressed() {
            if (pageIndex() < pageCount() - 1) {
                pager.setCurrentItem(pager.getCurrentItem() + 1, true);
            } else {
                complete();
            }
        }

        @Override
        public String getButtonTitle() {
            if (pageIndex() < pageCount() - 1) {
                return getString(R.string.next_page);
            } else {
                return getCompleteButtonTitle();
            }
        }

        @Override
        public int pageIndex() {
            return index;
        }

        @Override
        public int pageCount() {
            return contents.size();
        }
    }

    private FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {

        @Override
        public int getCount() {
            return contents.size();
        }

        @Override
        public Fragment getItem(int index) {
            QrCodeFragment f = new QrCodeFragment();
            f.setDelegate(new FragmentDelegate(index));
            return f;
        }
    };
}
