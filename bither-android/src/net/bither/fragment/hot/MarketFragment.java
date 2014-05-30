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

package net.bither.fragment.hot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.adapter.hot.MarketFragmentListAdapter;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.fragment.Unselectable;
import net.bither.model.Market;
import net.bither.ui.base.MarketFragmentListItemView;
import net.bither.ui.base.MarketListHeader;
import net.bither.ui.base.MarketTickerChangedObserver;
import net.bither.util.BroadcastUtil;
import net.bither.util.MarketUtil;

import java.util.List;

public class MarketFragment extends Fragment implements Refreshable,
        Selectable, Unselectable, OnItemClickListener {
    private View v;
    private List<Market> markets;
    private MarketListHeader header;
    private ImageView ivMarketPriceAnimIcon;
    private ListView lv;
    private MarketFragmentListAdapter mAdaper;

    private SelectedThread selectedThread;

    private IntentFilter broadcastIntentFilter = new IntentFilter(
            BroadcastUtil.ACTION_MARKET);
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            header.onMarketTickerChanged();
            int itemCount = lv.getChildCount();
            for (int i = 0;
                 i < itemCount;
                 i++) {
                View v = lv.getChildAt(i);
                if (v instanceof MarketTickerChangedObserver) {
                    MarketTickerChangedObserver o = (MarketTickerChangedObserver) v;
                    o.onMarketTickerChanged();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        markets = MarketUtil.getMarkets();
        mAdaper = new MarketFragmentListAdapter(getActivity(), markets);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater
                .inflate(R.layout.fragment_market, container, false);
        header = (MarketListHeader) v.findViewById(R.id.v_header);
        lv = (ListView) v.findViewById(R.id.lv);
        ivMarketPriceAnimIcon = (ImageView) v.findViewById(R.id.iv_market_price_anim_icon);
        lv.setAdapter(mAdaper);
        lv.setOnItemClickListener(this);
        return v;
    }

    public void onResume() {
        super.onResume();
        header.onResume();
        int listItemCount = lv.getChildCount();
        for (int i = 0;
             i < listItemCount;
             i++) {
            View v = lv.getChildAt(i);
            if (v instanceof MarketFragmentListItemView) {
                MarketFragmentListItemView av = (MarketFragmentListItemView) v;
                av.onResume();
            }
        }
        getActivity()
                .registerReceiver(broadcastReceiver, broadcastIntentFilter);
    }

    @Override
    public void onPause() {
        header.onPause();
        int listItemCount = lv.getChildCount();
        for (int i = 0;
             i < listItemCount;
             i++) {
            View v = lv.getChildAt(i);
            if (v instanceof MarketFragmentListItemView) {
                MarketFragmentListItemView av = (MarketFragmentListItemView) v;
                av.onPause();
            }
        }
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        header.setMarket(markets.get(arg2));
    }

    @Override
    public void onSelected() {
        if (lv != null) {
            header.onResume();
            int listItemCount = lv.getChildCount();
            for (int i = 0;
                 i < listItemCount;
                 i++) {
                View v = lv.getChildAt(i);
                if (v instanceof MarketFragmentListItemView) {
                    MarketFragmentListItemView av = (MarketFragmentListItemView) v;
                    av.onResume();
                }
            }
        } else {
            if (selectedThread == null || !selectedThread.isAlive()) {
                selectedThread = new SelectedThread();
                selectedThread.start();
            }
        }
    }

    public void showPriceAlertAnimTo(int fromX, int fromY, Market toMarket) {
        int[] containerOffset = new int[2];
        v.getLocationInWindow(containerOffset);
        containerOffset[0] += v.getPaddingLeft();
        containerOffset[1] += v.getPaddingTop();
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) ivMarketPriceAnimIcon
                .getLayoutParams();
        lp.topMargin = fromY - containerOffset[1];
        lp.leftMargin = fromX - containerOffset[0];
        int marketIndex = markets.indexOf(toMarket);
        if (marketIndex < lv.getFirstVisiblePosition() && marketIndex > lv.getLastVisiblePosition
                ()) {
            lv.setSelection(marketIndex);
        }
        View marketView = lv.getChildAt(marketIndex - lv.getFirstVisiblePosition());
        View toIconView = marketView.findViewById(R.id.iv_price_alert);
        int[] toLocation = new int[2];
        toIconView.getLocationInWindow(toLocation);
        TranslateAnimation anim = new TranslateAnimation(0, toLocation[0] - fromX, 0,
                toLocation[1] - fromY);
        anim.setDuration(300);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                doRefresh();
                ivMarketPriceAnimIcon.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        ivMarketPriceAnimIcon.setVisibility(View.VISIBLE);
        ivMarketPriceAnimIcon.startAnimation(anim);
    }

    @Override
    public void doRefresh() {
        if (lv == null) {
            return;
        }
        refresh();
    }

    public void refresh() {
        if (mAdaper != null) {
            mAdaper.notifyDataSetChanged();
        }
    }

    @Override
    public void onUnselected() {
        header.reset();
    }

    public void notifPriceAlert(BitherSetting.MarketType marketType) {
        Market market = MarketUtil.getMarket(marketType);
        header.setMarket(market);
    }

    private class SelectedThread extends Thread {
        @Override
        public void run() {
            for (int i = 0;
                 i < 20;
                 i++) {
                if (lv != null) {
                    header.onResume();
                    int listItemCount = lv.getChildCount();
                    for (int j = 0;
                         j < listItemCount;
                         j++) {
                        View v = lv.getChildAt(i);
                        if (v instanceof MarketFragmentListItemView) {
                            MarketFragmentListItemView av = (MarketFragmentListItemView) v;
                            av.onResume();
                        }
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
