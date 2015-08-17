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
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.dialog.DialogTotalBtc;
import net.bither.ui.base.dialog.DialogWithArrow;
import net.bither.util.UIUtil;
import net.bither.util.UnitUtilWrapper;

import java.math.BigInteger;

public class TabButton extends FrameLayout implements OnShowListener, OnDismissListener {
    private ImageView ivIcon;
    private ToggleButton tbtnBottom;
    private TextView tvCount;

    private TextView tvText;
    private ImageView ivArrowDown;
    private boolean ellipsized = true;

    private DialogWithArrow dialog;

    private int uncheckedIcon;
    private int checkedIcon;

    private int mCount;

    private boolean overrideArrowBehavior = false;

    public TabButton(Context context) {
        super(context);
        init();
    }

    public TabButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TabButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        LinearLayout llIcon = new LinearLayout(getContext());
        llIcon.setOrientation(LinearLayout.HORIZONTAL);
        addView(llIcon, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT,
                Gravity.CENTER));
        ivIcon = new ImageView(getContext());
        ivIcon.setPadding(0, 0, 0, UIUtil.dip2pix(0.75f));
        LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(LayoutParams
                .MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lpIcon.topMargin = UIUtil.dip2pix(3);
        lpIcon.bottomMargin = UIUtil.dip2pix(3);
        lpIcon.gravity = Gravity.CENTER;
        ivIcon.setScaleType(ScaleType.CENTER_INSIDE);
        llIcon.addView(ivIcon, lpIcon);
        tvText = new TextView(getContext());
        tvText.setTextColor(Color.WHITE);
        tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tvText.setTypeface(null, Typeface.BOLD);
        tvText.setShadowLayer(0.5f, 1, -1, Color.argb(100, 0, 0, 0));
        tvText.setPadding(0, 0, 0, UIUtil.dip2pix(0.75f));
        tvText.setLines(1);
        tvText.setEllipsize(TruncateAt.END);
        llIcon.addView(tvText);
        ivArrowDown = new ImageView(getContext());
        llIcon.addView(ivArrowDown, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        ((LinearLayout.LayoutParams) ivArrowDown.getLayoutParams()).gravity = Gravity
                .CENTER_VERTICAL;
        LinearLayout.LayoutParams lpText = (LinearLayout.LayoutParams) tvText.getLayoutParams();
        lpText.weight = 1;
        lpText.width = 0;
        lpText.gravity = Gravity.CENTER_VERTICAL;
        lpText.leftMargin = UIUtil.dip2pix(-7);
        LayoutParams lpBottom = new LayoutParams(LayoutParams.MATCH_PARENT, UIUtil.dip2pix(2.67f));
        lpBottom.bottomMargin = UIUtil.dip2pix(0.75f);
        lpBottom.gravity = Gravity.BOTTOM;
        tbtnBottom = new ToggleButton(getContext());
        tbtnBottom.setTextOff("");
        tbtnBottom.setTextOn("");
        tbtnBottom.setText("");
        tbtnBottom.setBackgroundResource(R.drawable.tab_bottom_background_selector);
        tbtnBottom.setFocusable(false);
        tbtnBottom.setClickable(false);
        addView(tbtnBottom, lpBottom);

        tvCount = new TextView(getContext());
        tvCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tvCount.setGravity(Gravity.CENTER);
        tvCount.setTextColor(Color.WHITE);
        tvCount.setBackgroundResource(R.drawable.new_message_bg);
        LayoutParams lpCount = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        lpCount.leftMargin = UIUtil.dip2pix(21);
        lpCount.bottomMargin = UIUtil.dip2pix(11);
        addView(tvCount, lpCount);
        tvCount.setVisibility(View.GONE);
        tvText.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                                                                   @Override
                                                                   public void onGlobalLayout() {
                                                                       if (!ellipsized) {
                                                                           return;
                                                                       }
                                                                       ellipsized = false;
                                                                       Layout l = tvText.getLayout();
                                                                       if (l != null) {
                                                                           int lines = l.getLineCount();
                                                                           if (lines > 0) {
                                                                               if (l.getEllipsisCount(lines - 1) > 0) {
                                                                                   ellipsized = true;
                                                                               }
                                                                           }
                                                                       }
                                                                       updateArrow();
                                                                   }
                                                               }
        );
    }

    public void setIconResource(int unchecked, int checked) {
        this.uncheckedIcon = unchecked;
        this.checkedIcon = checked;
        configureIcon();
    }

    private void configureIcon() {
        if (isChecked()) {
            ivIcon.setImageResource(checkedIcon);
            tvText.setTextColor(Color.WHITE);
        } else {
            ivIcon.setImageResource(uncheckedIcon);
            tvText.setTextColor(Color.parseColor("#a2b3c2"));
        }
    }

    public void setCount(int count) {
        mCount = count;
        if (mCount > 0) {
            tvCount.setVisibility(View.VISIBLE);
            String c = Integer.toString(mCount);
            if (mCount > 9) {
                c = "9+";
            }
            tvCount.setText(c);
        } else {
            tvCount.setVisibility(View.GONE);
        }
    }

    public void setBigInteger(BigInteger btcPrivate, BigInteger btcWatchOnly, BigInteger btcHdm,
                              BigInteger btcHD, BigInteger btcHdMonitored) {
        BigInteger btc;
        if (btcPrivate == null && btcWatchOnly == null && btcHdm == null && btcHD == null &&
                btcHdMonitored == null) {
            btc = null;
        } else {
            btc = BigInteger.ZERO;
            if (btcPrivate != null) {
                btc = btc.add(btcPrivate);
            }
            if (btcWatchOnly != null) {
                btc = btc.add(btcWatchOnly);
            }
            if (btcHdm != null){
                btc = btc.add(btcHdm);
            }
            if (btcHD != null) {
                btc = btc.add(btcHD);
            }
            if (btcHdMonitored != null) {
                btc = btc.add(btcHdMonitored);
            }
        }
        ellipsized = true;
        if (AppSharedPreference.getInstance().getTotalBalanceHide().shouldShowBalance()) {
            tvText.setVisibility(View.VISIBLE);
            if (btc == null) {
                tvText.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            } else {
                tvText.setText(UnitUtilWrapper.formatValue(btc.longValue()));
            }
            ((View) tvText.getParent()).setPadding(0, 0, UIUtil.dip2pix(11), 0);
        } else {
            tvText.setText(null);
            tvText.setVisibility(View.GONE);
        }
        tvText.requestLayout();
        if (dialog == null) {
            DialogTotalBtc d = new DialogTotalBtc(getContext());
            setDialog(d);
        }
        if (dialog instanceof DialogTotalBtc) {
            DialogTotalBtc d = (DialogTotalBtc) dialog;
            d.setPrivateWatchOnlyHDMAndHD(btcPrivate, btcWatchOnly, btcHdm, btcHD, btcHdMonitored);
        }
        updateArrow();
    }

    public void setText(String text) {
        ellipsized = true;
        tvText.setText(text);
        ((View) tvText.getParent()).setPadding(0, 0, UIUtil.dip2pix(11), 0);
    }

    public void showDialog() {
        if (dialog != null && AppSharedPreference.getInstance().getTotalBalanceHide()
                .shouldShowChart()) {
            dialog.show(this);
        }
    }

    private void updateArrow() {
        if (!AppSharedPreference.getInstance().getTotalBalanceHide().shouldShowChart()) {
            ivArrowDown.setVisibility(View.GONE);
            return;
        }
        if (!overrideArrowBehavior) {
            if (tvText.getText() == null || tvText.getText().length() == 0) {
                ivArrowDown.setVisibility(View.GONE);
                return;
            }
            if (!ellipsized) {
                ivArrowDown.setVisibility(View.GONE);
                return;
            }
            ivArrowDown.setVisibility(View.VISIBLE);
        }
        if (dialog != null && dialog.isShowing()) {
            if (isChecked()) {
                ivArrowDown.setImageResource(R.drawable.tab_arrow_up_checked);
            } else {
                ivArrowDown.setImageResource(R.drawable.tab_arrow_up_unchecked);
            }
        } else {
            if (isChecked()) {
                ivArrowDown.setImageResource(R.drawable.tab_arrow_down_checked);
            } else {
                ivArrowDown.setImageResource(R.drawable.tab_arrow_down_unchecked);
            }
        }
    }

    public void setChecked(boolean checked) {
        tbtnBottom.setChecked(checked);
        configureIcon();
        if (checked) {
            setCount(0);
        }
        updateArrow();
    }

    public void setArrowVisible(boolean visible, boolean override) {
        overrideArrowBehavior = override;
        if (visible) {
            ivArrowDown.setVisibility(View.VISIBLE);
        } else {
            ivArrowDown.setVisibility(View.GONE);
        }
    }

    public void setDialog(DialogWithArrow dialog) {
        this.dialog = dialog;
        dialog.setOnShowListener(this);
        dialog.setOnDismissListener(this);
    }

    public boolean isChecked() {
        return tbtnBottom.isChecked();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        updateArrow();
    }

    @Override
    public void onShow(DialogInterface dialog) {
        updateArrow();
    }
}
