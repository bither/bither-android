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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.EncryptedData;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.fragment.Unselectable;
import net.bither.fragment.cold.CheckFragment;
import net.bither.fragment.cold.ColdAddressFragment;
import net.bither.image.glcrop.Util;
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
import net.bither.util.FileUtil;
import net.bither.util.KeyUtil;
import net.bither.util.LogUtil;
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

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        BitherApplication.coldActivity = this;
        setContentView(R.layout.activity_cold);
        initView();
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
        registerReceiver(addressIsLoadedReceiver,
                new IntentFilter(NotificationAndroidImpl.ACTION_ADDRESS_LOAD_COMPLETE_STATE));
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
    }

    private void initClick() {
        flAddPrivateKey.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (WalletUtils.isPrivateLimit()) {
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<File> files = FileUtil.getBackupFileListOfCold();
                if (files != null && files.size() > 0) {
                    showDialogOfColdBackup();
                }
            }
        }).start();

    }

    private void showDialogOfColdBackup() {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(ColdActivity.this,
                        getString(R.string.recover_from_backup_of_cold), passwordRunnable);
                dialogConfirmTask.show();
            }
        });

    }

    Runnable passwordRunnable = new Runnable() {
        @Override
        public void run() {
            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    showDialogPassword();
                }
            });
        }
    };

    private void showDialogPassword() {
        DialogPassword dialogPassword = new DialogPassword(ColdActivity.this,
                new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(SecureCharSequence password) {
                        importWalletFromBackup(password);
                    }
                });
        dialogPassword.setCheckPre(false);
        dialogPassword.show();
    }

    private void importWalletFromBackup(final SecureCharSequence password) {
        List<File> fileList = FileUtil.getBackupFileListOfCold();
        final File file = fileList.get(0);
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
                    if (strings[0].indexOf(QRCodeUtil.HDM_QR_CODE_FLAG) == 0) {
                        String keychainString = strings[0].substring(1);
                        try {

                            check = HDMKeychain.checkPassword(keychainString, password);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        PasswordSeed passwordSeed = new PasswordSeed(strings[0]);
                        check = passwordSeed.checkPassword(password);
                    }
                    if (!check) {
                        checkPasswordWrong();
                    } else {
                        List<Address> addressList = new
                                ArrayList<Address>();
                        for (String keyString : strings) {
                            String[] strs = QRCodeUtil.splitString(keyString);
                            if (strs.length != 4) {
                                continue;
                            }
                            if (keyString.indexOf(QRCodeUtil.HDM_QR_CODE_FLAG) == 0) {
                                String[] passwordSeeds = QRCodeUtil.splitOfPasswordSeed(keyString);
                                String encreyptString = Utils.joinString(new String[]{passwordSeeds[1], passwordSeeds[2], passwordSeeds[3]}, QRCodeUtil.QR_CODE_SPLIT);

                                try {
                                    hdmKeychain = new HDMKeychain(new EncryptedData(encreyptString)
                                            , password, null);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                continue;
                            }

                            PasswordSeed passwordSeed = new PasswordSeed(keyString);
                            ECKey key = passwordSeed.getECKey(password);
                            if (key != null) {
                                Address address = new Address(key.toAddress(), key.getPubKey(),
                                        PrivateKeyUtil.getEncryptedString(key), key.isFromXRandom());
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

    private void checkPasswordWrong() {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (pd != null) {
                    pd.dismiss();
                }
                DropdownMessage.showDropdownMessage(ColdActivity.this, R.string.password_wrong);
                showDialogPassword();
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
            if (AddressManager.getInstance().getPrivKeyAddresses() != null
                    && AddressManager.getInstance().getPrivKeyAddresses().size() == 0 && AddressManager.getInstance().getHdmKeychain() == null) {
                checkBackup();
            }
        }
    }
}
