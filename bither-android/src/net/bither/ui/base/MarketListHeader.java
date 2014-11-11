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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.hot.MarketDetailActivity;
import net.bither.fragment.hot.MarketFragment;
import net.bither.model.Market;
import net.bither.model.PriceAlert;
import net.bither.model.Ticker;
import net.bither.preference.AppSharedPreference;
import net.bither.util.ExchangeUtil;
import net.bither.util.MarketUtil;
import net.bither.util.StringUtil;
import net.bither.util.UnitUtil;
import net.bither.util.WalletUtils;

public class MarketListHeader extends FrameLayout implements MarketTickerChangedObserver,
        ViewTreeObserver.OnGlobalLayoutListener {
    private static final int LightScanInterval = 1200;
    public static int BgAnimDuration = 600;
    private Animation refreshAnim = AnimationUtils.loadAnimation(getContext(),
            R.anim.check_light_scan);
    private View ivLight;
    private TextView tvName;
    private TextView tvSymbol;
    private TextView tvPrice;
    private TextView tvHigh;
    private TextView tvLow;
    private TextView tvVolume;
    private TextView tvSell;
    private TextView tvBuy;
    private ImageView ivVolumeSymbol;
    private TrendingGraphicView vTrending;
    private LinearLayout llTrending;
    private View vContainer;
    private View flContainer;
    private View parent;
    private LinearLayout llAlert;
    private EditText etAlertHigh;
    private EditText etAlertLow;
    private BgHolder bg;
    private Button btnPriceAlertOk;
    private GestureDetector gestureDetector;
    private ContainerBottomPaddingHolder bottomHolder;
    private InputMethodManager imm;
    private Market mMarket;
    private TextViewListener alertTextViewListener;

    public MarketListHeader(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        removeAllViews();
        imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        parent = LayoutInflater.from(getContext()).inflate(R.layout.layout_market_list_header,
                null);
        addView(parent, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        flContainer = findViewById(R.id.fl_container);
        vContainer = findViewById(R.id.ll_container);
        ivLight = findViewById(R.id.iv_light);
        tvName = (TextView) findViewById(R.id.tv_market_name);
        tvSymbol = (TextView) findViewById(R.id.tv_currency_symbol);
        tvPrice = (TextView) findViewById(R.id.tv_new_price);
        tvHigh = (TextView) findViewById(R.id.tv_high);
        tvLow = (TextView) findViewById(R.id.tv_low);
        tvVolume = (TextView) findViewById(R.id.tv_volume);
        tvSell = (TextView) findViewById(R.id.tv_sell);
        tvBuy = (TextView) findViewById(R.id.tv_buy);
        ivVolumeSymbol = (ImageView) findViewById(R.id.iv_volume_symbol);
        vTrending = (TrendingGraphicView) findViewById(R.id.v_trending);
        llTrending = (LinearLayout) findViewById(R.id.ll_trending);
        llAlert = (LinearLayout) findViewById(R.id.ll_alert);
        etAlertHigh = (EditText) findViewById(R.id.et_alert_high);
        etAlertLow = (EditText) findViewById(R.id.et_alert_low);
        btnPriceAlertOk = (Button) findViewById(R.id.btn_ok);
        gestureDetector = new GestureDetector(getContext(), new SwipeDetector());
        bottomHolder = new ContainerBottomPaddingHolder();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
        alertTextViewListener = new TextViewListener();
        etAlertHigh.setOnEditorActionListener(alertTextViewListener);
        etAlertHigh.addTextChangedListener(alertTextViewListener);
        etAlertHigh.setOnFocusChangeListener(alertTextViewListener);
        etAlertLow.setOnEditorActionListener(alertTextViewListener);
        etAlertLow.addTextChangedListener(alertTextViewListener);
        etAlertLow.setOnFocusChangeListener(alertTextViewListener);
        btnPriceAlertOk.setOnClickListener(new PriceAlertOkClick());
        refreshAnim.setDuration(LightScanInterval);
        refreshAnim.setFillBefore(false);
        refreshAnim.setRepeatCount(0);
        refreshAnim.setFillAfter(false);
        ivVolumeSymbol.setImageBitmap(UnitUtil.getBtcSymbol(tvVolume, UnitUtil.BitcoinUnit.BTC));
        llTrending.setOnClickListener(new MarketDetailClick());
        llTrending.setOnTouchListener(new TrendingTouch());
        setMarket(MarketUtil.getDefaultMarket());
    }

    private void showMarket() {
        bringToFront();
        if (mMarket == null) {
            return;
        }
        bg.setEndColor(mMarket.getMarketColor());
        tvName.setText(mMarket.getName());
        String symbol = "";
        if (mMarket.getTicker() != null) {
            if (ExchangeUtil.getCurrenciesRate() != null) {
                symbol = AppSharedPreference.getInstance().getDefaultExchangeType().getSymbol();
            } else {
                symbol = ExchangeUtil.getExchangeType(mMarket.getMarketType()).getSymbol();
            }
        }
        tvSymbol.setText(symbol);
        if (mMarket != null) {
            vTrending.setMarketType(mMarket.getMarketType());
        }
        Ticker ticker = mMarket.getTicker();
        if (ticker == null) {
            tvPrice.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvHigh.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvLow.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvVolume.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvSell.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvBuy.setText(BitherSetting.UNKONW_ADDRESS_STRING);
        } else {
            tvPrice.setText(StringUtil.formatDoubleToMoneyString(ticker.getDefaultExchangePrice()));
            tvHigh.setText(symbol + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeHigh()));
            tvLow.setText(symbol + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeLow()));
            tvVolume.setText(StringUtil.formatDoubleToMoneyString(ticker.getAmount()));
            tvSell.setText(symbol + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeSell()));
            tvBuy.setText(symbol + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeBuy()));
        }
    }

    private void showAlert() {
        PriceAlert priceAlert = mMarket.getPriceAlert();
        etAlertHigh.setText("");
        etAlertLow.setText("");
        if (priceAlert != null) {
            if (priceAlert.getExchangeHigher() > 0) {
                etAlertHigh.setText(StringUtil.formatDoubleToMoneyString(priceAlert
                        .getExchangeHigher()));
            }
            if (priceAlert.getExchangeLower() > 0) {
                etAlertLow.setText(StringUtil.formatDoubleToMoneyString(priceAlert
                        .getExchangeLower()));
            }
        }
    }

    public MarketListHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MarketListHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public Market getMarket() {
        return mMarket;
    }

    public void setMarket(Market market) {
        if (mMarket == null) {
            bg = new BgHolder(market.getMarketColor());
        }
        if (mMarket != null) {
            if (bottomHolder.getBottom() > 0) {
                bottomHolder.reset();
            } else {
                bottomHolder.shake();
            }
        }
        mMarket = market;
        showMarket();
        showAlert();
    }

    @Override
    public void onMarketTickerChanged() {
        if (mMarket == null) {
            return;
        }
        showMarket();
        if (!refreshAnim.hasStarted() || refreshAnim.hasEnded()) {
            ivLight.startAnimation(refreshAnim);
        }
    }

    public void onPause() {
        bottomHolder.reset();
    }

    public void onResume() {
        showMarket();
    }

    public void reset() {
        bottomHolder.reset();
    }

    @Override
    public void onGlobalLayout() {
        if (parent.getLayoutParams().height <= 0) {
            parent.getLayoutParams().height = parent.getHeight();
            vContainer.getLayoutParams().height = vContainer.getHeight();
            flContainer.getLayoutParams().height = flContainer.getHeight();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void savePriceAlert() {
        double high = -1;
        double low = -1;
        if (etAlertHigh.getText().length() > 0) {
            high = Double.parseDouble(etAlertHigh.getText().toString());
        }
        if (etAlertLow.getText().length() > 0) {
            low = Double.parseDouble(etAlertLow.getText().toString());
        }
        if (mMarket.getTicker() != null) {
            if (high > 0 && high <= mMarket.getTicker().getDefaultExchangePrice()) {
                etAlertHigh.setText("");
                DropdownMessage.showDropdownMessage((Activity) getContext(),
                        R.string.market_price_alert_high_error);
                return;
            }
            if (low > 0 && low >= mMarket.getTicker().getDefaultExchangePrice()) {
                etAlertLow.setText("");
                DropdownMessage.showDropdownMessage((Activity) getContext(),
                        R.string.market_price_alert_low_error);
                return;
            }
        }
        mMarket.setPriceAlert(low, high);
        if (BitherApplication.hotActivity != null && BitherApplication.hotActivity
                .getFragmentAtIndex(0) != null && BitherApplication.hotActivity
                .getFragmentAtIndex(0) instanceof MarketFragment) {
            final MarketFragment f = (MarketFragment) BitherApplication.hotActivity
                    .getFragmentAtIndex(0);
            if (low > 0 || high > 0) {
                imm.hideSoftInputFromWindow(etAlertHigh.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                etAlertLow.clearFocus();
                etAlertHigh.clearFocus();
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bottomHolder.reset();
                        int[] location = new int[2];
                        findViewById(R.id.iv_icon).getLocationInWindow(location);
                        f.showPriceAlertAnimTo(location[0], location[1], mMarket);
                    }
                }, 300);
            } else {
                bottomHolder.reset();
                f.doRefresh();
            }
        }
    }

    private class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        private int SwipeThreshold = 20;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) < Math.abs(diffY)) {
                    if (Math.abs(diffY) > SwipeThreshold) {
                        if (diffY > 0) {
                            if (bottomHolder.isShowing()) {
                                bottomHolder.reset();
                                return true;
                            }
                        } else {
                            if (!bottomHolder.isShowing()) {
                                bottomHolder.showBottom();
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (bottomHolder.isShowing()) {
                if (touchInView(e, etAlertHigh) || touchInView(e, etAlertLow) || touchInView(e,
                        btnPriceAlertOk)) {
                    return super.onDown(e);
                }
            } else {
                if (touchInView(e, llTrending)) {
                    return super.onDown(e);
                }
            }
            return true;
        }

        private boolean touchInView(MotionEvent e, View v) {
            float x, y;
            x = e.getX();
            y = e.getY();
            int[] locationInWindow = new int[2];
            getLocationInWindow(locationInWindow);
            x = x + locationInWindow[0];
            y = y + locationInWindow[1];
            v.getLocationInWindow(locationInWindow);
            if (x > locationInWindow[0] && x < locationInWindow[0] + v.getWidth()) {
                if (y > locationInWindow[1] && y < locationInWindow[1] + v.getHeight()) {
                    return true;
                }
            }
            return false;
        }
    }

    private class ContainerBottomPaddingHolder {
        public static final int AnimDuration = 180;

        public void reset() {
            if (isShowing()) {
                imm.hideSoftInputFromWindow(etAlertHigh.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                ObjectAnimator.ofInt(this, "bottom", 0).setDuration(AnimDuration).start();
                etAlertLow.clearFocus();
                etAlertHigh.clearFocus();
            }
        }

        public boolean isShowing() {
            return getBottom() >= llAlert.getHeight();
        }

        public int getBottom() {
            return flContainer.getPaddingBottom();
        }

        public void setBottom(int bottom) {
            bottom = Math.max(0, Math.min(bottom, llAlert.getHeight()));
            flContainer.setPadding(0, 0, 0, bottom);
            vContainer.requestLayout();
        }

        public void shake() {
            ObjectAnimator.ofInt(this, "bottom", getBottom(), llAlert.getHeight() / 2,
                    0).setDuration(AnimDuration * 2).start();
        }

        public void showBottom() {
            if (!isShowing()) {
                showAlert();
                ObjectAnimator.ofInt(this, "bottom", getBottom(),
                        llAlert.getHeight()).setDuration(AnimDuration * (llAlert.getHeight() -
                        getBottom()) / llAlert.getHeight()).start();
            }
        }

        public int getThreshold() {
            return (int) (llAlert.getHeight() * 0.8f);
        }
    }

    private class MarketDetailClick implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (mMarket != null) {
                Intent intent = new Intent(getContext(), MarketDetailActivity.class);
                intent.putExtra(BitherSetting.INTENT_REF.MARKET_INTENT, mMarket.getMarketType());
                getContext().startActivity(intent);
            }
        }
    }

    private class TrendingTouch implements OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            vTrending.causeDraw();
            return false;
        }
    }

    private final class PriceAlertOkClick implements OnClickListener {

        @Override
        public void onClick(View v) {
            savePriceAlert();
        }
    }

    private final class TextViewListener implements TextWatcher, OnFocusChangeListener,
            TextView.OnEditorActionListener {
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count,
                                      final int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before,
                                  final int count) {
        }

        @Override
        public void afterTextChanged(final Editable s) {
            // workaround for German keyboards
            final String original = s.toString();
            final String replaced = original.replace(',', '.');
            if (!replaced.equals(original)) {
                s.clear();
                s.append(replaced);
            }
            if (s.length() > 0) {
                WalletUtils.formatSignificant(s, true ? WalletUtils.SMALLER_SPAN : null);
            }
        }

        @Override
        public void onFocusChange(final View v, final boolean hasFocus) {
            if (!hasFocus) {
                if (v instanceof EditText) {
                    EditText et = (EditText) v;
                    if (et.getText().length() > 0) {
                        et.setText(StringUtil.formatDoubleToMoneyString(Double.parseDouble(et
                                .getText().toString())));
                    }
                }
            }
        }

        @Override
        public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
            savePriceAlert();
            return true;
        }
    }

    private class BgHolder {
        private float animProgress = 0;
        private ArgbEvaluator evaluator = new ArgbEvaluator();
        private int startColor;
        private int endColor;
        private int currentColor;
        private ObjectAnimator animator;
        private View bg;

        public BgHolder(int startColor) {
            this.startColor = startColor;
            this.endColor = startColor;
            bg = findViewById(R.id.fl_bg);
            setBg((Integer) evaluator.evaluate(0, startColor, startColor));
        }

        private void setBg(int color) {
            currentColor = color;
            bg.setBackgroundColor(color);
        }

        public float getAnimProgress() {
            return animProgress;
        }

        public void setAnimProgress(float animProgress) {
            this.animProgress = animProgress;
            if (currentColor == endColor) {
                setBg(currentColor);
            } else {
                setBg((Integer) evaluator.evaluate(animProgress, startColor, endColor));
            }
        }

        public void setEndColor(int endColor) {
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            startColor = currentColor;
            this.endColor = endColor;
            animProgress = 0;
            animator = ObjectAnimator.ofFloat(this, "animProgress", 1);
            animator.setDuration(BgAnimDuration);
            animator.start();
        }
    }

}
