package net.bither.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import net.bither.R;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogProgress;

import java.io.File;
import java.io.FileNotFoundException;

public class ShareUtil {

    static private DialogProgress progress;

    public static boolean shareBitmap(final Activity activity, final Bitmap bitmap) {
        if (bitmap == null) {
            DropdownMessage.showDropdownMessage(activity, R.string.market_share_failed);
            return false;
        }
        showProgress(activity);
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File shearImageFile = FileUtil.saveShareImageFile(bitmap);
            String imageUri = insertImageToSystem(activity, shearImageFile.getAbsolutePath());
            if (imageUri == null) {
                hideProgress();
                DropdownMessage.showDropdownMessage(activity, R.string.permissions_no_grant);
                return false;
            }
            uri = Uri.parse(imageUri);
        } else {
            uri = FileUtil.saveShareImage(bitmap);
        }
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType("image/jpg");
        try {
            intent = Intent.createChooser(intent, activity.getResources().getString(R.string.market_share_button));
            activity.startActivity(intent);
            hideProgress();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            DropdownMessage.showDropdownMessage(activity, R.string.market_share_failed);
        }
        hideProgress();
        return false;
    }

    public static String insertImageToSystem(Context context, String imagePath) {
        String url = null;
        if (PermissionUtil.isGrantExternalRW((Activity) context, 1000)) {
            try {
                url = MediaStore.Images.Media.insertImage(context.getContentResolver(), imagePath, "share.jpg", "bither_share");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    static private void showProgress(Activity activity) {
        if (progress != null) {
            return;
        }
        progress = new DialogProgress(activity, R.string.please_wait);
        progress.show();
    }

    static private void hideProgress() {
        if (progress != null) {
            progress.dismiss();
            progress = null;
        }
    }
}
