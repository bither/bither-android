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
import android.os.Environment;
import android.provider.MediaStore;

import com.pi.common.util.NativeUtil;

import net.bither.BitherApplication;

import java.io.File;
import java.io.IOException;

public class ImageFileUtil {

    private static final String FILE_TYPE_KEY = ".jpg";
    private static final String DCIM_FILE_NAME = "IMG_%s";
    private static final String AVATAR_FILE_NAME = "a%d.jpg";

    public static File getImageForGallery(long timeMillis) {
        String pictureName = getImageNameForGallery(
                timeMillis);
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


    public static final String saveImageToDcim(Bitmap bit, int orientation,
                                               long timeMillis) {
        String pictureName = getImageNameForGallery(
                timeMillis);
        File file = getImageForGallery(timeMillis);
        try {

            NativeUtil.compressBitmap(bit, 100, file.getAbsolutePath(), true);
            saveExifInterface(file, orientation);
            addPicutureToResolver(file, pictureName, orientation, timeMillis);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file.getAbsolutePath();

    }

    private static void addPicutureToResolver(File file, String pictureName,
                                              int orientation, long timeInMillis) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.TITLE, pictureName);
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, pictureName);
        v.put(MediaStore.Images.ImageColumns.DESCRIPTION, "Taken with Picamera.");
        v.put(MediaStore.MediaColumns.DATE_ADDED, timeInMillis);
        v.put(MediaStore.Images.ImageColumns.DATE_TAKEN, timeInMillis);
        v.put(MediaStore.MediaColumns.DATE_MODIFIED, timeInMillis);
        v.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation);
        v.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());

        File parent = file.getParentFile();
        String path = parent.toString().toLowerCase();
        String name = parent.getName().toLowerCase();
        v.put(MediaStore.Images.ImageColumns.BUCKET_ID, path.hashCode());
        v.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
        v.put(MediaStore.MediaColumns.SIZE, file.length());
        ContentResolver c = BitherApplication.mContext.getContentResolver();
        if (c != null) {
            c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
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
        return StringUtil.format(AVATAR_FILE_NAME, time);
    }

}
