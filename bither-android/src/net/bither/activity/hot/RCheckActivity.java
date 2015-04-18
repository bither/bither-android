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

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ObjectAnimator;

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.utils.Utils;
import net.bither.model.Check;
import net.bither.ui.base.RCheckHeaderView;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.WrapLayoutParamsForAnimator;
import net.bither.ui.base.dialog.DialogAddressFull;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.CheckUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by songchenwen on 14-10-20.
 */
public class RCheckActivity extends SwipeRightFragmentActivity implements RCheckHeaderView
        .RCheckHeaderViewListener {
    private static final int ListExpandAnimDuration = 500;

    private FrameLayout flContainer;
    private RCheckHeaderView vCheckHeader;
    private FrameLayout fl;
    private ListView lv;

    private int checkCount;
    private int checkFinishedCount;

    private ArrayList<CheckPoint> checkPoints = new ArrayList<CheckPoint>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r_check);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        flContainer = (FrameLayout) findViewById(R.id.fl_container);
        vCheckHeader = (RCheckHeaderView) findViewById(R.id.v_check_header);
        lv = (ListView) findViewById(R.id.lv);
        fl = (FrameLayout) findViewById(R.id.fl);
        lv.setStackFromBottom(false);
        lv.setAdapter(adapter);
        vCheckHeader.setListener(this);
    }

    @Override
    public void beginCheck() {
        final List<Address> addresses = AddressManager.getInstance().getAllAddresses();
        checkPoints.clear();
        final ArrayList<Check> checks = new ArrayList<Check>();
        if (AddressManager.getInstance().hasHDAccount()) {
            HDAccount account = AddressManager.getInstance().getHdAccount();
            CheckPoint point = new CheckPoint(HDAccount.HDAccountPlaceHolder);
            checkPoints.add(point);
            checks.add(CheckUtil.initCheckForRValueOfHD(account).setCheckListener(point));
        }
        for (int i = 0;
             i < addresses.size();
             i++) {
            Address address = addresses.get(i);
            CheckPoint point = new CheckPoint(address.getAddress());
            checkPoints.add(point);
            checks.add(CheckUtil.initCheckForRValue(address).setCheckListener(point));
        }
        adapter.notifyDataSetChanged();
        if (lv.getHeight() <= 0) {
            int lvHeight = flContainer.getHeight() - vCheckHeader.getHeight();
            ObjectAnimator animator = ObjectAnimator.ofInt(new WrapLayoutParamsForAnimator(fl),
                    "height", lvHeight).setDuration(ListExpandAnimDuration);
            animator.addListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    check(checks);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }
            });
            animator.start();
        } else {
            check(checks);
        }
    }


    private void check(final List<Check> checks) {
        checkCount = checks.size();
        checkFinishedCount = 0;
        vCheckHeader.setTotalCheckCount(checkCount);
        vCheckHeader.setPassedCheckCount(0);
        lv.postDelayed(new Runnable() {
            @Override
            public void run() {
                CheckUtil.runChecks(checks, 1);
            }
        }, 600);
    }

    private class CheckPoint implements Check.CheckListener {
        private boolean waiting;
        private boolean checking;
        private boolean result;
        private String address;

        public CheckPoint(String address) {
            waiting = true;
            this.address = address;
        }

        @Override
        public void onCheckBegin(Check check) {
            checking = true;
            waiting = false;
            adapter.notifyDataSetChanged();
            final int index = checkPoints.indexOf(this);
            int itemHeight = lv.getChildAt(0).getHeight();
            int lvHeight = lv.getHeight() - lv.getPaddingBottom() - lv.getPaddingTop();
            lv.setSelectionFromTop(index, lvHeight - itemHeight);
        }

        @Override
        public void onCheckEnd(Check check, boolean success) {
            checking = false;
            result = success;
            checkFinishedCount++;
            if (success) {
                vCheckHeader.addPassedCheckCount();
            }
            adapter.notifyDataSetChanged();
            if (checkFinishedCount >= checkCount) {
                vCheckHeader.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        vCheckHeader.stop();
                    }
                }, 600);
            }
        }

        public boolean isWaiting() {
            return waiting;
        }

        public boolean isChecking() {
            return checking;
        }

        public boolean getResult() {
            return result;
        }

        public String getAddress() {
            return address;
        }
    }

    public void check() {
        vCheckHeader.check();
    }

    private BaseAdapter adapter = new BaseAdapter() {
        private ColorStateList titleNormalColor;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder h;
            if (convertView == null) {
                convertView = LayoutInflater.from(RCheckActivity.this).inflate(R.layout
                        .list_item_check, null);
                h = new ViewHolder(convertView);
            } else {
                h = (ViewHolder) convertView.getTag();
            }
            CheckPoint point = getItem(position);
            if(titleNormalColor == null){
                titleNormalColor = h.tv.getTextColors();
            }

            h.tv.setTextColor(titleNormalColor);
            h.tv.setText(getSpannableStringFromAddress(point.getAddress(), true));
            h.ibtnFull.setOnClickListener(new AddressFullClick(point.getAddress()));
            if (Utils.compareString(point.getAddress(), HDAccount.HDAccountPlaceHolder)) {
                h.ibtnFull.setVisibility(View.GONE);
            } else {
                h.ibtnFull.setVisibility(View.VISIBLE);
            }
            if (point.isWaiting()) {
                h.pb.setVisibility(View.GONE);
                h.iv.setVisibility(View.GONE);
            } else {
                if (point.isChecking()) {
                    h.pb.setVisibility(View.VISIBLE);
                    h.iv.setVisibility(View.GONE);
                } else {
                    h.iv.setVisibility(View.VISIBLE);
                    h.pb.setVisibility(View.GONE);
                    if (point.getResult()) {
                        h.iv.setImageResource(R.drawable.checkmark);
                    } else {
                        h.iv.setImageBitmap(null);
                        h.iv.setVisibility(View.GONE);
                        h.tv.setText(getSpannableStringFromAddress(point.getAddress(), false));
                        h.tv.setTextColor(getResources().getColor(R.color.red));
                    }
                }
            }
            return convertView;
        }

        private SpannableString getSpannableStringFromAddress(String address, boolean safe) {
            if (Utils.compareString(address, HDAccount.HDAccountPlaceHolder)) {
                int resource = R.string.rcheck_address_title;
                if (!safe) {
                    resource = R.string.rcheck_address_danger_title;
                }
                String str = String.format(getString(resource), getString(R.string
                        .address_group_hd));
                return new SpannableString(str);
            } else {
                address = Utils.shortenAddress(address);
                String a = address.substring(0, 4);
                int resource = R.string.rcheck_address_title;
                if (!safe) {
                    resource = R.string.rcheck_address_danger_title;
                }
                String str = String.format(getString(resource), address);
                int indexOfAddress = str.indexOf(a);
                SpannableString spannable = new SpannableString(str);
                spannable.setSpan(new TypefaceSpan("monospace"), indexOfAddress, indexOfAddress +
                        4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                return spannable;
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public CheckPoint getItem(int position) {
            return checkPoints.get(position);
        }

        @Override
        public int getCount() {
            return checkPoints.size();
        }

        class ViewHolder {
            TextView tv;
            ProgressBar pb;
            ImageView iv;
            ImageButton ibtnFull;

            public ViewHolder(View v) {
                tv = (TextView) v.findViewById(R.id.tv_check_title);
                pb = (ProgressBar) v.findViewById(R.id.pb_check);
                iv = (ImageView) v.findViewById(R.id.iv_check_state);
                ibtnFull = (ImageButton) v.findViewById(R.id.ibtn_address_full);
                v.setTag(this);
            }
        }

        class AddressFullClick implements View.OnClickListener {
            private String address;

            public AddressFullClick(String address) {
                this.address = address;
            }

            @Override
            public void onClick(View v) {
                LinkedHashMap<String, Long> map = new LinkedHashMap<String, Long>();
                map.put(address, 0L);
                DialogAddressFull dialog = new DialogAddressFull(RCheckActivity.this, map);
                dialog.show(v);
            }
        }
    };
}
