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

package net.bither.ui.base;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.utils.Utils;

public class SettingSelectorView extends FrameLayout implements OnClickListener {
    public static interface SettingSelector {
        public int getOptionCount();

        public String getOptionName(int index);

        public String getOptionNote(int index);

        public Drawable getOptionDrawable(int index);

        public String getSettingName();

        public int getCurrentOptionIndex();

        public void onOptionIndexSelected(int index);
    }

    private LinearLayout llSetting;
    private LinearLayout llOptions;
    private TextView tvSettingName;
    private TextView tvCurrentOption;
    private SettingSelector selector;

    private LayoutInflater inflater;

    private int topSpace;
    private int bottomSpace;

    private int preTopSpace;
    private int preSettingBottomSpace;
    private int preOptionBottomSpace;

    public SettingSelectorView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        removeAllViews();
        inflater = LayoutInflater.from(getContext());
        addView(inflater.inflate(R.layout.layout_setting_selector, null),
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        llSetting = (LinearLayout) findViewById(R.id.ll_setting);
        llOptions = (LinearLayout) findViewById(R.id.ll_options);
        tvSettingName = (TextView) findViewById(R.id.tv_setting_name);
        tvCurrentOption = (TextView) findViewById(R.id.tv_setting_current);
        llOptions.setVisibility(View.GONE);
        llSetting.setOnClickListener(this);
        preTopSpace = llSetting.getPaddingTop();
        preSettingBottomSpace = llSetting.getPaddingBottom();
        llSetting.setPadding(llSetting.getPaddingLeft(), preTopSpace + topSpace,
                llSetting.getPaddingRight(), llSetting.getPaddingBottom());
    }

    public SettingSelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public SettingSelectorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public void setTopSpace(int topSpace) {
        this.topSpace = topSpace;
        llSetting.setPadding(llSetting.getPaddingLeft(), topSpace + preTopSpace,
                llSetting.getPaddingRight(), llSetting.getPaddingBottom());
    }

    public void setBottomSpace(int bottomSpace) {
        this.bottomSpace = bottomSpace;
        llSetting.setPadding(llSetting.getPaddingLeft(), llSetting.getPaddingTop(),
                llSetting.getPaddingRight(), preSettingBottomSpace + bottomSpace);
        if (llOptions.getChildCount() > 0) {
            View v = llOptions.getChildAt(llOptions.getChildCount() - 1);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    preOptionBottomSpace + bottomSpace);
        }
    }

    public SettingSelector getSelector() {
        return selector;
    }

    public void setSelector(SettingSelector selector) {
        this.selector = selector;
        loadData();
    }

    public void loadData() {
        tvSettingName.setText(selector.getSettingName());
        if (selector.getCurrentOptionIndex() >= 0 && selector.getCurrentOptionIndex() < selector
                .getOptionCount()) {
            tvCurrentOption.setText(selector.getOptionName(selector.getCurrentOptionIndex()));
        } else {
            tvCurrentOption.setText("");
        }
        llOptions.removeAllViews();
        if (selector != null) {
            int count = selector.getOptionCount();
            for (int i = 0;
                 i < count;
                 i++) {
                View v = inflater.inflate(R.layout.list_item_setting_option, null);
                llOptions.addView(v, LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                TextView tv = (TextView) v.findViewById(R.id.tv_option_name);
                TextView tvNote = (TextView) v.findViewById(R.id.tv_option_note);
                ImageView iv = (ImageView) v.findViewById(R.id.iv_check);
                tv.setText(selector.getOptionName(i));
                String note = selector.getOptionNote(i);
                if (Utils.isEmpty(note)) {
                    tvNote.setText("");
                } else {
                    tvNote.setText(note);
                }
                tvNote.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        selector.getOptionDrawable(i), null);
                if (i == selector.getCurrentOptionIndex()) {
                    iv.setVisibility(View.VISIBLE);
                } else {
                    iv.setVisibility(View.GONE);
                }
                v.setOnClickListener(new OptionClick(i));
                if (i == count - 1) {
                    preOptionBottomSpace = v.getPaddingBottom();
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                            preOptionBottomSpace + bottomSpace);
                }
            }
        }
    }

    private class OptionClick implements OnClickListener {
        private int index;

        public OptionClick(int index) {
            this.index = index;
        }

        @Override
        public void onClick(View v) {
            if (selector != null) {
                if (index != selector.getCurrentOptionIndex()) {
                    selector.onOptionIndexSelected(index);
                }
            }
            loadData();
        }
    }

    @Override
    public void onClick(View v) {
        if (llOptions.getVisibility() == View.VISIBLE) {
            llOptions.setVisibility(View.GONE);
            if (bottomSpace != 0) {
                llSetting.setPadding(llSetting.getPaddingLeft(), llSetting.getPaddingTop(),
                        llSetting.getPaddingRight(), preSettingBottomSpace + bottomSpace);
            }
        } else {
            llOptions.setVisibility(View.VISIBLE);
            if (bottomSpace != 0) {
                llSetting.setPadding(llSetting.getPaddingLeft(), llSetting.getPaddingTop(),
                        llSetting.getPaddingRight(), preSettingBottomSpace);
            }
        }
    }

}
