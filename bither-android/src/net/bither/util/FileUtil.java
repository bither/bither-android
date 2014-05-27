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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.pi.common.util.NativeUtil;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.BitherSetting.AppMode;
import net.bither.preference.AppSharedPreference;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileUtil {

    // old watch only dir
    private static final String WALLET_WATCH_ONLY_OLD = "w";

    // old tickerName file
    private static final String HUOBI_TICKER_NAME = "huobi.ticker";
    private static final String BITSTAMP_TICKER_NAME = "bitstamp.ticker";
    private static final String BTCE_TICKER_NAME = "btce.ticker";
    private static final String OKCOIN_TICKER_NAME = "okcoin.ticker";
    private static final String CHBTC_TICKER_NAME = "chbtc.ticker";
    private static final String BTCCHINA_TICKER_NAME = "btcchina.ticker";

    private static final String WALLET_ROM_CACHE = "wallet";
    private static final String WALLET_WATCH_ONLY = "watch";
    private static final String WALLET_HOT = "hot";
    private static final String WALLET_COLD = "cold";
    private static final String WALLET_ERROR = "error";
    private static final String UNSIGN_TXS = "unsign_txs";

    private static final String BITHER_BACKUP_SDCARD_DIR = "BitherBackup";
    private static final String BITHER_BACKUP_ROM_DIR = "backup";

    private static final String BITHER_BACKUP_HOT_FILE_NAME = "keys";

    private static final String WALLET_SEQUENCE_WATCH_ONLY = "sequence_watch_only";
    private static final String WALLET_SEQUENCE_PRIVATE = "sequence_private";

    private static final String EXCAHNGE_TICKER_NAME = "exchange.ticker";
    private static final String EXCHANGE_KLINE_NAME = "exchange.kline";
    private static final String EXCHANGE_DEPTH_NAME = "exchange.depth";
    private static final String ADDRESS_LIST = "address_list";

    private static final String EXCHANGERATE = "exchangerate";
    private static final String MARKET_CAHER = "mark";

    private static final String IMAGE_CACHE_DIR = "image";
    private static final String IMAGE_SHARE_FILE_NAME = "share.jpg";

    private static final String IMAGE_CACHE_UPLOAD = IMAGE_CACHE_DIR + "/upload";
    private static final String IMAGE_CACHE_612 = IMAGE_CACHE_DIR + "image/612";
    private static final String IMAGE_CACHE_150 = IMAGE_CACHE_DIR + "image/150";

    // share to weibo
    private static int MAX_SHARE_SIZE = 618;

    /**
     * sdCard exist
     */
    public static boolean existSdCardMounted() {
        String storageState = android.os.Environment.getExternalStorageState();
        if (StringUtil.isEmpty(storageState)) {
            return false;
        }
        return StringUtil.compareString(storageState,
                android.os.Environment.MEDIA_MOUNTED);
    }

    public static File getSDPath() {
        File sdDir = Environment.getExternalStorageDirectory();
        return sdDir;
    }

    public static File getBackupSdCardDir() {
        File backupDir = new File(getSDPath(), BITHER_BACKUP_SDCARD_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        return backupDir;
    }

    public static File getBackupFileOfCold() {
        File file = new File(getBackupSdCardDir(),
                DateTimeUtil.getNameForFile(System.currentTimeMillis())
                        + ".bak"
        );
        return file;
    }

    public static List<File> getBackupFileListOfCold() {
        File dir = getBackupSdCardDir();
        List<File> fileList = new ArrayList<File>();
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            files = orderByDateDesc(files);
            for (File file : files) {
                if (StringUtil.checkBackupFileOfCold(file.getName())) {
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }

    private static File getBackupRomDir() {
        File backupDir = new File(getWalletRomCache(), BITHER_BACKUP_ROM_DIR);

        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        return backupDir;
    }

    public static File getBackupKeyOfHot() {
        File backupDir = getBackupRomDir();
        return new File(backupDir, BITHER_BACKUP_HOT_FILE_NAME);
    }

    public static File getDiskDir(String dirName, Boolean createNomedia) {

        File dir = getDiskCacheDir(BitherApplication.mContext, dirName);
        if (!dir.exists()) {
            dir.mkdirs();
            if (createNomedia) {
                try {
                    File noMediaFile = new File(dir, ".nomedia");
                    noMediaFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return dir;
    }


    public static Uri saveShareImage(Bitmap bmp) {
        File dir = getDiskDir(IMAGE_CACHE_DIR, true);
        File jpg = new File(dir, IMAGE_SHARE_FILE_NAME);
        float w = bmp.getWidth();
        float h = bmp.getHeight();
        float sampleSize = 1;
        if (w > MAX_SHARE_SIZE) {
            sampleSize = MAX_SHARE_SIZE / w;
        }
        w = (int) (w * sampleSize);
        h = (int) (h * sampleSize);
        bmp = ImageManageUtil.getMatrixBitmap(bmp, (int) w, (int) h, true);
        NativeUtil.compressBitmap(bmp, 65, jpg.getAbsolutePath(), true);
        return Uri.fromFile(jpg);
    }

    public static File getExternalCacheDir(Context context) {
        if (SdkUtils.hasFroyo()) {

            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName()
                + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath()
                + cacheDir);
    }

    public static File getErrorLogFile() {
        File path = new File(BitherApplication.mContext.getExternalCacheDir(),
                "bither");
        return new File(path, "error.log");
    }

    public static File getDiskCacheDir(Context context, String uniqueName) {
        File extCacheDir = getExternalCacheDir(context);
        final String cachePath = (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState()) || !isExternalStorageRemovable())
                && extCacheDir != null ? extCacheDir.getPath() : context
                .getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    @TargetApi(9)
    public static boolean isExternalStorageRemovable() {
        if (SdkUtils.hasGingerbread()) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    public static File getWalletErrorFile(String address) {
        File file = getWatchErrorDir();
        return new File(file, address);

    }

    public static File getPrivateCacheDir() {
        File file = getWalletRomCache();
        String dirName = WALLET_HOT;
        if (AppSharedPreference.getInstance().getAppMode() == AppMode.COLD) {
            dirName = WALLET_COLD;
        }
        file = new File(file, dirName);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static File getWalletFileFromPrivate(String address) {
        File file = null;
        file = getPrivateCacheDir();
        return new File(file, address);
    }

    public static File getWarmPrivateAddressSequenceFile() {
        File dir = getWalletRomCache();
        File file = new File(dir, WALLET_SEQUENCE_PRIVATE);
        return file;
    }

    public static File getWatchOnlyAddressSequenceFile() {
        File dir = getWalletRomCache();
        File file = new File(dir, WALLET_SEQUENCE_WATCH_ONLY);
        return file;
    }

    public static File getAddressListFile() {
        File file = BitherApplication.mContext.getFileStreamPath(ADDRESS_LIST);
        return file;
    }

    public static File getWalletRomCache() {
        File file = BitherApplication.mContext.getDir(WALLET_ROM_CACHE,
                Context.MODE_PRIVATE);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static File getUnSignTxsCacheFile() {
        File file = getWalletRomCache();
        return new File(file, UNSIGN_TXS);
    }

    public static File getWatchOnlyCacheDir() {
        File file = getWalletRomCache();
        file = new File(file, WALLET_WATCH_ONLY);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static File getOldWatchOnlyCacheDir() {
        File file = getWalletRomCache();
        file = new File(file, WALLET_WATCH_ONLY_OLD);
        return file;
    }

    public static File getWatchErrorDir() {
        File file = getWalletRomCache();
        file = new File(file, WALLET_ERROR);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static File getWalletFileFromWatch(String address) {
        File file = getWatchOnlyCacheDir();
        return new File(file, address);
    }

    public static File getBlockChainFile() {
        return new File(BitherApplication.mContext.getDir(
                BitherSetting.BLOCKCHAIN_FOLDER, Context.MODE_PRIVATE),
                BitherSetting.BLOCKCHAIN_FILENAME
        );
    }

    private static File getMarketCache() {
        return getDiskDir(MARKET_CAHER, false);

    }

    public static File getUploadImageDir() {
        return getDiskDir(IMAGE_CACHE_UPLOAD, true);
    }

    public static File getAvatarDir() {
        return getDiskDir(IMAGE_CACHE_612, true);
    }

    public static File getSmallAvatarDir() {
        return getDiskDir(IMAGE_CACHE_150, true);
    }

    public static File getExchangeRateFile() {
        File file = getDiskDir("", false);
        return new File(file, EXCHANGERATE);
    }

    public static File getTickerFile() {
        File file = getMarketCache();
        file = new File(file, EXCAHNGE_TICKER_NAME);
        return file;

    }

    public static File getKlineFile() {
        File file = getMarketCache();
        file = new File(file, EXCHANGE_KLINE_NAME);
        return file;
    }

    public static File getDepthFile() {
        File file = getMarketCache();
        file = new File(file, EXCHANGE_DEPTH_NAME);
        return file;
    }

    public static void writeFile(byte[] data, File tar) throws IOException {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(tar);
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    @SuppressWarnings("resource")
    public static Object deserialize(File file) {
        Object object = new Object();
        FileInputStream fos = null;
        try {
            if (!file.exists()) {
                return null;
            }
            fos = new FileInputStream(file);
            ObjectInputStream ois;
            ois = new ObjectInputStream(fos);
            object = ois.readObject();

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return object;
    }

    public static void serializeObject(File file, Object object) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
            oos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        }

    }

    public static File[] orderByDateDesc(File[] fs) {
        Arrays.sort(fs, new Comparator<File>() {
            public int compare(File f1, File f2) {
                long diff = f1.lastModified() - f2.lastModified();
                if (diff > 0) {
                    return 1;
                } else if (diff == 0) {
                    return 0;
                } else {
                    return -1;
                }
            }

            public boolean equals(Object obj) {
                return true;
            }

        });
        return fs;
    }

    public static void copyFile(File src, File tar) throws Exception {

        if (src.isFile()) {
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            try {
                InputStream is = new FileInputStream(src);
                bis = new BufferedInputStream(is);
                OutputStream op = new FileOutputStream(tar);
                bos = new BufferedOutputStream(op);
                byte[] bt = new byte[8192];
                int len = bis.read(bt);
                while (len != -1) {
                    bos.write(bt, 0, len);
                    len = bis.read(bt);
                }
                bis.close();
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

            }

        } else if (src.isDirectory()) {
            File[] files = src.listFiles();
            tar.mkdir();
            for (int i = 0;
                 i < files.length;
                 i++) {
                copyFile(files[i].getAbsoluteFile(),
                        new File(tar.getAbsoluteFile() + File.separator
                                + files[i].getName())
                );
            }
        } else {
            throw new FileNotFoundException();
        }

    }

    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath);
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private static void delAllFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            return;
        }
        String[] tempList = file.list();
        if (tempList == null) {
            return;
        }
        File temp = null;
        for (int i = 0;
             i < tempList.length;
             i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);
                delFolder(path + "/" + tempList[i]);
            }
        }
    }

    public static String readFile(File file) {
        if (!file.exists()) {
            return null;
        }
        FileInputStream is = null;
        ByteArrayOutputStream arrayOutputStream = null;
        try {
            is = new FileInputStream(file);
            byte[] bytes = new byte[1024];

            arrayOutputStream = new ByteArrayOutputStream();
            while (is.read(bytes) != -1) {
                arrayOutputStream.write(bytes, 0, bytes.length);
            }
            is.close();
            arrayOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new String(arrayOutputStream.toByteArray());

    }

    public static void upgradeTickerFile() {
        File marketDir = getMarketCache();
        File file = new File(marketDir, BITSTAMP_TICKER_NAME);
        fileExistAndDelete(file);
        file = new File(marketDir, BTCE_TICKER_NAME);
        fileExistAndDelete(file);
        file = new File(marketDir, HUOBI_TICKER_NAME);
        fileExistAndDelete(file);
        file = new File(marketDir, OKCOIN_TICKER_NAME);
        fileExistAndDelete(file);
        file = new File(marketDir, CHBTC_TICKER_NAME);
        fileExistAndDelete(file);
        file = new File(marketDir, BTCCHINA_TICKER_NAME);
        fileExistAndDelete(file);
    }

    public static boolean fileExistAndDelete(File file) {
        return file.exists() && file.delete();

    }

    public static File convertUriToFile(Activity activity, Uri uri) {
        File file = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            @SuppressWarnings("deprecation")
            Cursor actualimagecursor = activity.managedQuery(uri, proj, null,
                    null, null);
            if (actualimagecursor != null) {
                int actual_image_column_index = actualimagecursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                actualimagecursor.moveToFirst();
                String img_path = actualimagecursor
                        .getString(actual_image_column_index);
                if (!StringUtil.isEmpty(img_path)) {
                    file = new File(img_path);
                }
            } else {

                file = new File(new URI(uri.toString()));
                if (file.exists()) {
                    return file;
                }

            }
        } catch (Exception e) {
        }
        return file;

    }

    public static int getOrientationOfFile(String fileName) {
        int orientation = 0;
        try {
            ExifInterface exif = new ExifInterface(fileName);
            String orientationString = exif
                    .getAttribute(ExifInterface.TAG_ORIENTATION);
            if (StringUtil.isNubmer(orientationString)) {
                int orc = Integer.valueOf(orientationString);
                switch (orc) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        orientation = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        orientation = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        orientation = 270;
                        break;
                    default:
                        break;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return orientation;

    }


}
