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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import net.bither.BitherApplication;
import net.bither.bitherj.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class ImageFileUtil {

    private static final String FILE_TYPE_KEY = ".jpg";
    private static final String DCIM_FILE_NAME = "IMG_%s";
    private static final String AVATAR_FILE_NAME = "a%d.jpg";

    public static File getImageForGallery(long timeMillis) {
        String pictureName = getImageNameForGallery(timeMillis);
        File dcimFile = new File(Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM, "Camera");
        if (!dcimFile.exists()) {
            dcimFile.mkdirs();
        }
        File file = new File(dcimFile, formatFileName(pictureName));
        return file;
    }

    public static String getImageNameForGallery(
            long timeMillis) {
        return String.format(DCIM_FILE_NAME,
                DateTimeUtil.getNameForDcim(timeMillis));
    }


    public static final void saveImageToDcim(Bitmap bit, int orientation,
                                               long timeMillis) {
        String pictureName = getImageNameForGallery(timeMillis);
        addPicutureToResolver(bit, pictureName, orientation, timeMillis);
    }

    private static void addPicutureToResolver(Bitmap bitmap, String pictureName, int orientation, long timeInMillis) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.TITLE, pictureName);
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, pictureName);
        v.put(MediaStore.Images.ImageColumns.DESCRIPTION, "Taken with Picamera.");
        v.put(MediaStore.MediaColumns.DATE_ADDED, timeInMillis);
        v.put(MediaStore.Images.ImageColumns.DATE_TAKEN, timeInMillis);
        v.put(MediaStore.MediaColumns.DATE_MODIFIED, timeInMillis);
        v.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation);
        v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES); // 存储路径
        ContentResolver c = BitherApplication.mContext.getContentResolver();
        if (c != null) {
            Uri uri = c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri != null) {
                try {
                    OutputStream outputStream = c.openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public static String formatFileName(String name) {
        return name + FILE_TYPE_KEY;
    }

    private static void saveExifInterface(File file, int orientation)
            throws IOException {
        ExifInterface exif = new ExifInterface(file.getAbsolutePath());
        switch (orientation) {
            case 90:
                exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                        Integer.toString(ExifInterface.ORIENTATION_ROTATE_90));
                break;
            case 180:
                exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                        Integer.toString(ExifInterface.ORIENTATION_ROTATE_180));
                break;
            case 270:
                exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                        Integer.toString(ExifInterface.ORIENTATION_ROTATE_270));
                break;
            default:
                exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                        Integer.toString(ExifInterface.ORIENTATION_NORMAL));
                break;
        }
        exif.saveAttributes();
    }

    public static File getUploadAvatarFile(String fileName) {
        File file = FileUtil.getUploadImageDir();
        return new File(file, fileName);
    }

    public static File getAvatarFile(String fileName) {
        File file = FileUtil.getAvatarDir();
        return new File(file, fileName);
    }

    public static File getSmallAvatarFile(String fileName) {
        File file = FileUtil.getSmallAvatarDir();
        return new File(file, fileName);
    }

    public static String getAvatarFileName(long time) {
        return Utils.format(AVATAR_FILE_NAME, time);
    }

    public static File getAdImageFile(String fileName) {
        File file = getAdImageFolder(fileName);
        clearUselessImageFile(file);
        file = new File(file, fileName);
        return file;
    }

    private static void clearUselessImageFile(File file) {
        File files[] = file.listFiles();
        int length = files.length;
        if (length > 1) {
            File imageFile = files[length-1];
            imageFile.delete();
        }
    }

    public static File getAdImageFolder(String fileName) {
        File file = null;
        if (fileName.contains("en")) {
            file = FileUtil.getAdImageEnDir();
        } else if (fileName.contains("CN")) {
            file = FileUtil.getAdImagZhCnDir();
        } else {
            file = FileUtil.getAdImagZhTwDir();
        }
        return file;
    }
}
