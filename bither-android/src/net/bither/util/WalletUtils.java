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

package net.bither.util;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.VerificationException;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.wallet.WalletTransaction;
import com.google.bitcoin.wallet.WalletTransaction.Pool;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.BitherSetting.AppMode;
import net.bither.model.AddressInfo;
import net.bither.model.BitherAddress;
import net.bither.model.BitherAddressProtobufSerializer;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.model.BitherAddressWithPrivateKeyProtobufSerializer;
import net.bither.model.PasswordSeed;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;

import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WalletUtils {
    public static final RelativeSizeSpan SMALLER_SPAN = new RelativeSizeSpan(
            0.85f);
    public static final FileFilter KEYS_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(final File file) {
            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(file), BitherSetting.UTF_8));
                WalletUtils.readKeys(reader);

                return true;
            } catch (final IOException x) {
                return false;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException x) {
                        // swallow
                    }
                }
            }
        }
    };
    static final byte[] EMPTY_BYTES = new byte[32];
    private static final Logger log = LoggerFactory
            .getLogger(WalletUtils.class);
    private static final byte[] addressLock = new byte[0];
    private static final byte[] addressSequenceLock = new byte[0];
    private static final Pattern P_SIGNIFICANT = Pattern.compile("^([-+]"
            + BitherSetting.CHAR_THIN_SPACE + ")?\\d*(\\.\\d{0,2})?");
    private static final Object SIGNIFICANT_SPAN = new StyleSpan(Typeface.BOLD);
    private static List<BitherAddressWithPrivateKey> privateAddressList;
    private static List<BitherAddress> watchOnlyAddressList;

    public static Editable formatAddress(@Nonnull final Address address,
                                         final int groupSize, final int lineSize) {
        return formatHash(address.toString(), groupSize, lineSize);
    }

    public static Editable formatAddress(@Nullable final String prefix,
                                         @Nonnull final Address address, final int groupSize,
                                         final int lineSize) {
        return formatHash(prefix, address.toString(), groupSize, lineSize,
                BitherSetting.CHAR_THIN_SPACE);
    }

    public static Editable formatHash(@Nonnull final String address,
                                      final int groupSize, final int lineSize) {
        return formatHash(null, address, groupSize, lineSize,
                BitherSetting.CHAR_THIN_SPACE);
    }

    public static long longHash(@Nonnull final Sha256Hash hash) {
        final byte[] bytes = hash.getBytes();

        return (bytes[31] & 0xFFl) | ((bytes[30] & 0xFFl) << 8)
                | ((bytes[29] & 0xFFl) << 16) | ((bytes[28] & 0xFFl) << 24)
                | ((bytes[27] & 0xFFl) << 32) | ((bytes[26] & 0xFFl) << 40)
                | ((bytes[25] & 0xFFl) << 48) | ((bytes[23] & 0xFFl) << 56);
    }

    public static Editable formatHash(@Nullable final String prefix,
                                      @Nonnull final String address, final int groupSize,
                                      final int lineSize, final char groupSeparator) {
        final SpannableStringBuilder builder = prefix != null ? new SpannableStringBuilder(
                prefix) : new SpannableStringBuilder();

        final int len = address.length();
        for (int i = 0;
             i < len;
             i += groupSize) {
            final int end = i + groupSize;
            final String part = address.substring(i, end < len ? end : len);

            builder.append(part);
            builder.setSpan(new TypefaceSpan("monospace"), builder.length()
                            - part.length(), builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            if (end < len) {
                final boolean endOfLine = lineSize > 0 && end % lineSize == 0;
                builder.append(endOfLine ? '\n' : groupSeparator);
            }
        }

        return builder;
    }

    public static void formatSignificant(@Nonnull final Editable s,
                                         @Nullable final RelativeSizeSpan
                                                 insignificantRelativeSizeSpan) {
        s.removeSpan(SIGNIFICANT_SPAN);
        if (insignificantRelativeSizeSpan != null) {
            s.removeSpan(insignificantRelativeSizeSpan);
        }

        final Matcher m = P_SIGNIFICANT.matcher(s);
        if (m.find()) {
            final int pivot = m.group().length();
            s.setSpan(SIGNIFICANT_SPAN, 0, pivot,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (s.length() > pivot && insignificantRelativeSizeSpan != null) {
                s.setSpan(insignificantRelativeSizeSpan, pivot, s.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static BigInteger localValue(@Nonnull final BigInteger btcValue,
                                        @Nonnull final BigInteger rate) {
        return btcValue.multiply(rate).divide(GenericUtils.ONE_BTC);
    }

    public static BigInteger btcValue(@Nonnull final BigInteger localValue,
                                      @Nonnull final BigInteger rate) {
        return localValue.multiply(GenericUtils.ONE_BTC).divide(rate);
    }

    @CheckForNull
    public static Address getFirstFromAddress(@Nonnull final Transaction tx) {
        if (tx.isCoinBase()) {
            return null;
        }

        try {
            for (final TransactionInput input : tx.getInputs()) {
                return input.getFromAddress();
            }

            throw new IllegalStateException();
        } catch (final ScriptException x) {
            // this will happen on inputs connected to coinbase transactions
            return null;
        }
    }

    @CheckForNull
    public static Address getFirstToAddress(@Nonnull final Transaction tx) {
        try {
            for (final TransactionOutput output : tx.getOutputs()) {
                return output.getScriptPubKey().getToAddress(
                        BitherSetting.NETWORK_PARAMETERS);
            }

            throw new IllegalStateException();
        } catch (final ScriptException x) {
            return null;
        }
    }

    public static boolean isInternal(@Nonnull final Transaction tx) {
        if (tx.isCoinBase()) {
            return false;
        }

        final List<TransactionOutput> outputs = tx.getOutputs();
        if (outputs.size() != 1) {
            return false;
        }

        try {
            final TransactionOutput output = outputs.get(0);
            final Script scriptPubKey = output.getScriptPubKey();
            if (!scriptPubKey.isSentToRawPubKey()) {
                return false;
            }

            return true;
        } catch (final ScriptException x) {
            return false;
        }
    }

    public static void writeKeys(@Nonnull final Writer out,
                                 @Nonnull final List<ECKey> keys) throws IOException {
        final DateFormat format = Iso8601Format.newDateTimeFormatT();

        out.write("# KEEP YOUR PRIVATE KEYS SAFE! Anyone who can read this can spend your " +
                "Bitcoins.\n");

        for (final ECKey key : keys) {
            out.write(key
                    .getPrivateKeyEncoded(BitherSetting.NETWORK_PARAMETERS)
                    .toString());
            if (key.getCreationTimeSeconds() != 0) {
                out.write(' ');
                out.write(format.format(new Date(key.getCreationTimeSeconds()
                        * DateUtils.SECOND_IN_MILLIS)));
            }
            out.write('\n');
        }
    }

    public static List<ECKey> readKeys(@Nonnull final BufferedReader in)
            throws IOException {
        try {
            final DateFormat format = Iso8601Format.newDateTimeFormatT();

            final List<ECKey> keys = new LinkedList<ECKey>();

            while (true) {
                final String line = in.readLine();
                if (line == null) {
                    break; // eof
                }
                if (line.trim().isEmpty() || line.charAt(0) == '#') {
                    continue; // skip comment
                }

                final String[] parts = line.split(" ");

                final ECKey key = new DumpedPrivateKey(
                        BitherSetting.NETWORK_PARAMETERS, parts[0]).getKey();
                key.setCreationTimeSeconds(parts.length >= 2 ? format.parse(
                        parts[1]).getTime()
                        / DateUtils.SECOND_IN_MILLIS : 0);

                keys.add(key);
            }

            return keys;
        } catch (final AddressFormatException x) {
            throw new IOException("cannot read keys", x);
        } catch (final ParseException x) {
            throw new IOException("cannot read keys", x);
        }
    }

    @CheckForNull
    public static ECKey pickOldestKey(@Nonnull final Wallet wallet) {
        ECKey oldestKey = null;

        for (final ECKey key : wallet.getKeys())
            if (!wallet.isKeyRotating(key)) {
                if (oldestKey == null
                        || key.getCreationTimeSeconds() < oldestKey
                        .getCreationTimeSeconds()) {
                    oldestKey = key;
                }
            }

        return oldestKey;
    }

    public static void saveBitherAddress(BitherAddress bitherAddress) {
        synchronized (addressLock) {
            if (!bitherAddress.isError()) {
                try {
                    if (bitherAddress.hasPrivateKey()) {
                        File walletFile = FileUtil
                                .getWalletFileFromPrivate(bitherAddress
                                        .getAddress());
                        LogUtil.i("save wallet", bitherAddress.toString());
                        WalletUtils.protobufSerializeWallet(bitherAddress,
                                walletFile);

                    } else {
                        File walletFile = FileUtil
                                .getWalletFileFromWatch(bitherAddress
                                        .getAddress());
                        LogUtil.i("save wallet", bitherAddress.toString());
                        WalletUtils.protobufSerializeWallet(bitherAddress,
                                walletFile);
                    }

                } catch (final IOException x) {
                    throw new RuntimeException(x);
                }
            }
        }
    }

    public static void saveWallet() {
        try {
            synchronized (addressLock) {
                if (privateAddressList != null && privateAddressList.size() > 0) {
                    for (BitherAddress bitherAddress : privateAddressList) {
                        if (!bitherAddress.isError()) {
                            File walletFile = FileUtil
                                    .getWalletFileFromPrivate(bitherAddress
                                            .getAddress());
                            LogUtil.i("save wallet", bitherAddress.toString());
                            WalletUtils.protobufSerializeWallet(bitherAddress,
                                    walletFile);
                        }
                    }
                }
                if (watchOnlyAddressList != null
                        && watchOnlyAddressList.size() > 0) {
                    for (BitherAddress bitherAddress : watchOnlyAddressList) {
                        if (!bitherAddress.isError()) {
                            File walletFile = FileUtil
                                    .getWalletFileFromWatch(bitherAddress
                                            .getAddress());
                            LogUtil.i("save wallet", bitherAddress.toString());
                            WalletUtils.protobufSerializeWallet(bitherAddress,
                                    walletFile);
                        }
                    }
                }
            }
        } catch (final IOException x) {
            throw new RuntimeException(x);
        }
    }

    public static void protobufSerializeWallet(@Nonnull final Wallet wallet,
                                               File walletFile) throws IOException {
        final long start = System.currentTimeMillis();

        wallet.saveToFile(walletFile);

        // make wallets world accessible in test mode
        if (BitherSetting.TEST) {
            Io.chmod(walletFile, 0777);
        }

        LogUtil.d("application", "wallet saved to: '" + walletFile + "', took "
                + (System.currentTimeMillis() - start) + "ms");
    }

    public static void initWalletList() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<BitherAddressWithPrivateKey> privates = initPrivateWallet
                        (readPrivateAddressSequence());
                List<String> watchOnlySequence = readWatchOnlyAddressSequence();
                final List<BitherAddress> watchOnlys = initWatchOnlyWallet(watchOnlySequence);
                final List<BitherAddress> errors = initErrorWallet(watchOnlySequence);
                AppMode appMode = AppSharedPreference.getInstance()
                        .getAppMode();
                initAddressList(watchOnlys, errors, privates);
                if (appMode == AppMode.HOT) {
                    if (privateAddressList.size() + watchOnlyAddressList.size() > 0) {
                        sendTotalBroadcast();
                    }
                    BroadcastUtil.sendBroadcastDowloadBlockState();
                }
                BroadcastUtil.sendBroadcastAddressLoadCompleteState(true);

            }
        }).start();

    }

    public static void sendTotalBroadcast() {
        if (AppSharedPreference.getInstance().getAppMode() == AppMode.HOT) {
            BigInteger privateKeyBig = getPrivateKeyTotal();
            BigInteger watchOnlyBig = getWatchOnlyTotal();
            BroadcastUtil.sendBroadcastTotalBitcoinState(privateKeyBig, watchOnlyBig);
        }
    }

    private static void initAddressList(List<BitherAddress> watchOnlys,
                                        List<BitherAddress> errors,
                                        List<BitherAddressWithPrivateKey> privates) {
        watchOnlys.addAll(errors);
        synchronized (addressLock) {
            privateAddressList = privates;
            watchOnlyAddressList = watchOnlys;
        }
    }

    private static File[] sortAddressFile(File[] fs, final List<String> sequence) {
        Arrays.sort(fs, new Comparator<File>() {
            public int compare(File f1, File f2) {
                long diff = sequence.indexOf(f1.getName())
                        - sequence.indexOf(f2.getName());
                if (diff > 0) {
                    return 1;
                } else if (diff == 0) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        return fs;
    }

    private static List<BitherAddressWithPrivateKey> initPrivateWallet(
            List<String> sequence) {
        File dir = FileUtil.getPrivateCacheDir();
        File[] fs = dir.listFiles();
        if (sequence != null) {
            fs = sortAddressFile(fs, sequence);
        }
        ArrayList<BitherAddressWithPrivateKey> addresses = new
                ArrayList<BitherAddressWithPrivateKey>();
        for (File walletFile : fs) {
            String name = walletFile.getName();
            if (StringUtil.validBicoinAddress(name)) {
                BitherAddressWithPrivateKey bit = (BitherAddressWithPrivateKey)
                        loadAddressWithPrivateKeyFromProtobuf(walletFile);

                if (bit != null) {
                    bit.setError(false);
                    bit.autosaveToFile(walletFile, 1, TimeUnit.SECONDS, null);
                    AddressInfo addressInfo = new AddressInfo(bit);
                    bit.setAddressInfo(addressInfo);
                    if (!bit.isConsistent()) {
                        Log.e("wallet error", name + " :error of isConsistent");
                    } else {
                        Log.d("wallet", name + " :sucess");
                    }
                    addresses.add(bit);
                }
            }
        }
        BackupUtil.backupColdKey(true);

        return addresses;
    }

    private static List<BitherAddress> initWatchOnlyWallet(List<String> sequence) {
        File file = FileUtil.getWatchOnlyCacheDir();
        File[] fs = file.listFiles();
        if (sequence != null) {
            fs = sortAddressFile(fs, sequence);
        }
        ArrayList<BitherAddress> addresses = new ArrayList<BitherAddress>();
        for (File walletFile : fs) {
            String name = walletFile.getName();
            if (StringUtil.validBicoinAddress(name)) {
                BitherAddress bit = loadWalletFromProtobuf(walletFile);
                if (bit != null) {
                    bit.setError(false);
                    bit.autosaveToFile(walletFile, 1, TimeUnit.SECONDS, null);
                    AddressInfo addressInfo = new AddressInfo(bit);
                    bit.setAddressInfo(addressInfo);
                    if (!bit.isConsistent()) {
                        Log.e("wallet error", name + " :error of isConsistent");
                    } else {
                        Log.d("wallet", name + " :sucess");
                    }
                    addresses.add(bit);

                }
            }
        }
        return addresses;
    }

    private static List<BitherAddress> initErrorWallet(List<String> sequence) {
        File file = FileUtil.getWatchErrorDir();
        File[] fs = file.listFiles();
        if (sequence != null) {
            fs = sortAddressFile(fs, sequence);
        }
        ArrayList<BitherAddress> addresses = new ArrayList<BitherAddress>();
        for (File walletFile : fs) {
            BitherAddress bit = loadWalletFromProtobuf(walletFile);
            if (bit != null) {
                bit.setError(true);
                addresses.add(bit);
            }
        }
        return addresses;
    }

    private static BigInteger getWatchOnlyTotal() {
        BigInteger total = BigInteger.ZERO;
        if (watchOnlyAddressList != null && watchOnlyAddressList.size() > 0) {
            for (BitherAddress bitherAddress : watchOnlyAddressList) {
                if (!bitherAddress.isError()) {
                    total = total.add(bitherAddress
                            .getBalance(BalanceType.ESTIMATED));
                }
            }
        }
        return total;
    }

    private static BigInteger getPrivateKeyTotal() {
        BigInteger total = BigInteger.ZERO;
        if (privateAddressList != null && privateAddressList.size() > 0) {
            for (BitherAddress bitherAddress : privateAddressList) {
                if (!bitherAddress.isError()) {
                    total = total.add(bitherAddress
                            .getBalance(BalanceType.ESTIMATED));
                }
            }
        }
        return total;
    }

    public static void addAddressWithPrivateKey(
            final BlockchainService blockchainService,
            final BitherAddressWithPrivateKey address) {
        address.setAddressInfo(new AddressInfo(address));
        AppSharedPreference prefs = AppSharedPreference.getInstance();
        if (prefs.getPasswordSeed() == null) {
            prefs.setPasswordSeed(new PasswordSeed(address));
        }
        synchronized (addressLock) {
            final AppMode appMode = AppSharedPreference.getInstance()
                    .getAppMode();
            if (appMode == AppMode.HOT && blockchainService != null) {
                blockchainService.stopPeerGroup();
            }
            final boolean hasAddress = privateAddressList.contains(address);
            if (!hasAddress) {
                privateAddressList.add(0, address);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        savePrivateAddressSequence();
                        File walletFile = FileUtil
                                .getWalletFileFromPrivate(address.getAddress());
                        LogUtil.i("save wallet", address.toString());
                        try {
                            WalletUtils.protobufSerializeWallet(address,
                                    walletFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        address.autosaveToFile(walletFile, 1, TimeUnit.SECONDS,
                                null);
                        if (appMode == AppMode.HOT && blockchainService != null) {
                            blockchainService
                                    .beginInitBlockAndWalletInUiThread();
                        }

                        BackupUtil.backupColdKey(false);
                        BackupUtil.backupHotKey();

                    }

                }).start();
            }
        }
    }

    public static void addBitherAddress(
            final BlockchainService blockchainService,
            final List<BitherAddress> addresses) {
        final List<BitherAddress> addList = new ArrayList<BitherAddress>();
        for (BitherAddress bitherAddress : addresses) {
            if (!watchOnlyAddressList.contains(bitherAddress)) {
                bitherAddress.setAddressInfo(new AddressInfo(bitherAddress));
                addList.add(bitherAddress);
            }
        }
        synchronized (addressLock) {
            final AppMode appMode = AppSharedPreference.getInstance()
                    .getAppMode();
            if (appMode == AppMode.HOT && blockchainService != null) {
                blockchainService.stopPeerGroup();
            }
            for (BitherAddress bitherAddress : addList) {
                watchOnlyAddressList.add(0, bitherAddress);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    saveWatchOnlyAddressSequence();
                    for (BitherAddress bitherAddress : addList) {
                        File walletFile = FileUtil
                                .getWalletFileFromWatch(bitherAddress.getAddress());
                        LogUtil.i("save wallet", bitherAddress.toString());
                        try {
                            WalletUtils.protobufSerializeWallet(bitherAddress,
                                    walletFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        bitherAddress.autosaveToFile(walletFile, 1, TimeUnit.SECONDS,
                                null);
                    }
                    if (appMode == AppMode.HOT && blockchainService != null) {
                        blockchainService
                                .beginInitBlockAndWalletInUiThread();
                    }

                }
            }).start();

        }
    }

    private static void savePrivateAddressSequence() {
        synchronized (addressSequenceLock) {
            if (privateAddressList != null && privateAddressList.size() > 0) {
                ArrayList<String> addressSequenceList = new ArrayList<String>();
                for (BitherAddress a : privateAddressList) {
                    addressSequenceList.add(a.getAddress());
                }
                if (addressSequenceList != null
                        && addressSequenceList.size() > 0) {
                    File file = FileUtil.getWarmPrivateAddressSequenceFile();
                    FileUtil.serializeObject(file, addressSequenceList);
                }
            }
        }
    }

    private static void saveWatchOnlyAddressSequence() {
        synchronized (addressSequenceLock) {
            if (watchOnlyAddressList != null && watchOnlyAddressList.size() > 0) {
                ArrayList<String> addressSequenceList = new ArrayList<String>();
                for (BitherAddress a : watchOnlyAddressList) {
                    addressSequenceList.add(a.getAddress());
                }
                if (addressSequenceList != null
                        && addressSequenceList.size() > 0) {
                    File file = FileUtil.getWatchOnlyAddressSequenceFile();
                    FileUtil.serializeObject(file, addressSequenceList);
                }
            }
        }
    }

    private static List<String> readPrivateAddressSequence() {
        synchronized (addressSequenceLock) {
            File file = FileUtil.getWarmPrivateAddressSequenceFile();
            if (file.exists()) {
                ArrayList<String> addresses = (ArrayList<String>) FileUtil
                        .deserialize(file);
                return addresses;
            } else {
                return null;
            }
        }
    }

    private static List<String> readWatchOnlyAddressSequence() {
        synchronized (addressSequenceLock) {
            File file = FileUtil.getWatchOnlyAddressSequenceFile();
            if (file.exists()) {
                ArrayList<String> addresses = (ArrayList<String>) FileUtil
                        .deserialize(file);
                return addresses;
            } else {
                return null;
            }
        }
    }

    public static void moveWalletToError(BitherAddress bitherAddress) {
        File walletFile = FileUtil.getWalletFileFromWatch(bitherAddress
                .getAddress());
        File walletError = FileUtil.getWalletErrorFile(bitherAddress
                .getAddress());
        try {
            FileUtil.copyFile(walletFile, walletError);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (walletFile.exists()) {
            walletFile.delete();
        }

    }

    public static void removeBitherAddress(final BitherAddress bitherAddress) {
        if (AppSharedPreference.getInstance().getAppMode() == AppMode.HOT) {
            synchronized (addressLock) {
                if (watchOnlyAddressList.contains(bitherAddress)) {
                    watchOnlyAddressList.remove(bitherAddress);
                    LogUtil.d("bitherAddress",
                            "remove:" + bitherAddress.getAddress());
                }
                new ThreadNeedService(null, BitherApplication.mContext) {

                    @Override
                    public void runWithService(BlockchainService service) {
                        if (service != null) {
                            service.stopPeerGroup();
                        }
                        File walletFile = null;
                        if (bitherAddress.isError()) {
                            walletFile = FileUtil
                                    .getWalletErrorFile(bitherAddress
                                            .getAddress());
                        } else {
                            walletFile = FileUtil
                                    .getWalletFileFromWatch(bitherAddress
                                            .getAddress());
                        }
                        if (walletFile.exists()) {
                            walletFile.delete();
                        }
                        saveWatchOnlyAddressSequence();
                        BroadcastUtil.sendBroadcastProgressState(1.0);
                        sendTotalBroadcast();
                        if (service != null) {
                            service.beginInitBlockAndWalletInUiThread();
                        }

                    }
                }.start();

            }
        }
    }

    public static List<BitherAddressWithPrivateKey> getPrivateAddressList() {
        synchronized (addressLock) {
            return privateAddressList;
        }
    }

    public static BitherAddressWithPrivateKey findPrivateKey(String address) {
        for (BitherAddressWithPrivateKey bitherAddressWithPrivateKey : getPrivateAddressList()) {

            if (StringUtil.compareString(address,
                    bitherAddressWithPrivateKey.getAddress())) {
                return bitherAddressWithPrivateKey;
            }
        }
        return null;
    }

    public static BitherAddress findAddress(String address) {
        List<BitherAddress> watchonlys = getWatchOnlyAddressList();
        for (BitherAddress a : watchonlys) {
            if (StringUtil.compareString(address, a.getAddress())) {
                return a;
            }
        }
        List<BitherAddressWithPrivateKey> privates = getPrivateAddressList();
        for (BitherAddressWithPrivateKey a : privates) {
            if (StringUtil.compareString(address, a.getAddress())) {
                return a;
            }
        }
        return null;
    }

    public static List<BitherAddress> getWatchOnlyAddressList() {
        synchronized (addressLock) {
            ArrayList<BitherAddress> addresses = new ArrayList<BitherAddress>();
            if (watchOnlyAddressList != null) {
                addresses.addAll(watchOnlyAddressList);
            }
            return addresses;
        }
    }

    public static boolean hasAnyAddresses() {
        synchronized (addressLock) {
            List<BitherAddressWithPrivateKey> privates = getPrivateAddressList();
            List<BitherAddress> watchOnlys = getWatchOnlyAddressList();
            int privateCount = privates == null ? 0 : privates.size();
            int watchOnlyCount = watchOnlys == null ? 0 : watchOnlys.size();
            return privateCount + watchOnlyCount > 0;
        }
    }

    public static List<BitherAddress> getBitherAddressList() {
        return getBitherAddressList(false);
    }

    public static List<BitherAddress> getBitherAddressList(boolean noErrorAddr) {
        synchronized (addressLock) {
            ArrayList<BitherAddress> addresses = new ArrayList<BitherAddress>();
            if (privateAddressList != null) {
                for (BitherAddress wallet : privateAddressList) {
                    if (noErrorAddr && wallet.isError()) {
                        continue;
                    }
                    addresses.add(wallet);
                }
            }
            if (watchOnlyAddressList != null) {
                for (BitherAddress wallet : watchOnlyAddressList) {
                    if (noErrorAddr && wallet.isError()) {
                        continue;
                    }
                    addresses.add(wallet);
                }
            }
            return addresses;
        }
    }

    public static void clearAddressList() {
        synchronized (addressLock) {
            privateAddressList.clear();
            watchOnlyAddressList.clear();
        }
    }

    private static BitherAddressWithPrivateKey loadAddressWithPrivateKeyFromProtobuf(
            File walletFile) {
        BitherAddressWithPrivateKey bitherAddress = null;
        if (walletFile.exists()) {
            boolean isBitcoinAddress = StringUtil.validBicoinAddress(walletFile
                    .getName());
            if (!isBitcoinAddress) {
                LogUtil.d("address", "null:" + walletFile.getName());
                return null;
            } else {
                LogUtil.d("address", "addrs:" + walletFile.getName());
            }
            final long start = System.currentTimeMillis();

            FileInputStream walletStream = null;

            try {
                walletStream = new FileInputStream(walletFile);

                bitherAddress = (BitherAddressWithPrivateKey) new
                        BitherAddressWithPrivateKeyProtobufSerializer()
                        .readWallet(walletStream);

                LogUtil.i("wallet file", "wallet loaded from: '" + walletFile
                        + "', took " + (System.currentTimeMillis() - start)
                        + "ms");
            } catch (final FileNotFoundException x) {
                LogUtil.i("wallet file", "problem loading wallet" + x);
            } catch (final UnreadableWalletException x) {
                LogUtil.i("wallet file", "problem loading wallet" + x);
            } finally {
                if (walletStream != null) {
                    try {
                        walletStream.close();
                    } catch (final IOException x) {
                        // swallow
                    }
                }
            }
            if (bitherAddress == null) {
                return bitherAddress;
            } else {
                if (!bitherAddress.getParams().equals(
                        BitherSetting.NETWORK_PARAMETERS)) {
                    throw new Error("bad wallet network parameters: "
                            + bitherAddress.getParams().getId());
                }
            }
        }
        return bitherAddress;
    }

    private static BitherAddress loadWalletFromProtobuf(File walletFile) {
        BitherAddress bitherAddress = null;
        if (walletFile.exists()) {
            boolean isBitcoinAddress = StringUtil.validBicoinAddress(walletFile
                    .getName());
            if (!isBitcoinAddress) {
                LogUtil.d("address", "null:" + walletFile.getName());
                return null;
            } else {
                LogUtil.d("address", "addrs:" + walletFile.getName());
            }
            final long start = System.currentTimeMillis();

            FileInputStream walletStream = null;

            try {
                walletStream = new FileInputStream(walletFile);

                bitherAddress = (BitherAddress) new BitherAddressProtobufSerializer()
                        .readWallet(walletStream);

                LogUtil.i("wallet file", "wallet loaded from: '" + walletFile
                        + "', took " + (System.currentTimeMillis() - start)
                        + "ms");
            } catch (final FileNotFoundException x) {
                LogUtil.i("wallet file", "problem loading wallet" + x);
            } catch (final UnreadableWalletException x) {
                LogUtil.i("wallet file", "problem loading wallet" + x);
            } finally {
                if (walletStream != null) {
                    try {
                        walletStream.close();
                    } catch (final IOException x) {
                        // swallow
                    }
                }
            }
            if (bitherAddress == null) {
                return bitherAddress;
            } else {
                if (!bitherAddress.getParams().equals(
                        BitherSetting.NETWORK_PARAMETERS)) {
                    throw new Error("bad wallet network parameters: "
                            + bitherAddress.getParams().getId());
                }
            }
        }
        return bitherAddress;
    }

    public synchronized static void addTxToWallet(final Wallet wallet,
                                                  List<Transaction> txList,
                                                  int lastBlockSeenHeight, Sha256Hash hash)
            throws VerificationException {
        wallet.clearTransactions(0);
        List<WalletTransaction> walletTransactions = getWalletTx(wallet, txList);
        WalletTransaction preWalletTx = null;
        double count = 0;
        double size = txList.size();
        for (WalletTransaction walletTransaction : walletTransactions) {
            if (preWalletTx != null) {
                if (preWalletTx.getTransaction().getConfidence()
                        .getAppearedAtChainHeight() == walletTransaction
                        .getTransaction().getConfidence()
                        .getAppearedAtChainHeight()) {
                    Date time = walletTransaction.getTransaction()
                            .getUpdateTime();
                    walletTransaction.getTransaction().setUpdateTime(
                            new Date(time.getTime() + 1000));

                }
            }
            count++;
            double progress = BitherSetting.SYNC_TX_PROGRESS_BLOCK_HEIGHT
                    + BitherSetting.SYNC_TX_PROGRESS_STEP1
                    + BitherSetting.SYNC_TX_PROGRESS_STEP2
                    + +BitherSetting.SYNC_TX_PROGRESS_STEP3 * (count / size);
            BroadcastUtil.sendBroadcastProgressState(progress);
            preWalletTx = walletTransaction;
        }
        for (WalletTransaction walletTransaction : walletTransactions) {
            wallet.addWalletTransaction(walletTransaction);
        }
        LogUtil.d("tx", "count:" + count);
        // no block hash in api,use Sha256Hash.zero_hash
        wallet.setLastBlockSeenHash(hash);
        wallet.setLastBlockSeenHeight(lastBlockSeenHeight);

    }

    public static List<WalletTransaction> getWalletTx(Wallet wallet,
                                                      List<Transaction> txList) {
        HashMap<String, List<Transaction>> equalsInputHashTxList = new HashMap<String,
                List<Transaction>>();
        for (Transaction tx : txList) {
            for (TransactionInput tInput : tx.getInputs()) {
                String inputHash = tInput.getOutpoint().getHash().toString();
                if (equalsInputHashTxList.containsKey(inputHash)) {
                    equalsInputHashTxList.get(inputHash).add(tx);
                } else {
                    List<Transaction> temp = new ArrayList<Transaction>();
                    temp.add(tx);
                    equalsInputHashTxList.put(inputHash, temp);
                }
            }
        }

        List<WalletTransaction> walletTransactions = new ArrayList<WalletTransaction>();
        for (Transaction tx : txList) {
            WalletTransaction walletTransaction = getTxOfWallet(wallet, tx,
                    equalsInputHashTxList, true);
            walletTransactions.add(walletTransaction);

        }
        Collections.sort(walletTransactions, new ComparatorWalletTx());
        return walletTransactions;
    }

    private static WalletTransaction getTxOfWallet(Wallet wallet,
                                                   Transaction tx,
                                                   HashMap<String,
                                                           List<Transaction>> equalsInputHashTxList,
                                                   boolean forceAddToPool) throws
            VerificationException {
        Pool pool = null;
        boolean isSendToMe = false;
        for (TransactionOutput out : tx.getOutputs()) {
            if (out.isMine(wallet)
                    && (out.getValue().compareTo(BigInteger.ZERO) > 0)) {
                isSendToMe = true;
            }
        }
        if (isSendToMe) {
            if (equalsInputHashTxList.containsKey(tx.getHashAsString())) {// make
                // input
                List<Transaction> txAsIns = equalsInputHashTxList.get(tx
                        .getHashAsString());
                boolean isSendFromMe = false;
                for (Transaction txAsIn : txAsIns) {
                    for (TransactionInput txInInPut : txAsIn.getInputs()) {
                        String txInInPutOutpointHash = txInInPut.getOutpoint()
                                .getHash().toString();
                        if (StringUtil.compareString(txInInPutOutpointHash,
                                tx.getHashAsString())) {
                            int index = (int) txInInPut.getOutpoint()
                                    .getIndex();
                            LogUtil.d("tx", "index:" + index);
                            // check output is my
                            if (index < tx.getOutputs().size()) {
                                TransactionOutput rTransactionOutput = tx
                                        .getOutput(index);
                                if (rTransactionOutput.isMine(wallet)) {
                                    pool = Pool.SPENT;
                                    isSendFromMe = true;
                                    for (TransactionOutput out : tx
                                            .getOutputs()) {
                                        if (out.isMine(wallet)
                                                && out.isAvailableForSpending()) {
                                            out.markAsSpent(txInInPut);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!isSendFromMe) {
                        pool = Pool.UNSPENT;
                    }
                }

            } else {
                if (isSendToMe) {
                    pool = Pool.UNSPENT;
                } else {
                    pool = Pool.SPENT;
                }
            }
        } else {
            pool = Pool.SPENT;
        }
        WalletTransaction walletTransaction = new WalletTransaction(pool, tx);
        return walletTransaction;
    }

    public static Transaction getLastTx(BitherAddress bit) {
        Transaction lastTransaction = null;
        List<Transaction> txs = bit.getTransactionsByTime();
        if (txs != null && txs.size() > 0) {
            lastTransaction = txs.get(0);
        }
        return lastTransaction;
    }

    public static boolean isPrivateLimit() {
        int maxPrivateKey = AppSharedPreference.getInstance().getAppMode() == AppMode.COLD ?
                BitherSetting.WATCH_ONLY_ADDRESS_COUNT_LIMIT
                : BitherSetting.PRIVATE_KEY_OF_HOT_COUNT_LIMIT;
        return WalletUtils.getPrivateAddressList() != null
                && WalletUtils.getPrivateAddressList().size() >= maxPrivateKey;
    }

    public static boolean isWatchOnlyLimit() {
        return WalletUtils.getWatchOnlyAddressList() != null
                && WalletUtils.getWatchOnlyAddressList().size() >= BitherSetting
                .WATCH_ONLY_ADDRESS_COUNT_LIMIT;
    }

    public static void fixWalletTransactions(BitherAddress wallet) {
        List<Transaction> cloneTxList = new ArrayList<Transaction>();
        for (Transaction tx : wallet.getTransactionsByTime()) {
            TransactionConfidence oldConfidence = tx.getConfidence();
            Transaction newTx = (Transaction) SerializationUtils.clone(tx);
            TransactionConfidence newConfidence = newTx.getConfidence();
            copyConfidenceListeners(oldConfidence, newConfidence);
            cloneTxList.add(newTx);
        }

        for (Transaction tx : cloneTxList) {
            for (TransactionOutput trOutput : tx.getOutputs()) {
                trOutput.markAsUnspent();
            }
        }

        List<WalletTransaction> walletTxList = getWalletTx(wallet, cloneTxList);

        wallet.clearTransactionsWithoutSave();

        for (WalletTransaction walletTransaction : walletTxList) {
            wallet.addWalletTransaction(walletTransaction);
        }
        log.info("fix wallet: " + wallet.getAddress());
        WalletUtils.saveBitherAddress(wallet);
        sendTotalBroadcast();
        BroadcastUtil.sendBroadcastAddressState(wallet);
    }

    private static final void copyConfidenceListeners(
            TransactionConfidence oldOne, TransactionConfidence newOne) {
        try {
            Field listeners = TransactionConfidence.class
                    .getDeclaredField("listeners");
            listeners.setAccessible(true);
            listeners.set(newOne, listeners.get(oldOne));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void notifyAddressInfo(BitherAddress address) {
        address.setAddressInfo(new AddressInfo(address));
        BroadcastUtil.sendBroadcastAddressState(address);
    }

    public static boolean editPassword(String oldPassword, String newPassword) {
        synchronized (addressLock) {
            List<BitherAddressWithPrivateKey> addresses = WalletUtils.getPrivateAddressList();
            try {
                ArrayList<Object> files = new ArrayList<Object>();
                for (BitherAddressWithPrivateKey a : addresses) {
                    Object f = a.disableAutoSave();
                    if (f == null) {
                        LogUtil.e("EditPassword", "auto save not disabled");
                        return false;
                    }
                    files.add(f);
                }
                for (BitherAddressWithPrivateKey a : addresses) {
                    a.decrypt(a.getKeyCrypter().deriveKey(oldPassword));
                    a.encrypt(newPassword);
                }
                for (int i = 0;
                     i < addresses.size();
                     i++) {
                    addresses.get(i).enableAutoSave(files.get(i));
                }
                AppSharedPreference.getInstance().setPasswordSeed(new PasswordSeed(addresses.get
                        (0)));
                if (AppSharedPreference.getInstance().getAppMode() == AppMode.COLD) {
                    BackupUtil.backupColdKey(false);
                } else {
                    BackupUtil.backupHotKey();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            try {
                for (BitherAddressWithPrivateKey a : addresses) {
                    File walletFile = FileUtil
                            .getWalletFileFromPrivate(a
                                    .getAddress());
                    LogUtil.i("save wallet", a.toString());
                    WalletUtils.protobufSerializeWallet(a,
                            walletFile);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    private static class ComparatorWalletTx implements
            Comparator<WalletTransaction> {

        @Override
        public int compare(WalletTransaction lhs, WalletTransaction rhs) {
            Transaction lhsTx = lhs.getTransaction();
            Transaction rhsTx = rhs.getTransaction();
            int lhsHeight = lhsTx.getConfidence().getAppearedAtChainHeight();
            int rhsHeight = rhsTx.getConfidence().getAppearedAtChainHeight();
            if (lhsHeight > rhsHeight) {
                return 1;
            } else if (lhsHeight < rhsHeight) {
                return -1;
            } else {
                if (lhs.getPool() == rhs.getPool()) {
                    return 0;
                }
                if (lhs.getPool() == Pool.SPENT) {
                    return -1;
                } else if (rhs.getPool() == Pool.SPENT) {
                    return 1;
                } else {
                    return 0;
                }
            }

        }
    }
}
