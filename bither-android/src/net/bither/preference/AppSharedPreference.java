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

package net.bither.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import net.bither.BitherApplication;
import net.bither.BitherSetting.AppMode;
import net.bither.BitherSetting.MarketType;
import net.bither.model.PasswordSeed;
import net.bither.util.ExchangeUtil.ExchangeType;
import net.bither.util.StringUtil;
import net.bither.util.TransactionsUtil;
import net.bither.util.TransactionsUtil.TransactionFeeMode;

import java.util.Date;
import java.util.Locale;

public class AppSharedPreference {
    private static final String APP_BITHER = "app_bither";
    private static final String PREFS_KEY_LAST_VERSION = "last_version";
    private static final String DEFAULT_MARKET = "default_market";
    private static final String DEFAULT_EXCHANGE_RATE = "default_exchange_rate";
    private static final String APP_MODE = "app_mode";
    private static final String LAST_CHECK_PRIVATE_KEY_TIME = "last_check_private_key_time";
    private static final String LAST_BACK_UP_PRIVATE_KEY_TIME = "last_back_up_private_key_time";
    private static final String HAS_PRIVATE_KEY = "has_private_key";
    private static final String TRANSACTION_FEE_MODE = "transaction_fee_mode";
    // from service
    private static final String SYNC_BLOCK_ONLY_WIFI = "sync_block_only_wifi";
    private static final String PREFS_KEY_CONNECTIVITY_NOTIFICATION = "connectivity_notification";
    private static final String PREFS_KEY_BTC_PRECISION = "btc_precision";
    private static final String PREFS_DEFAULT_BTC_PRECISION = "4";
    private static final String DOWNLOAD_SPV_FINISH = "download_spv_finish";
    private static final String PASSWORD_SEED = "password_seed";
    private static final String USER_AVATAR = "user_avatar";

    private static AppSharedPreference mInstance = new AppSharedPreference();
    private SharedPreferences mPreferences;

    private AppSharedPreference() {
        this.mPreferences = BitherApplication.mContext.getSharedPreferences(
                APP_BITHER, Context.MODE_MULTI_PROCESS);
    }

    public static AppSharedPreference getInstance() {
        return mInstance;
    }

    public int getVerionCode() {
        return this.mPreferences.getInt(PREFS_KEY_LAST_VERSION, 0);
    }

    public void setVerionCode(int versionCode) {
        this.mPreferences.edit().putInt(PREFS_KEY_LAST_VERSION, versionCode)
                .commit();
    }

    public MarketType getDefaultMarket() {
        MarketType marketType = getMarketType();
        if (marketType == null) {
            setDefault();
        }
        marketType = getMarketType();
        return marketType;

    }

    private MarketType getMarketType() {
        int type = this.mPreferences.getInt(DEFAULT_MARKET, -1);
        if (type == -1) {
            return null;
        }
        return MarketType.values()[type];

    }

    public void setMarketType(MarketType marketType) {
        this.mPreferences.edit().putInt(DEFAULT_MARKET, marketType.ordinal())
                .commit();
    }

    private void setDefault() {
        String defaultCountry = Locale.getDefault().getCountry();
        if (StringUtil.compareString(defaultCountry, "CN")
                || StringUtil.compareString(defaultCountry, "cn")) {
            setExchangeType(ExchangeType.CNY);
            setMarketType(MarketType.HUOBI);
        } else {
            setExchangeType(ExchangeType.USD);
            setMarketType(MarketType.BITSTAMP);
        }

    }

    public ExchangeType getDefaultExchangeRate() {
        ExchangeType exchangeType = getExchangeType();
        if (exchangeType == null) {
            setDefault();
        }
        exchangeType = getExchangeType();
        return exchangeType;

    }

    private ExchangeType getExchangeType() {
        int type = this.mPreferences.getInt(DEFAULT_EXCHANGE_RATE, -1);
        if (type == -1) {
            return null;
        }
        return ExchangeType.values()[type];

    }

    public void setExchangeType(ExchangeType exchangeType) {
        this.mPreferences.edit()
                .putInt(DEFAULT_EXCHANGE_RATE, exchangeType.ordinal()).commit();
    }

    public AppMode getAppMode() {
        int index = mPreferences.getInt(APP_MODE, -1);
        if (index < 0 || index >= AppMode.values().length) {
            return null;
        }
        return AppMode.values()[index];
    }

    public void setAppMode(AppMode mode) {
        int index = -1;
        if (mode != null) {
            index = mode.ordinal();
        }
        mPreferences.edit().putInt(APP_MODE, index).commit();
    }

    public Date getLastCheckPrivateKeyTime() {
        Date date = null;
        long time = mPreferences.getLong(LAST_CHECK_PRIVATE_KEY_TIME, 0);
        if (time > 0) {
            date = new Date(time);
        }
        return date;
    }

    public void setLastCheckPrivateKeyTime(Date date) {
        if (date != null) {
            mPreferences.edit()
                    .putLong(LAST_CHECK_PRIVATE_KEY_TIME, date.getTime())
                    .commit();
        }

    }

    public Date getLastBackupkeyTime() {
        Date date = null;
        long time = mPreferences.getLong(LAST_BACK_UP_PRIVATE_KEY_TIME, 0);
        if (time > 0) {
            date = new Date(time);
        }
        return date;
    }

    public void setLastBackupKeyTime(Date date) {
        if (date != null) {
            mPreferences.edit()
                    .putLong(LAST_BACK_UP_PRIVATE_KEY_TIME, date.getTime())
                    .commit();
        }
    }

    public boolean hasPrivateKey() {
        return mPreferences.getBoolean(HAS_PRIVATE_KEY, false);
    }

    public void setHasPrivateKey(boolean hasPrivateKey) {
        mPreferences.edit().putBoolean(HAS_PRIVATE_KEY, hasPrivateKey).commit();
    }

    public void clear() {
        mPreferences.edit().clear().commit();
    }

    // from service
    public String getPrecision() {
        return mPreferences.getString(PREFS_KEY_BTC_PRECISION,
                PREFS_DEFAULT_BTC_PRECISION);
    }

    public void registerOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener sharedPreferenceChangeListener) {
        mPreferences
                .registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    public void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener sharedPreferenceChangeListener) {
        mPreferences
                .unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    public boolean getNotificationFlag() {
        return mPreferences.getBoolean(PREFS_KEY_CONNECTIVITY_NOTIFICATION,
                false);
    }

    public boolean getSyncBlockOnlyWifi() {
        return mPreferences.getBoolean(SYNC_BLOCK_ONLY_WIFI, false);
    }

    public void setSyncBlockOnlyWifi(boolean onlyWifi) {
        this.mPreferences.edit().putBoolean(SYNC_BLOCK_ONLY_WIFI, onlyWifi)
                .commit();
    }

    public boolean getDownloadSpvFinish() {
        return mPreferences.getBoolean(DOWNLOAD_SPV_FINISH, false);
    }

    public void setDownloadSpvFinish(boolean finish) {
        this.mPreferences.edit().putBoolean(DOWNLOAD_SPV_FINISH, finish)
                .commit();
    }

    public PasswordSeed getPasswordSeed() {
        String str = this.mPreferences.getString(PASSWORD_SEED, "");
        if (StringUtil.isEmpty(str)) {
            return null;
        }
        return new PasswordSeed(str);
    }

    public void setPasswordSeed(PasswordSeed passwordSeed) {
        this.mPreferences.edit()
                .putString(PASSWORD_SEED, passwordSeed.toString()).commit();

    }

    public TransactionFeeMode getTransactionFeeMode() {
        int ordinal = this.mPreferences.getInt(TRANSACTION_FEE_MODE, 0);
        if (ordinal < TransactionFeeMode.values().length && ordinal >= 0) {
            return TransactionFeeMode.values()[ordinal];
        }
        return TransactionFeeMode.Normal;
    }

    public void setTransactionFeeMode(TransactionFeeMode mode) {
        if (mode == null) {
            mode = TransactionFeeMode.Normal;
        }
        this.mPreferences.edit().putInt(TRANSACTION_FEE_MODE, mode.ordinal())
                .commit();
        TransactionsUtil.configureMinFee(mode.getMinFeeSatoshi());
    }

    public boolean hasUserAvatar() {
        return !StringUtil.isEmpty(getUserAvatar());
    }

    public String getUserAvatar() {
        return this.mPreferences.getString(USER_AVATAR, "");
    }

    public void setUserAvatar(String avatar) {
        this.mPreferences.edit().putString(USER_AVATAR, avatar).commit();
    }

}
