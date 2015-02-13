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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import net.bither.R;
import net.bither.runnable.FancyQrCodeThread;
import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 15/2/13.
 */
public class DialogFragmentHDMSingularColdSeedQr extends Fragment implements FancyQrCodeThread
        .FancyQrCodeListener {

    private static final String QrTag = "QR";
    private String qr;
    private ImageView iv;
    private ProgressBar pb;

    public static DialogFragmentHDMSingularColdSeedQr newInstance(String qr) {
        DialogFragmentHDMSingularColdSeedQr page = new DialogFragmentHDMSingularColdSeedQr();
        Bundle bundle = new Bundle();
        bundle.putString(QrTag, qr);
        page.setArguments(bundle);
        return page;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        qr = bundle.getString(QrTag);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_hdm_singular_cold_seed_qr, null);
        iv = (ImageView) v.findViewById(R.id.iv_qr);
        pb = (ProgressBar) v.findViewById(R.id.pb);
        new FancyQrCodeThread(qr, Math.min(UIUtil.getScreenHeight(), UIUtil.getScreenWidth()),
                Color.BLACK, Color.WHITE, this, false).start();
        return v;
    }

    @Override
    public void generated(Bitmap bmp) {
        iv.setImageBitmap(bmp);
        iv.setVisibility(View.VISIBLE);
        pb.setVisibility(View.GONE);
    }
}
