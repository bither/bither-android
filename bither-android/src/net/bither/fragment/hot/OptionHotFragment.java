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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.hot.CheckPrivateKeyActivity;
import net.bither.activity.hot.HotAdvanceActivity;
import net.bither.activity.hot.NetworkMonitorActivity;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Selectable;
import net.bither.image.glcrop.CropImageGlActivity;
import net.bither.model.Market;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.UploadAvatarRunnable;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SettingSelectorView;
import net.bither.ui.base.SettingSelectorView.SettingSelector;
import net.bither.ui.base.dialog.DialogDonate;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogSetAvatar;
import net.bither.util.ExchangeUtil;
import net.bither.util.FileUtil;
import net.bither.util.ImageFileUtil;
import net.bither.util.ImageManageUtil;
import net.bither.util.LogUtil;
import net.bither.util.MarketUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;

import java.io.File;
import java.util.List;

public class OptionHotFragment extends Fragment implements Selectable,
        DialogSetAvatar.SetAvatarDelegate {
    private static Uri imageUri;
    private SettingSelectorView ssvCurrency;
    private SettingSelectorView ssvMarket;
    private SettingSelectorView ssvTransactionFee;
    private Button btnAvatar;
    private Button btnCheck;
    private Button btnDonate;
    private Button btnAdvance;
    private TextView tvWebsite;
    private TextView tvVersion;
    private ImageView ivLogo;


    private DialogProgress dp;
    private OnClickListener logoClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), NetworkMonitorActivity.class);
            startActivity(intent);
        }
    };
    private SettingSelector currencySelector = new SettingSelector() {

        @Override
        public int getOptionCount() {
            return 2;
        }

        @Override
        public void onOptionIndexSelected(int index) {
            if (index == 0) {
                AppSharedPreference.getInstance().setExchangeType(ExchangeUtil.Currency.USD);
            } else {
                AppSharedPreference.getInstance().setExchangeType(ExchangeUtil.Currency.CNY);
            }
        }

        @Override
        public String getSettingName() {
            return getString(R.string.setting_name_currency);
        }

        @Override
        public String getOptionName(int index) {
            if (index == 0) {
                return "USD";
            }
            return "CNY";
        }


        @Override
        public int getCurrentOptionIndex() {
            return AppSharedPreference.getInstance().getDefaultExchangeType().ordinal();
        }

        @Override
        public String getOptionNote(int index) {
            return null;
        }

        @Override
        public Drawable getOptionDrawable(int index) {
            return null;
        }
    };
    private SettingSelector marketSelector = new SettingSelector() {
        private List<Market> markets = MarketUtil.getMarkets();

        @Override
        public void onOptionIndexSelected(int index) {
            AppSharedPreference.getInstance().setMarketType(markets.get(index).getMarketType());
        }

        @Override
        public String getSettingName() {
            return getString(R.string.setting_name_market);
        }

        @Override
        public String getOptionName(int index) {
            return markets.get(index).getName();
        }

        @Override
        public int getOptionCount() {
            return markets.size();
        }

        @Override
        public int getCurrentOptionIndex() {
            return markets.indexOf(MarketUtil.getDefaultMarket());
        }

        @Override
        public String getOptionNote(int index) {
            return null;
        }

        @Override
        public Drawable getOptionDrawable(int index) {
            return null;
        }
    };
    private SettingSelector transactionFeeModeSelector = new SettingSelector() {

        @Override
        public void onOptionIndexSelected(int index) {
            // This warning is no longer needed. As more and more mining pool upgrade their
            // bitcoin client to 0.9.+, low fee transactions get confirmed soon enough.
//            if (index == TransactionFeeMode.Low.ordinal()) {
//
//                DialogConfirmTask dialog = new DialogConfirmTask(getActivity(),
//                        getString(R.string.setting_name_transaction_fee_low_warn),
//new Runnable() {
//                    @Override
//                    public void run() {
//                        ssvTransactionFee.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                AppSharedPreference.getInstance().setTransactionFeeMode
//                                        (TransactionFeeMode.Low);
//                                ssvTransactionFee.loadData();
//                            }
//                        });
//                    }
//                }
//                );
//                dialog.show();
//            }
            AppSharedPreference.getInstance().setTransactionFeeMode(getModeByIndex(index));
        }

        @Override
        public String getSettingName() {
            return getString(R.string.setting_name_transaction_fee);
        }

        @Override
        public String getOptionName(int index) {
            BitherjSettings.TransactionFeeMode transactionFeeMode = getModeByIndex(index);
            switch (transactionFeeMode) {
                case Low:
                    return getString(R.string.setting_name_transaction_fee_low);
                default:
                    return getString(R.string.setting_name_transaction_fee_normal);
            }
        }

        @Override
        public int getOptionCount() {
            return BitherjSettings.TransactionFeeMode.values().length;
        }

        @Override
        public int getCurrentOptionIndex() {
            return AppSharedPreference.getInstance().getTransactionFeeMode().ordinal();
        }

        private BitherjSettings.TransactionFeeMode getModeByIndex(int index) {
            if (index >= 0 && index < BitherjSettings.TransactionFeeMode.values().length) {
                return BitherjSettings.TransactionFeeMode.values()[index];
            }
            return BitherjSettings.TransactionFeeMode.Normal;
        }

        @Override
        public String getOptionNote(int index) {
            switch (getModeByIndex(index)) {
                case Low:
                    return getString(R.string.setting_name_transaction_fee_low_note);
                default:
                    return getString(R.string.setting_name_transaction_fee_normal_note);
            }
        }

        @Override
        public Drawable getOptionDrawable(int index) {
            return null;
        }
    };

    private OnClickListener checkClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (AddressManager.getInstance().getPrivKeyAddresses() == null || AddressManager.getInstance().getPrivKeyAddresses().size() == 0) {
                DropdownMessage.showDropdownMessage(getActivity(), R.string.private_key_is_empty);
                return;
            }
            Intent intent = new Intent(getActivity(), CheckPrivateKeyActivity.class);
            startActivity(intent);
        }
    };
    private OnClickListener donateClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogDonate dialog = new DialogDonate(getActivity());
            dialog.show();
        }
    };
    private OnClickListener advanceClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), HotAdvanceActivity.class);
            startActivity(intent);
        }
    };
    private OnClickListener avatarClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            DialogSetAvatar dialog = new DialogSetAvatar(getActivity(), OptionHotFragment.this);
            dialog.show();
        }
    };
    private OnClickListener websiteClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://bither.net/"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                DropdownMessage.showDropdownMessage(getActivity(), R.string.find_browser_error);
            }
        }
    };


    @Override
    public void avatarFromCamera() {
        if (FileUtil.existSdCardMounted()) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = ImageFileUtil.getImageForGallery(System.currentTimeMillis());
            imageUri = Uri.fromFile(file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, BitherSetting.REQUEST_CODE_CAMERA);
        } else {
            DropdownMessage.showDropdownMessage(getActivity(), R.string.no_sd_card);
        }
    }

    @Override
    public void avatarFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media
                .EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, BitherSetting.REQUEST_CODE_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case BitherSetting.REQUEST_CODE_IMAGE:
                if (data != null) {
                    Intent intent = new Intent(getActivity(), CropImageGlActivity.class);
                    intent.setData(data.getData());
                    intent.setAction(data.getAction());
                    LogUtil.d("fragment", "REQUEST_CODE_IMAGE");
                    startActivityForResult(intent, BitherSetting.REQUEST_CODE_CROP_IMAGE);
                }
                break;
            case BitherSetting.REQUEST_CODE_CAMERA:
                Intent intent = new Intent(getActivity(), CropImageGlActivity.class);

                intent.putExtra("android.intent.extra.STREAM", imageUri);
                intent.setAction(Intent.ACTION_SEND);
                LogUtil.d("fragment", "REQUEST_CODE_CAMERA");
                startActivityForResult(intent, BitherSetting.REQUEST_CODE_CROP_IMAGE);
                break;
            case BitherSetting.REQUEST_CODE_CROP_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    String photoName = "";
                    if (data != null && data.hasExtra(BitherSetting.INTENT_REF
                            .PIC_PASS_VALUE_TAG)) {
                        photoName = data.getStringExtra(BitherSetting.INTENT_REF
                                .PIC_PASS_VALUE_TAG);
                    }
                    LogUtil.d("fragment", "photoName:" + photoName);
                    if (!Utils.isEmpty(photoName)) {
                        AppSharedPreference.getInstance().setUserAvatar(photoName);
                        setAvatar(photoName);
                    }
                }
                break;
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hot_option, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        ssvCurrency = (SettingSelectorView) view.findViewById(R.id.ssv_currency);
        ssvMarket = (SettingSelectorView) view.findViewById(R.id.ssv_market);
        ssvTransactionFee = (SettingSelectorView) view.findViewById(R.id.ssv_transaction_fee);
        tvVersion = (TextView) view.findViewById(R.id.tv_version);
        tvWebsite = (TextView) view.findViewById(R.id.tv_website);
        tvWebsite.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        ivLogo = (ImageView) view.findViewById(R.id.iv_logo);
        btnAvatar = (Button) view.findViewById(R.id.btn_avatar);
        btnCheck = (Button) view.findViewById(R.id.btn_check_private_key);
        btnDonate = (Button) view.findViewById(R.id.btn_donate);
        btnAdvance = (Button) view.findViewById(R.id.btn_advance);
        ssvCurrency.setSelector(currencySelector);
        ssvMarket.setSelector(marketSelector);
        ssvTransactionFee.setSelector(transactionFeeModeSelector);
        dp = new DialogProgress(getActivity(), R.string.please_wait);
        dp.setCancelable(false);
        String version = null;
        try {
            version = getActivity().getPackageManager().getPackageInfo(getActivity()
                    .getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        if (version != null) {
            tvVersion.setText(version);
            tvVersion.setVisibility(View.VISIBLE);
        } else {
            tvVersion.setVisibility(View.GONE);
        }
        btnCheck.setOnClickListener(checkClick);
        btnDonate.setOnClickListener(donateClick);
        btnAvatar.setOnClickListener(avatarClick);
        btnAdvance.setOnClickListener(advanceClick);
        tvWebsite.setOnClickListener(websiteClick);
        ivLogo.setOnClickListener(logoClickListener);
        setAvatar(AppSharedPreference.getInstance().getUserAvatar());
    }

    private void setAvatar(String photoName) {
        Bitmap avatar = null;
        if (!Utils.isEmpty(photoName)) {
            new UpdateAvatarThread(photoName).start();
        } else {
            btnAvatar.setCompoundDrawablesWithIntrinsicBounds(null, null,
                    getResources().getDrawable(R.drawable.avatar_button_icon), null);
        }
    }

    @Override
    public void onSelected() {
    }

    private class UpdateAvatarThread extends Thread {
        private String photoName;

        private UpdateAvatarThread(String photoName) {
            this.photoName = photoName;
        }

        @Override
        public void run() {
            Bitmap avatar = null;
            if (!Utils.isEmpty(photoName)) {
                File file = ImageFileUtil.getSmallAvatarFile(photoName);
                avatar = ImageManageUtil.getBitmapNearestSize(file, 150);
            }
            if (avatar != null) {
                int borderPadding = UIUtil.dip2pix(2);
                Bitmap bmpBorder = BitmapFactory.decodeResource(getResources(),
                        R.drawable.avatar_button_icon_border);
                Bitmap result = Bitmap.createBitmap(bmpBorder.getWidth(), bmpBorder.getHeight(),
                        bmpBorder.getConfig());
                Canvas c = new Canvas(result);
                c.drawBitmap(avatar, null, new Rect(borderPadding, borderPadding,
                                result.getWidth() - borderPadding,
                                result.getHeight() - borderPadding), null
                );
                c.drawBitmap(bmpBorder, 0, 0, null);
                final BitmapDrawable d = new BitmapDrawable(getResources(), result);
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        btnAvatar.setCompoundDrawablesWithIntrinsicBounds(null, null, d, null);
                    }

                });
            }
            UploadAvatarRunnable uploadAvatarRunnable = new UploadAvatarRunnable();
            uploadAvatarRunnable.run();
        }
    }
}
