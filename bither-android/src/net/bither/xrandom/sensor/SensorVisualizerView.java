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

package net.bither.xrandom.sensor;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.LinearLayout;

import net.bither.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by songchenwen on 14-9-16.
 */
public class SensorVisualizerView extends LinearLayout {
    private static final float FadeAlpha = 0.3f;
    private static final float FlashAlpha = 1.0f;
    private static final int FlashDuration = 300;

    private static final HashMap<Integer, Integer> ViewIds = new HashMap<Integer, Integer>();

    static {
        ViewIds.put(Sensor.TYPE_MAGNETIC_FIELD, R.id.iv_magnetic);
        ViewIds.put(Sensor.TYPE_ACCELEROMETER, R.id.iv_accelerometer);
        ViewIds.put(Sensor.TYPE_LIGHT, R.id.iv_light);
        ViewIds.put(Sensor.TYPE_GRAVITY, R.id.iv_gravity);
    }

    private HashSet<Integer> animatingSensors;

    private boolean holdingBackNextFlash;


    public SensorVisualizerView(Context context) {
        super(context);
        firstConfigure();
    }

    public SensorVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        firstConfigure();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SensorVisualizerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        firstConfigure();
        holdingBackNextFlash = false;
    }

    private void firstConfigure() {
        removeAllViews();
        LayoutInflater.from(getContext()).inflate(R.layout.layout_xrandom_sensor, this);
        animatingSensors = new HashSet<Integer>();
    }

    public void setSensors(final List<Sensor> sensors) {
        post(new Runnable() {
            @Override
            public void run() {
                Iterator<Integer> iterator = ViewIds.values().iterator();
                while (iterator.hasNext()) {
                    findViewById(iterator.next()).setVisibility(View.GONE);
                }
                for (final Sensor sensor : sensors) {
                    Integer id = ViewIds.get(sensor.getType());
                    if (id != null) {
                        View v = findViewById(id);
                        if (v == null) {
                            return;
                        }
                        animatingSensors.add(sensor.getType());
                        v.setVisibility(View.VISIBLE);
                        AlphaAnimation alphaAnimation = new AlphaAnimation(FlashAlpha, FadeAlpha);
                        alphaAnimation.setFillAfter(true);
                        alphaAnimation.setDuration(FlashDuration);
                        alphaAnimation.setAnimationListener(new FadeAnimListener(sensor.getType()));
                        v.startAnimation(alphaAnimation);
                    }
                }
            }
        });
    }

    public void onSensorData(final Sensor sensor) {
        if (animatingSensors.contains(sensor.getType()) || holdingBackNextFlash) {
            return;
        }
        holdingBackNextFlash = true;
        
        postDelayed(new Runnable() {
            @Override
            public void run() {
                holdingBackNextFlash = false;
            }
        }, FlashDuration / 2);

        post(new Runnable() {
            @Override
            public void run() {
                Integer id = ViewIds.get(sensor.getType());
                if (id == null) {
                    return;
                }
                View v = findViewById(id);
                if (v == null) {
                    return;
                }
                animatingSensors.add(sensor.getType());
                AlphaAnimation fadeIn = new AlphaAnimation(FadeAlpha, FlashAlpha);
                fadeIn.setDuration(FlashDuration);
                AlphaAnimation fadeOut = new AlphaAnimation(FlashAlpha, FadeAlpha);
                fadeOut.setDuration(FlashDuration);
                fadeOut.setStartOffset(fadeIn.getDuration() + FlashDuration / 2);
                AnimationSet set = new AnimationSet(false);
                set.addAnimation(fadeIn);
                set.addAnimation(fadeOut);
                set.setAnimationListener(new FadeAnimListener(sensor.getType()));
                set.setFillAfter(true);
                v.startAnimation(set);
            }
        });
    }

    private class FadeAnimListener implements Animation.AnimationListener {
        private int type;

        public FadeAnimListener(int type) {
            this.type = type;
        }

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (animatingSensors.contains(type)) {
                        animatingSensors.remove(type);
                    }
                }
            }, FlashDuration);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }
}
