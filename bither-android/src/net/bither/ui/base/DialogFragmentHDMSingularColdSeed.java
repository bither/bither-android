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

package net.bither.ui.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ToggleButton;

import net.bither.R;
import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/2/13.
 */
public class DialogFragmentHDMSingularColdSeed extends DialogFragment implements View
        .OnClickListener, ViewPager.OnPageChangeListener {
    public static interface DialogFragmentHDMSingularColdSeedListener {
        public void HDMSingularColdSeedRemembered();
    }

    public static final String FragmentTag = "DialogFragmentHDMSingularColdSeed";
    public static final String WordsKey = "Content";
    public static final String QrCodeKey = "QRCode";

    private String qr;
    private List<String> words;
    private ViewPager pager;
    private ToggleButton tbtnWords;
    private ToggleButton tbtnQr;
    private PagerAdapter adapter;
    private DialogFragmentHDMSingularColdSeedListener listener;
    private Activity activity;

    public static DialogFragmentHDMSingularColdSeed newInstance(List<String> words, String qr,
                                                                DialogFragmentHDMSingularColdSeedListener listener) {
        DialogFragmentHDMSingularColdSeed dialog = new DialogFragmentHDMSingularColdSeed();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordsKey, new ArrayList<String>(words));
        bundle.putString(QrCodeKey, qr);
        dialog.setArguments(bundle);
        dialog.listener = listener;
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.QrCodePager);
        qr = getArguments().getString(QrCodeKey);
        words = (List<String>) getArguments().getSerializable(WordsKey);
        adapter = new PagerAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View vContainer = inflater.inflate(R.layout.dialog_hdm_singular_cold_seed, container,
                false);
        pager = (ViewPager) vContainer.findViewById(R.id.pager);
        tbtnWords = (ToggleButton) vContainer.findViewById(R.id.tbtn_words);
        tbtnQr = (ToggleButton) vContainer.findViewById(R.id.tbtn_qr);
        vContainer.findViewById(R.id.btn_confirm).setOnClickListener(this);
        pager.setOffscreenPageLimit(1);
        int size = Math.min(UIUtil.getScreenWidth(), UIUtil.getScreenHeight());
        pager.getLayoutParams().width = pager.getLayoutParams().height = size;
        pager.setAdapter(adapter);
        pager.setCurrentItem(0);
        pager.setOnPageChangeListener(this);
        tbtnWords.setChecked(true);
        tbtnQr.setChecked(false);
        tbtnWords.setOnClickListener(new IndicatorClick(0));
        tbtnQr.setOnClickListener(new IndicatorClick(1));
        return vContainer;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        setCancelable(false);
        return dialog;
    }

    @Override
    public void onStart() {
        activity = getActivity();
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        super.onStart();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (listener != null) {
            listener.HDMSingularColdSeedRemembered();
        }
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    private class PagerAdapter extends FragmentPagerAdapter {

        public PagerAdapter() {
            super(getChildFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return DialogFragmentHDMSingularColdSeedWords.newInstance(words);
            } else {
                return DialogFragmentHDMSingularColdSeedQr.newInstance(qr);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }


    private class IndicatorClick implements View.OnClickListener {
        private int position;

        public IndicatorClick(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            if (position != pager.getCurrentItem()) {
                pager.setCurrentItem(position, true);
            }
            if (position == 0) {
                tbtnQr.setChecked(false);
                tbtnWords.setChecked(true);
            } else {
                tbtnQr.setChecked(true);
                tbtnWords.setChecked(false);
            }
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (adapter.getCount() > 1) {
            if (position == 0) {
                tbtnWords.setChecked(true);
                tbtnQr.setChecked(false);
            } else {
                tbtnQr.setChecked(true);
                tbtnWords.setChecked(false);
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
