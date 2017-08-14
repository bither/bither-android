package net.bither.ui.base.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import net.bither.R;
import net.bither.util.WalletUtils;

/**
 * Created by Hzz on 2017/8/14.
 */

public class DialogImportPrivateKeyAddressValidation extends CenterDialog implements DialogInterface
        .OnDismissListener, View.OnClickListener {

    private Runnable importCompressPrivateKeyRunnable;
    private Runnable importUncompressedPrivateKeyRunnable;
    private int clickedId;
    private TextView tvCompressAddress;
    private TextView tvUncompressedAddress;

    public DialogImportPrivateKeyAddressValidation(Context context, String compressAddress, String uncompressedAddress, Runnable
            importRunnable, Runnable importUncompressedPrivateKeyRunnable) {
        super(context);
        this.importCompressPrivateKeyRunnable = importRunnable;
        this.importUncompressedPrivateKeyRunnable = importUncompressedPrivateKeyRunnable;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_import_private_key_address_validation);
        tvCompressAddress = (TextView) findViewById(R.id.tv_compress_address);
        tvUncompressedAddress = (TextView) findViewById(R.id.tv_uncompressed_address);
        tvCompressAddress.setText(WalletUtils.formatHash(compressAddress, 4, 16));
        tvUncompressedAddress.setText(WalletUtils.formatHash(uncompressedAddress, 4, 16));
        findViewById(R.id.btn_import_compress).setOnClickListener(this);
        findViewById(R.id.btn_import_uncompressed).setOnClickListener(this);
    }

    @Override
    public void show() {
        super.show();
        clickedId = 0;
    }

    @Override
    public void onClick(View v) {
        clickedId = v.getId();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (clickedId == R.id.btn_import_compress && importCompressPrivateKeyRunnable != null) {
            importCompressPrivateKeyRunnable.run();
        } else if (clickedId == R.id.btn_import_uncompressed && importUncompressedPrivateKeyRunnable != null) {
            importUncompressedPrivateKeyRunnable.run();
        }
    }

}
