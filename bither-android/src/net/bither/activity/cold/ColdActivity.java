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

package net.bither.activity.cold;

import static net.bither.BitherSetting.IS_ANDROID11_OR_HIGHER;
import static net.bither.util.FileHandlePresenter.REQUEST_CODE_READ_FILE_FROM_EXTERNAL;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.NotificationAndroidImpl;
import net.bither.R;
import net.bither.adapter.cold.ColdFragmentPagerAdapter;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitpieHDAccountCold;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.EncryptedData;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicCode;
import net.bither.bitherj.crypto.mnemonic.MnemonicWordList;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.fragment.Unselectable;
import net.bither.fragment.cold.CheckFragment;
import net.bither.fragment.cold.ColdAddressFragment;
import net.bither.mnemonic.MnemonicCodeAndroid;
import net.bither.model.Check;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.BaseFragmentActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.TabButton;
import net.bither.ui.base.dialog.DialogColdAddressCount;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogFirstRunWarning;
import net.bither.ui.base.dialog.DialogGenerateAddressFinalConfirm;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.ProgressDialog;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.BackupUtil;
import net.bither.util.FileHandlePresenter;
import net.bither.util.FileUtil;
import net.bither.util.KeyUtil;
import net.bither.util.LogUtil;
import net.bither.util.PermissionUtil;
import net.bither.util.StringUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ColdActivity extends BaseFragmentActivity {

    private TabButton tbtnMessage;
    private TabButton tbtnMain;
    private TabButton tbtnMe;
    private FrameLayout flAddPrivateKey;
    private ColdFragmentPagerAdapter mAdapter;
    private ViewPager mPager;
    private ProgressDialog pd;
    private final AddressIsLoadedReceiver addressIsLoadedReceiver = new AddressIsLoadedReceiver();
    private FileHandlePresenter fileHandlePresenter;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        BitherApplication.coldActivity = this;
        setContentView(R.layout.activity_cold);
        initView();
        fileHandlePresenter = new FileHandlePresenter(this);
        mPager.postDelayed(new Runnable() {

            @Override
            public void run() {
                initClick();
                mPager.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Fragment f = getActiveFragment();
                        if (f instanceof Selectable) {
                            ((Selectable) f).onSelected();
                        }
                        BackupUtil.backupColdKey(true);
                    }
                }, 100);

            }
        }, 500);
        DialogFirstRunWarning.show(this);
        registerReceiver(addressIsLoadedReceiver, new IntentFilter(NotificationAndroidImpl
                .ACTION_ADDRESS_LOAD_COMPLETE_STATE));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            configureTabArrow();
            ArrayList<String> addresses = (ArrayList<String>) data.getExtras().getSerializable
                    (BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG);
            if (addresses != null && addresses.size() > 0) {
                Address a = WalletUtils.findPrivateKey(addresses.get(0));
                if (a != null && a.hasPrivKey() && !a.isFromXRandom()) {
                    new DialogGenerateAddressFinalConfirm(this, addresses.size(),
                            a.isFromXRandom()).show();
                }

                Fragment f = getFragmentAtIndex(1);
                if (f != null && f instanceof ColdAddressFragment) {
                    ColdAddressFragment af = (ColdAddressFragment) f;
                    af.showAddressesAdded(addresses);

                }
                if (data.getExtras().getBoolean(BitherSetting.INTENT_REF
                        .ADD_PRIVATE_KEY_SUGGEST_CHECK_TAG, false)) {
                    new DialogConfirmTask(this,
                            getString(R.string.first_add_private_key_check_suggest),
                            new Runnable() {
                                @Override
                                public void run() {
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mPager.setCurrentItem(0, true);
                                            mPager.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Fragment f = getFragmentAtIndex(0);
                                                    if (f != null && f instanceof CheckFragment) {
                                                        CheckFragment c = (CheckFragment) f;
                                                        c.check();
                                                    }
                                                }
                                            }, 300);
                                        }
                                    });
                                }
                            }).show();
                }
                if (f != null && f instanceof Refreshable) {
                    Refreshable r = (Refreshable) f;
                    r.doRefresh();
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_READ_FILE_FROM_EXTERNAL && resultCode == Activity.RESULT_OK) {
            try {
                Uri fileUri = data.getData();
                if (fileUri == null) {
                    showApiQExportFailure();
                    return;
                }
                File file = FileUtil.uriToFileApiQ(ColdActivity.this, fileUri);
                if (file != null) {
                    if (file.isFile() && StringUtil.checkBackupFileOfCold(file.getName())) {
                        showDialogPassword(file);
                    } else {
                        DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(ColdActivity.this,
                                getString(R.string.recover_from_backup_no_supoprt_file_type),
                                getString(R.string.recover_from_backup_reselect),
                                getString(R.string.cancel),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        fileHandlePresenter.requestReadExternalStorage();
                                    }
                                }
                        );
                        dialogConfirmTask.show();
                    }
                } else {
                    showApiQExportFailure();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showApiQExportFailure();
            }
        }
    }

    private void showApiQExportFailure() {
        DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(ColdActivity.this,
                getString(R.string.recover_from_backup_error_alert),
                getString(R.string.recover_from_backup_reselect),
                getString(R.string.cancel),
                new Runnable() {
            @Override
            public void run() {
                fileHandlePresenter.requestReadExternalStorage();
            }
        });
        dialogConfirmTask.show();
    }

    private void initClick() {
        flAddPrivateKey.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (AddressManager.isPrivateLimit()) {
                    DropdownMessage.showDropdownMessage(ColdActivity.this,
                            R.string.private_key_count_limit);
                    return;
                }
                Intent intent = new Intent(ColdActivity.this, AddColdAddressActivity.class);
                startActivityForResult(intent, BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
                overridePendingTransition(R.anim.activity_in_drop, R.anim.activity_out_back);
            }
        });
    }

    private void initView() {
        flAddPrivateKey = (FrameLayout) findViewById(R.id.fl_add_address);

        tbtnMain = (TabButton) findViewById(R.id.tbtn_main);
        tbtnMessage = (TabButton) findViewById(R.id.tbtn_message);
        tbtnMe = (TabButton) findViewById(R.id.tbtn_me);

        configureTopBarSize();

        tbtnMain.setIconResource(R.drawable.tab_main, R.drawable.tab_main_checked);
        tbtnMessage.setIconResource(R.drawable.tab_guard, R.drawable.tab_guard_checked);
        tbtnMe.setIconResource(R.drawable.tab_option, R.drawable.tab_option_checked);
        tbtnMain.setDialog(new DialogColdAddressCount(this));
        configureTabArrow();

        mPager = (ViewPager) findViewById(R.id.pager);
        mAdapter = new ColdFragmentPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(1);
        mPager.setOffscreenPageLimit(2);
        mPager.setOnPageChangeListener(new PageChangeListener(new TabButton[]{tbtnMessage,
                tbtnMain, tbtnMe}, mPager));

    }

    private class PageChangeListener implements OnPageChangeListener {
        private List<TabButton> indicators;
        private ViewPager pager;

        public PageChangeListener(TabButton[] buttons, ViewPager viewPager) {
            this.indicators = new ArrayList<TabButton>();
            this.pager = viewPager;
            int size = buttons.length;
            for (int i = 0;
                 i < size;
                 i++) {
                TabButton button = buttons[i];
                indicators.add(button);
                if (pager.getCurrentItem() == i) {
                    button.setChecked(true);
                }
                button.setOnClickListener(new IndicatorClick(i));
            }

        }

        public void onPageScrollStateChanged(int state) {

        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        private class IndicatorClick implements OnClickListener {

            private int position;

            public IndicatorClick(int position) {
                this.position = position;
            }

            public void onClick(View v) {
                if (pager.getCurrentItem() != position) {
                    pager.setCurrentItem(position, true);
                } else {
                    if (getActiveFragment() instanceof Refreshable) {
                        ((Refreshable) getActiveFragment()).doRefresh();
                    }
                    if (position == 1) {
                        tbtnMain.showDialog();
                    }
                }
            }
        }

        public void onPageSelected(int position) {

            if (position >= 0 && position < indicators.size()) {
                for (int i = 0;
                     i < indicators.size();
                     i++) {
                    indicators.get(i).setChecked(i == position);
                    if (i != position) {
                        Fragment f = getFragmentAtIndex(i);
                        if (f instanceof Unselectable) {
                            ((Unselectable) f).onUnselected();
                        }
                    }
                }
            }
            Fragment mFragment = getActiveFragment();
            if (mFragment instanceof Selectable) {
                ((Selectable) mFragment).onSelected();
            }
        }
    }

    public Fragment getActiveFragment() {
        Fragment localFragment = null;
        if (this.mPager == null) {
            return localFragment;
        }
        localFragment = getFragmentAtIndex(mPager.getCurrentItem());
        return localFragment;
    }

    public Fragment getFragmentAtIndex(int i) {
        String str = StringUtil.makeFragmentName(this.mPager.getId(), i);
        return getSupportFragmentManager().findFragmentByTag(str);
    }

    public void scrollToFragmentAt(int index) {
        if (mPager.getCurrentItem() != index) {
            mPager.setCurrentItem(index, true);
        }
    }

    private void configureTopBarSize() {
        int sideBarSize = UIUtil.getScreenWidth() / 3 - UIUtil.getScreenWidth() / 18;
        tbtnMessage.getLayoutParams().width = sideBarSize;
        tbtnMe.getLayoutParams().width = sideBarSize;
    }

    private void configureTabArrow() {
        if (AddressManager.getInstance().getAllAddresses().size() > 0) {
            tbtnMain.setArrowVisible(true, true);
        } else {
            tbtnMain.setArrowVisible(false, false);
        }
    }

    private void checkBackup() {
        if (AddressManager.getInstance().getPrivKeyAddresses() != null && AddressManager
                .getInstance().getPrivKeyAddresses().size() == 0 && AddressManager
                .getInstance().getHdmKeychain() == null
                && !AddressManager.getInstance().hasHDAccountCold()
                && !AddressManager.getInstance().hasBitpieHDAccountCold()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final List<File> files = FileUtil.getBackupFileListOfCold();
                    if (files != null && files.size() > 0) {
                        showDialogOfColdBackup(files.get(0));
                    } else if (IS_ANDROID11_OR_HIGHER) {
                        if (FileUtil.isExistBackupSDCardDirOfCold(false) || FileUtil.isExistBackupSDCardDirOfCold(true)) {
                            showDialogOfColdBackup(null);
                        }
                    }
                }
            }).start();

        }
    }

    private void showDialogOfColdBackup(final File bcackupFile) {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                DialogConfirmTask dialogConfirmTask;
                if (bcackupFile == null) {
                    dialogConfirmTask = new DialogConfirmTask(ColdActivity.this,
                            getString(R.string.recover_from_backup_of_android11_or_higher_cold),
                            getString(R.string.recover_from_backup_select),
                            getString(R.string.cancel),
                            new Runnable() {
                                @Override
                                public void run() {
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            fileHandlePresenter.requestReadExternalStorage();
                                        }
                                    });
                                }
                            }
                    );
                } else {
                    dialogConfirmTask = new DialogConfirmTask(ColdActivity.this, getString(R.string.recover_from_backup_of_cold), new Runnable() {
                        @Override
                        public void run() {
                            ThreadUtil.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    showDialogPassword(bcackupFile);
                                }
                            });
                        }
                    });
                }
                dialogConfirmTask.show();
            }
        });
    }

    private void showDialogPassword(final File bcackupFile) {
        DialogPassword dialogPassword = new DialogPassword(ColdActivity.this,
                new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(SecureCharSequence password) {
                        importWalletFromBackup(bcackupFile, password);
                    }
                });
        dialogPassword.setCheckPre(false);
        dialogPassword.show();
    }

    private void importWalletFromBackup(final File file, final SecureCharSequence password) {
        LogUtil.d("backup", file.getName());
        if (pd == null) {
            pd = new ProgressDialog(ColdActivity.this, getString(R.string.please_wait), null);
        }
        pd.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] strings = BackupUtil.getBackupKeyStrList(file);
                HDMKeychain hdmKeychain = null;
                boolean check = false;
                if (strings != null && strings.length > 0) {
                    MnemonicCode firstMnemonicCode = getMnemonicCode(strings[0]);
                    int hdQrCodeFlagLength = firstMnemonicCode == null ? 0 : firstMnemonicCode.getMnemonicWordList().getHdQrCodeFlag().length();
                    String firstStr = strings[0];
                    if (firstStr.indexOf(QRCodeUtil.HDM_QR_CODE_FLAG) == 0 || hdQrCodeFlagLength != 0) {
                        String keychainString;
                        if (firstStr.contains(QRCodeUtil.ADD_MODE_QR_CODE_FLAG)) {
                            keychainString = firstStr.substring(hdQrCodeFlagLength, firstStr.indexOf(QRCodeUtil.ADD_MODE_QR_CODE_FLAG));
                        } else {
                            keychainString = firstStr.substring(hdQrCodeFlagLength);
                        }
                        try {
                            check = HDMKeychain.checkPassword(firstMnemonicCode, keychainString, password);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        String passwordSeedStr;
                        if (firstStr.contains(QRCodeUtil.ADD_MODE_QR_CODE_FLAG)) {
                            passwordSeedStr = firstStr.substring(0, firstStr.indexOf(QRCodeUtil.ADD_MODE_QR_CODE_FLAG));
                        } else {
                            passwordSeedStr = firstStr;
                        }
                        PasswordSeed passwordSeed = new PasswordSeed(passwordSeedStr);
                        check = passwordSeed.checkPassword(password);
                    }
                    if (!check) {
                        checkPasswordWrong(file);
                    } else {
                        List<Address> addressList = new ArrayList<>();
                        for (String keyString : strings) {
                            String[] strs = QRCodeUtil.splitString(keyString);
                            if (strs.length != 4) {
                                continue;
                            }
                            if (keyString.indexOf(QRCodeUtil.HDM_QR_CODE_FLAG) == 0) {
                                String[] passwordSeeds = QRCodeUtil.splitOfPasswordSeed(keyString);
                                String encreyptString = Utils.joinString(new String[]{passwordSeeds[1], passwordSeeds[2], passwordSeeds[3]}, QRCodeUtil.QR_CODE_SPLIT);

                                try {
                                    hdmKeychain = new HDMKeychain(new EncryptedData(encreyptString), password, null);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                continue;
                            }

                            MnemonicCode mnemonicCode = getMnemonicCode(keyString);
                            if (mnemonicCode != null && MnemonicWordList.getMnemonicWordListForHdSeed(keyString).equals(mnemonicCode.getMnemonicWordList())) {
                                try {
                                    Address.AddMode addMode = Address.AddMode.Other;
                                    if (keyString.contains(QRCodeUtil.ADD_MODE_QR_CODE_FLAG)) {
                                        int addModeFlagIndex = keyString.indexOf(QRCodeUtil.ADD_MODE_QR_CODE_FLAG);
                                        addMode = Address.AddMode.fromValue(keyString.substring(addModeFlagIndex + 1));
                                        keyString = keyString.substring(0, addModeFlagIndex);
                                    }
                                    String[] passwordSeeds = QRCodeUtil.splitOfPasswordSeed(keyString);
                                    String encryptString = Utils.joinString(new String[]{passwordSeeds[1], passwordSeeds[2], passwordSeeds[3]}, QRCodeUtil.QR_CODE_SPLIT);
                                    if (MnemonicWordList.isHDQrCode(keyString)) {
                                        new HDAccountCold(mnemonicCode, new EncryptedData(encryptString), password, addMode);
                                        AppSharedPreference.getInstance().setMnemonicWordList(mnemonicCode.getMnemonicWordList());
                                        MnemonicCode.setInstance(mnemonicCode);
                                    } else if (MnemonicWordList.isBitpieColdQrCode(keyString)) {
                                        new BitpieHDAccountCold(mnemonicCode, new EncryptedData(encryptString), password);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                continue;
                            }

                            Address.AddMode addMode = Address.AddMode.Other;
                            if (keyString.contains(QRCodeUtil.ADD_MODE_QR_CODE_FLAG)) {
                                int addModeFlagIndex = keyString.indexOf(QRCodeUtil.ADD_MODE_QR_CODE_FLAG);
                                addMode = Address.AddMode.fromValue(keyString.substring(addModeFlagIndex + 1));
                                keyString = keyString.substring(0, addModeFlagIndex);
                            }
                            PasswordSeed passwordSeed = new PasswordSeed(keyString);
                            ECKey key = passwordSeed.getECKey(password);
                            if (key != null) {
                                Address address = new Address(key.toAddress(), key.getPubKey(),
                                        PrivateKeyUtil.getEncryptedString(key), false, key.isFromXRandom());
                                address.setAddMode(addMode);
                                addressList.add(address);
                                key.clearPrivateKey();
                            }
                        }

                        KeyUtil.addAddressListByDesc(null, addressList);
                        if (hdmKeychain != null) {
                            KeyUtil.setHDKeyChain(hdmKeychain);
                        }
                        password.wipe();
                        recoverBackupSuccess();
                    }

                }
            }
        }).start();
    }

    private MnemonicCode getMnemonicCode(String string) {
        MnemonicWordList mnemonicWordList = MnemonicWordList.getMnemonicWordListForHdSeed(string);
        if (mnemonicWordList == null && string.equals(MnemonicWordList.getBitpieColdQrCodeFlag())) {
            mnemonicWordList = MnemonicWordList.English;
        }
        MnemonicCode mnemonicCode = null;
        if (mnemonicWordList != null) {
            try {
                mnemonicCode = new MnemonicCodeAndroid();
                mnemonicCode.setMnemonicWordList(mnemonicWordList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mnemonicCode;
    }

    private void checkPasswordWrong(final File backupFile) {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (pd != null) {
                    pd.dismiss();
                }
                DropdownMessage.showDropdownMessage(ColdActivity.this, R.string.password_wrong);
                showDialogPassword(backupFile);
            }
        });
    }

    private void recoverBackupSuccess() {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (pd != null) {
                    pd.dismiss();
                }
                Fragment fragment = getFragmentAtIndex(1);
                if (fragment instanceof Refreshable) {
                    Refreshable refreshable = (Refreshable) fragment;
                    refreshable.doRefresh();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        BitherApplication.coldActivity = null;
        unregisterReceiver(addressIsLoadedReceiver);
        super.onDestroy();
    }

    private final class AddressIsLoadedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !Utils.compareString(intent.getAction(), NotificationAndroidImpl.ACTION_ADDRESS_LOAD_COMPLETE_STATE)) {
                return;
            }
            Fragment fragment = getFragmentAtIndex(1);
            if (fragment != null && fragment instanceof ColdAddressFragment) {
                ((ColdAddressFragment) fragment).refresh();
            }
            if (!PermissionUtil.isWriteAndReadExternalStoragePermission(ColdActivity.this, BitherSetting.REQUEST_CODE_PERMISSION_WRITE_AND_READ_EXTERNAL_STORAGE)) {
                return;
            }
            checkBackup();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case BitherSetting.REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE:
            case BitherSetting.REQUEST_CODE_PERMISSION_WRITE_AND_READ_EXTERNAL_STORAGE:
                if (grantResults != null && grantResults.length > 0) {
                    boolean isResult = true;
                    for (int grantResult: grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            isResult = false;
                            break;
                        }
                    }
                    if (isResult) {
                        if (requestCode == BitherSetting.REQUEST_CODE_PERMISSION_WRITE_AND_READ_EXTERNAL_STORAGE) {
                            checkBackup();
                        }
                    } else {
                        DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(
                                this, getString(R.string.permissions_no_grant), new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        });
                        dialogConfirmTask.show();
                    }
                }
                break;
            default:
                break;
        }
    }

}
