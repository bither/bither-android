package net.bither.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.Fragment;

public class PermissionUtil {

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String PERMISSION_READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String PERMISSION_READ_MEDIA_IMAGES = Manifest.permission.READ_MEDIA_IMAGES;

    public static boolean isCameraPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(PERMISSION_CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{PERMISSION_CAMERA}, requestCode);
                return false;
            }
        }
        return true;
    }

    public static boolean isCameraPermission(Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (fragment.getContext() == null) {
                return false;
            }
            if (fragment.getContext().checkSelfPermission(PERMISSION_CAMERA) != PackageManager.PERMISSION_GRANTED) {
                fragment.requestPermissions(new String[]{PERMISSION_CAMERA}, requestCode);
                return false;
            }
        }
        return true;
    }

    public static boolean isReadPermission(Fragment fragment, int requestCode) {
        if (fragment.getContext() == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (fragment.getContext().checkSelfPermission(PERMISSION_READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                fragment.requestPermissions(new String[]{PERMISSION_READ_MEDIA_IMAGES}, requestCode);
                return false;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (fragment.getContext().checkSelfPermission(PERMISSION_READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                fragment.requestPermissions(new String[]{PERMISSION_READ_EXTERNAL_STORAGE}, requestCode);
                return false;
            }
        }
        return true;
    }

    public static boolean isWriteExternalStoragePermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(PERMISSION_WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{PERMISSION_WRITE_EXTERNAL_STORAGE}, requestCode);
                return false;
            }
        }
        return true;
    }

    public static boolean isWriteAndReadExternalStoragePermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(PERMISSION_WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(PERMISSION_READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{PERMISSION_WRITE_EXTERNAL_STORAGE, PERMISSION_READ_EXTERNAL_STORAGE}, requestCode);
                return false;
            }
        }
        return true;
    }

    public static boolean isGrantExternalRW(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(PERMISSION_WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || activity.checkSelfPermission(PERMISSION_READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || activity.checkSelfPermission(PERMISSION_CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{PERMISSION_WRITE_EXTERNAL_STORAGE, PERMISSION_READ_EXTERNAL_STORAGE, PERMISSION_CAMERA}, requestCode);
                return false;
            }
        }
        return true;
    }

}
