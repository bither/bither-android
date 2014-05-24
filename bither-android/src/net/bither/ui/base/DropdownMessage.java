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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

import net.bither.R;
import net.bither.util.StringUtil;
import net.bither.util.ThreadUtil;

public class DropdownMessage {
    public static final long defaultDuration = 3000;
    private static final long inAnimDuration = 300;
    private static final long outAnimDuration = 300;

    private static void showDropdownMessage(final Activity activity, final String msg, final long duration, final Runnable after) {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (activity == null || StringUtil.isEmpty(msg) || duration <= 0) {
                    return;
                }

                FrameLayout fl = (FrameLayout) activity
                        .findViewById(R.id.fl_dropdown_message);

                if (fl == null) {
                    return;
                }

                View vMsg = LayoutInflater.from(activity).inflate(
                        R.layout.layout_dropdown_message_item, null);
                TextView tvMsg = (TextView) vMsg.findViewById(R.id.tv);
                tvMsg.setText(msg);
                addMsg(fl, vMsg, duration, after);
            }
        });
    }

    private static void addMsg(final FrameLayout fl, final View msg,
                               final long duration, final Runnable after) {
        fl.addView(msg, new LayoutParams(fl.getWidth(),
                LayoutParams.WRAP_CONTENT, Gravity.TOP));
        msg.measure(fl.getWidth(), MeasureSpec.UNSPECIFIED);
        final int msgHeight = msg.getMeasuredHeight();
        Animation animIn = AnimationUtils.loadAnimation(fl.getContext(),
                R.anim.dropdown_message_in);
        msg.startAnimation(animIn);
        fl.postDelayed(new Runnable() {
            @Override
            public void run() {
                removeMsg(fl, msg, after);
            }
        }, duration + inAnimDuration);
        int msgCount = fl.getChildCount();
        for (int i = msgCount - 2;
             i >= 0;
             i--) {
            final View v = fl.getChildAt(i);
            if (v != msg && v.getVisibility() == View.VISIBLE) {
                LayoutParams lp = (LayoutParams) v.getLayoutParams();
                lp.topMargin += msgHeight;
                TranslateAnimation anim = new TranslateAnimation(0, 0,
                        -msgHeight, 0);
                anim.setDuration(inAnimDuration);
                anim.setFillBefore(true);
                anim.setFillAfter(true);
                v.startAnimation(anim);
            }
        }
    }

    private static void removeMsg(final FrameLayout fl, final View msg,
                                  final Runnable after) {
        int msgCount = fl.getChildCount();
        int msgHeight = msg.getHeight();
        for (int i = msgCount - 1;
             i >= 0;
             i--) {
            View v = fl.getChildAt(i);
            if (v != msg) {
                TranslateAnimation anim = new TranslateAnimation(0, 0, 0,
                        -msgHeight);
                anim.setDuration(outAnimDuration);
                anim.setFillBefore(false);
                anim.setFillAfter(true);
                anim.setAnimationListener(new DropdownAnimationListener(v,
                        -msgHeight));
            } else {
                Animation anim = AnimationUtils.loadAnimation(fl.getContext(),
                        R.anim.dropdown_message_out);
                fl.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            msg.setVisibility(View.GONE);
                            fl.removeView(msg);
                            if (after != null) {
                                fl.post(after);
                            }
                        } catch (Exception e) {
                        }
                    }
                }, outAnimDuration);
                v.startAnimation(anim);
                break;
            }
        }
    }

    public static void showDropdownMessage(Activity activity, String msg) {
        showDropdownMessage(activity, msg, defaultDuration, null);
    }

    public static void showDropdownMessage(Activity activity, int resource) {
        showDropdownMessage(activity, activity.getString(resource));
    }

    public static void showDropdownMessage(Activity activity, String msg,
                                           Runnable after) {
        showDropdownMessage(activity, msg, defaultDuration, after);
    }

    public static void showDropdownMessage(Activity activity, int resource,
                                           Runnable after) {
        showDropdownMessage(activity, activity.getString(resource), after);
    }

    private static class DropdownAnimationListener implements
            Animation.AnimationListener {
        private View v;
        private int offset;

        public DropdownAnimationListener(View v, int offset) {
            this.v = v;
            this.offset = offset;
        }

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            try {
                LayoutParams lp = (LayoutParams) v.getLayoutParams();
                lp.topMargin = lp.topMargin + offset;
            } catch (Exception e) {

            }
        }
    }
}
