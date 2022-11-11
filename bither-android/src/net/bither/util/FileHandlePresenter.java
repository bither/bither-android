package net.bither.util;

import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.support.v4.app.FragmentActivity;

import net.bither.R;
import net.bither.ui.base.dialog.DialogBackupSelectVersion;

public class FileHandlePresenter {

    public static final int REQUEST_CODE_READ_FILE_FROM_EXTERNAL = 1000;
    private FragmentActivity mactivity;

    public FileHandlePresenter(FragmentActivity activity) {
        mactivity = activity;
    }

    public void requestReadExternalStorage() {
        final boolean isExistBackup = FileUtil.isExistBackupSDCardDirOfCold(false);
        final boolean isExistOldBackup = FileUtil.isExistBackupSDCardDirOfCold(true);
        if (isExistBackup && isExistOldBackup) {
            DialogBackupSelectVersion dl = new DialogBackupSelectVersion(mactivity, new DialogBackupSelectVersion.Listener() {
                @Override
                public void onClicked(boolean isOld) {
                    chooseBackupFile(isOld);
                }
            });
            dl.show();
        } else {
            chooseBackupFile(isExistOldBackup);
        }
    }

    private void chooseBackupFile(boolean isOld) {
      String path = isOld ? "%2fBitherBackup%2f" : "%2fDocuments%2fBitherBackup%2f";
        Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:" + path);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        mactivity.startActivityForResult(Intent.createChooser(intent, mactivity.getString(R.string.recover_from_backup_title)), REQUEST_CODE_READ_FILE_FROM_EXTERNAL);
    }

}
