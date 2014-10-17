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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.utils.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.QrCodeFragment;
import net.bither.fragment.QrCodeFragment.QrCodeFragmentDelegate;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogQRCodeOption;
import net.bither.ui.base.listener.IBackClickListener;

import java.util.ArrayList;
import java.util.List;

public class BitherQRCodeActivity extends SwipeRightFragmentActivity implements DialogQRCodeOption.ISwitchQRCode {
    private List<String> contents;

    private ViewPager pager;
    private TextView tvTitle;
    private ImageView ivSeparator;
    private ImageButton btnSwitch;
    private boolean isNewVerion;
    private boolean hasOldQRCode;
    private List<String> oldContents;
    private QRFragmentPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);
        Intent intent = getIntent();
        String codeString = null;
        String oldString = null;
        if (intent != null && intent.getExtras() != null) {
            codeString = intent.getExtras().getString(BitherSetting.INTENT_REF.QR_CODE_STRING);
            oldString = intent.getExtras().getString(BitherSetting.INTENT_REF.OLD_QR_CODE_STRING);
        }
        hasOldQRCode = !Utils.isEmpty(oldString);
        if (hasOldQRCode) {
            this.oldContents = QRCodeUtil.getQrCodeStringList(QRCodeEnodeUtil.oldEncodeQrCodeString(oldString));
        }
        if (Utils.isEmpty(codeString)) {
            super.finish();
            overridePendingTransition(0, 0);
        } else {
            isNewVerion = true;
            this.contents = QRCodeUtil.getQrCodeStringList(QRCodeUtil.encodeQrCodeString(codeString));
            initView();
            String title = getTitleString();
            if (!Utils.isEmpty(title)) {
                tvTitle.setText(title);
            }
        }
    }

    private void initView() {
        findViewById(R.id.ibtn_cancel).setOnClickListener(new IBackClickListener());
        ivSeparator = (ImageView) findViewById(R.id.iv_separator);
        btnSwitch = (ImageButton) findViewById(R.id.ibtn_switch);
        btnSwitch.setOnClickListener(switchClickListener);
        pager = (ViewPager) findViewById(R.id.pager);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        mTouchView.addIgnoreView(pager);
        if (hasOldQRCode) {
            ivSeparator.setVisibility(View.VISIBLE);
            btnSwitch.setVisibility(View.VISIBLE);
        } else {
            ivSeparator.setVisibility(View.INVISIBLE);
            btnSwitch.setVisibility(View.INVISIBLE);
        }
        adapter = new QRFragmentPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
        refresh();

    }

    private void refresh() {
        boolean firestIn = pageContentList.size() == 0;
        if (isNewVerion) {
            pageContentList.clear();
            pageContentList.addAll(this.contents);
            if (!firestIn) {
                DropdownMessage.showDropdownMessage(BitherQRCodeActivity.this, R.string.use_new_version_qrcode);
            }
        } else {
            pageContentList.clear();
            pageContentList.addAll(this.oldContents);

            if (!firestIn) {
                DropdownMessage.showDropdownMessage(BitherQRCodeActivity.this, R.string.use_old_version_qrcode);
            }
        }

        adapter.notifyDataSetChanged();


    }

    private View.OnClickListener switchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            new DialogQRCodeOption(BitherQRCodeActivity.this, BitherQRCodeActivity.this).show();
        }
    };

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
        private List<String> mContents;

        public FragmentDelegate(List<String> paramContents, int index) {
            this.mContents = paramContents;
            this.index = index;
        }

        @Override
        public String getContent() {
            return this.mContents.get(index);
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
            return this.mContents.size();
        }
    }

    private List<String> pageContentList = new ArrayList<String>();

    private class QRFragmentPagerAdapter extends FragmentPagerAdapter {

        public QRFragmentPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return pageContentList.size();
        }

        @Override
        public Fragment getItem(int index) {
            QrCodeFragment f = new QrCodeFragment();
            f.setDelegate(new FragmentDelegate(pageContentList, index));
            return f;
        }

    }

    @Override
    public void switchQRCode() {
        isNewVerion = !isNewVerion;
        refresh();

    }
}
