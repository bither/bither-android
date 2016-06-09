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
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.ChooseModeActivity;
import net.bither.R;
import net.bither.activity.hot.CheckPrivateKeyActivity;
import net.bither.activity.hot.HotActivity;
import net.bither.activity.hot.HotAdvanceActivity;
import net.bither.activity.hot.NetworkMonitorActivity;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.crypto.hd.DeterministicKey;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Selectable;
import net.bither.image.glcrop.CropImageGlActivity;
import net.bither.model.Market;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.ScanActivity;
import net.bither.runnable.ThreadNeedService;
import net.bither.runnable.UploadAvatarRunnable;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SettingSelectorView;
import net.bither.ui.base.SettingSelectorView.SettingSelector;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogDonate;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogSetAvatar;
import net.bither.util.ExchangeUtil;
import net.bither.util.FileUtil;
import net.bither.util.ImageFileUtil;
import net.bither.util.ImageManageUtil;
import net.bither.util.LogUtil;
import net.bither.util.MarketUtil;
import net.bither.util.MonitorBitherColdUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;
import net.bither.util.UnitUtilWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OptionHotFragment extends Fragment implements Selectable,
        DialogSetAvatar.SetAvatarDelegate {
    private static final int MonitorCodeHDRequestCode = 1605;

    private static Uri imageUri;
    private SettingSelectorView ssvCurrency;
    private SettingSelectorView ssvMarket;
    private SettingSelectorView ssvTransactionFee;
    private SettingSelectorView ssvBitcoinUnit;
    private Button btnSwitchToCold;
    private Button btnAvatar;
    private Button btnCheck;
    private Button btnAdvance;
    private TextView tvWebsite;
    private TextView tvVersion;
    private ImageView ivLogo;
    private View llSwitchToCold;

    private MonitorBitherColdUtil monitorUtil;


    private DialogProgress dp;
    private OnClickListener logoClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), NetworkMonitorActivity.class);
            startActivity(intent);
        }
    };

    private SettingSelector bitcoinUnitSelector = new SettingSelector() {
        @Override
        public int getOptionCount() {
            return UnitUtilWrapper.BitcoinUnitWrapper.values().length;
        }

        @Override
        public CharSequence getOptionName(int index) {
            UnitUtilWrapper.BitcoinUnitWrapper unit = UnitUtilWrapper.BitcoinUnitWrapper.values()
                    [index];
            SpannableString s = new SpannableString("  " + unit.name());
            Bitmap bmp = UnitUtilWrapper.getBtcSlimSymbol(getResources().getColor(R.color.text_field_text_color),
                    getResources().getDisplayMetrics().scaledDensity * 15.6f, unit);
            s.setSpan(new ImageSpan(getActivity(), bmp, ImageSpan.ALIGN_BASELINE), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return s;
        }

        @Override
        public String getOptionNote(int index) {
            return null;
        }

        @Override
        public Drawable getOptionDrawable(int index) {
            return null;
        }

        @Override
        public String getSettingName() {
            return getString(R.string.setting_name_bitcoin_unit);
        }

        @Override
        public int getCurrentOptionIndex() {
            return AppSharedPreference.getInstance().getBitcoinUnit().ordinal();
        }

        @Override
        public void onOptionIndexSelected(int index) {
            if (index != getCurrentOptionIndex()) {
                AppSharedPreference.getInstance().setBitcoinUnit(UnitUtilWrapper
                        .BitcoinUnitWrapper.values()[index]);
                if (BitherApplication.hotActivity != null) {
                    BitherApplication.hotActivity.refreshTotalBalance();
                }
            }
        }
    };

    private SettingSelector currencySelector = new SettingSelector() {
        private int length = ExchangeUtil.Currency.values().length;

        @Override
        public int getOptionCount() {
            return length;
        }

        @Override
        public void onOptionIndexSelected(int index) {
            if (index >= 0 && index < length) {
                AppSharedPreference.getInstance().setExchangeType(ExchangeUtil.Currency.values()[index]);
            }
        }

        @Override
        public String getSettingName() {
            return getString(R.string.setting_name_currency);
        }

        @Override
        public String getOptionName(int index) {
            if (index >= 0 && index < length) {
                return ExchangeUtil.Currency.values()[index].getSymbol() + " " + ExchangeUtil
                        .Currency.values()[index].getName();
            }
            return ExchangeUtil.Currency.values()[0].getSymbol() + " " + ExchangeUtil.Currency
                    .values()[0].getName();
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
                case High:
                    return getString(R.string.setting_name_transaction_fee_high);
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
            BitherjSettings.TransactionFeeMode mode = AppSharedPreference.getInstance()
                    .getTransactionFeeMode();
            switch (mode) {
                case High:
                    return 0;
                case Low:
                    return 2;
                default:
                    return 1;
            }
        }

        private BitherjSettings.TransactionFeeMode getModeByIndex(int index) {
            if (index >= 0 && index < BitherjSettings.TransactionFeeMode.values().length) {
                switch (index) {
                    case 0:
                        return BitherjSettings.TransactionFeeMode.High;
                    case 1:
                        return BitherjSettings.TransactionFeeMode.Normal;
                    case 2:
                        return BitherjSettings.TransactionFeeMode.Low;
                }
            }
            return BitherjSettings.TransactionFeeMode.Normal;
        }

        @Override
        public String getOptionNote(int index) {
            switch (getModeByIndex(index)) {
                case Low:
                    return getString(R.string.setting_name_transaction_fee_low_note);
                case High:
                    return getString(R.string.setting_name_transaction_fee_high_note);
                default:
                    return getString(R.string.setting_name_transaction_fee_normal_note);
            }
        }

        @Override
        public Drawable getOptionDrawable(int index) {
            return null;
        }
    };

    private OnClickListener switchToColdClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            DialogConfirmTask dialog = new DialogConfirmTask(getActivity(),
                    getStyledConfirmString(getString(R.string
                            .launch_sequence_switch_to_cold_warn)), new Runnable() {
                @Override
                public void run() {
                    ThreadUtil.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            AppSharedPreference.getInstance().setAppMode(BitherjSettings.AppMode
                                    .COLD);
                            startActivity(new Intent(getActivity(), ChooseModeActivity.class));
                            getActivity().overridePendingTransition(R.anim.activity_in_drop, 0);
                            getActivity().finish();
                        }
                    });
                }
            });
            dialog.show();
        }

        private SpannableString getStyledConfirmString(String str) {
            int firstLineEnd = str.indexOf("\n");
            SpannableString spn = new SpannableString(str);
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.red)), 0,
                    firstLineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spn.setSpan(new StyleSpan(Typeface.BOLD), 0, firstLineEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spn.setSpan(new RelativeSizeSpan(0.8f), firstLineEnd, str.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spn;
        }
    };

    private OnClickListener checkClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if ((AddressManager.getInstance().getPrivKeyAddresses() == null
                        || AddressManager.getInstance().getPrivKeyAddresses().size() == 0)
                    && !AddressManager.getInstance().hasHDMKeychain()
                    && !AddressManager.getInstance().hasHDAccountHot()) {
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

    private OnClickListener monitorClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            monitorUtil = new MonitorBitherColdUtil(OptionHotFragment.this, new
                    MonitorBitherColdUtil.MonitorBitherColdUtilDelegate() {
                @Override
                public void onAddressMonitored(ArrayList<String> addresses) {
                    monitorUtil = null;
                    if (getActivity() instanceof HotActivity) {
                        HotActivity hot = (HotActivity) getActivity();
                        Intent intent = new Intent();
                        intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG,
                                addresses);
                        hot.onActivityResult(BitherSetting.INTENT_REF.SCAN_REQUEST_CODE, Activity
                                .RESULT_OK, intent);
                    }
                }
            });
            monitorUtil.scan();
        }
    };

    private OnClickListener monitorColdHDClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (AddressManager.getInstance().hasHDAccountMonitored()) {
                DropdownMessage.showDropdownMessage(getActivity(), R.string
                        .monitor_cold_hd_account_limit);
                return;
            }
            startActivityForResult(new Intent(getActivity(), ScanActivity.class),
                    MonitorCodeHDRequestCode);
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
        if (monitorUtil != null && monitorUtil.onActivityResult(requestCode, resultCode, data)) {
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
            case MonitorCodeHDRequestCode:
                if (data.getExtras().containsKey(ScanActivity.INTENT_EXTRA_RESULT)) {
                    final String content = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                    try {
                        Base58.decodeChecked(content);
                    }catch (AddressFormatException e){
                        try {
                            final boolean isXRandom = content.indexOf(QRCodeUtil.XRANDOM_FLAG) == 0;
                            Utils.hexStringToByteArray(isXRandom ? content.substring(1) : content);
                            DropdownMessage.showDropdownMessage(getActivity(), R.string.hd_account_monitor_xpub_need_to_upgrade);
                        } catch (Exception ex) {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            DropdownMessage.showDropdownMessage(getActivity(), R.string
                                    .monitor_cold_hd_account_failed);
                        }
                        return;
                    }
                    try {
                        new ThreadNeedService(dp, getActivity()) {
                            @Override
                            public void runWithService(BlockchainService service) {
                                if (service != null) {
                                    service.stopAndUnregister();
                                }
                                try {
                                    HDAccount account = new HDAccount(DeterministicKey.deserializeB58(content).getPubKeyExtended(),
                                            false, false, null);
                                    AddressManager.getInstance().setHDAccountMonitored(account);
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (dp.isShowing()) {
                                                dp.dismiss();
                                            }
                                            DropdownMessage.showDropdownMessage(getActivity(), R
                                                    .string.monitor_cold_hd_account_success);
                                        }
                                    });
                                } catch (MnemonicException.MnemonicLengthException e) {
                                    e.printStackTrace();
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (dp.isShowing()) {
                                                dp.dismiss();
                                            }
                                            DropdownMessage.showDropdownMessage(getActivity(), R
                                                    .string.monitor_cold_hd_account_failed);
                                        }
                                    });
                                } catch (HDAccount.DuplicatedHDAccountException e) {
                                    e.printStackTrace();
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (dp.isShowing()) {
                                                dp.dismiss();
                                            }
                                            DropdownMessage.showDropdownMessage(getActivity(), R
                                                    .string
                                                    .monitor_cold_hd_account_failed_duplicated);
                                        }
                                    });
                                } catch (AddressFormatException e){
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (dp.isShowing()) {
                                                dp.dismiss();
                                            }
                                            DropdownMessage.showDropdownMessage(getActivity(), R
                                                    .string
                                                    .hd_account_monitor_xpub_need_to_upgrade);
                                        }
                                    });
                                }
                                if (service != null) {
                                    service.startAndRegister();
                                }
                            }
                        }.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (dp.isShowing()) {
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(getActivity(), R.string
                                .monitor_cold_hd_account_failed);
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
        ssvBitcoinUnit = (SettingSelectorView) view.findViewById(R.id.ssv_bitcoin_unit);
        tvVersion = (TextView) view.findViewById(R.id.tv_version);
        tvWebsite = (TextView) view.findViewById(R.id.tv_website);
        tvWebsite.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        ivLogo = (ImageView) view.findViewById(R.id.iv_logo);
        btnSwitchToCold = (Button) view.findViewById(R.id.btn_switch_to_cold);
        llSwitchToCold = view.findViewById(R.id.ll_switch_to_cold);
        btnAvatar = (Button) view.findViewById(R.id.btn_avatar);
        btnCheck = (Button) view.findViewById(R.id.btn_check_private_key);
        btnAdvance = (Button) view.findViewById(R.id.btn_advance);
        view.findViewById(R.id.btn_monitor_hd).setOnClickListener(monitorColdHDClick);
        view.findViewById(R.id.btn_monitor).setOnClickListener(monitorClick);
        ssvCurrency.setSelector(currencySelector);
        ssvMarket.setSelector(marketSelector);
        ssvTransactionFee.setSelector(transactionFeeModeSelector);
        ssvBitcoinUnit.setSelector(bitcoinUnitSelector);
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
        btnSwitchToCold.setOnClickListener(switchToColdClick);
        btnCheck.setOnClickListener(checkClick);
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
                c.drawBitmap(avatar, null, new Rect(borderPadding, borderPadding, result.getWidth
                        () - borderPadding, result.getHeight() - borderPadding), null);
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

    private void configureSwitchToCold() {
        final Runnable check = new Runnable() {
            @Override
            public void run() {
                if (AddressManager.getInstance().getAllAddresses().size() > 0 || AddressManager
                        .getInstance().getTrashAddresses().size() > 0 || AddressManager
                        .getInstance().getHdmKeychain() != null || AddressManager.getInstance()
                        .hasHDAccountHot() || AddressManager.getInstance().hasHDAccountMonitored()) {
                    llSwitchToCold.setVisibility(View.GONE);
                } else {
                    llSwitchToCold.setVisibility(View.VISIBLE);
                }
            }
        };
        if (AbstractApp.addressIsReady) {
            check.run();
        } else {
            new Thread() {
                @Override
                public void run() {
                    AddressManager.getInstance().getAllAddresses();
                    ThreadUtil.runOnMainThread(check);
                }
            }.start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        configureSwitchToCold();
    }

    @Override
    public void onSelected() {
        configureSwitchToCold();
    }
}
