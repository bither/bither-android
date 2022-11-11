package net.bither.ui.base.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import net.bither.R;

public class DialogBackupSelectVersion extends CenterDialog {

    public interface Listener {
        void onClicked(boolean isOld);
    }

    private TextView tvBackup;
    private TextView tvOldBackup;
    private TextView tvCancel;

    public DialogBackupSelectVersion(Context context, final Listener listener) {
        super(context);
        setContentView(R.layout.dialog_backup_select_version);
        setCancelable(false);
        tvBackup = findViewById(R.id.tv_backup);
        tvBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClicked(false);
                dismiss();
            }
        });
        tvOldBackup = findViewById(R.id.tv_old_backup);
        tvOldBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClicked(true);
                dismiss();
            }
        });
        tvCancel = findViewById(R.id.tv_cancel);
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    @Override
    public void show() {
        super.show();
    }
}
