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
    private TextView tvRecommendAddressTitle;
    private TextView tvNotRecommendAddressTitle;
    private TextView tvRecommendAddress;
    private TextView tvNotRecommendAddress;
    private boolean isCompressedKeyRecommend;

    public DialogImportPrivateKeyAddressValidation(Context context, String compressAddress, String uncompressedAddress, boolean isCompressedKeyRecommend, Runnable
            importRunnable, Runnable importUncompressedPrivateKeyRunnable) {
        super(context);
        this.isCompressedKeyRecommend = isCompressedKeyRecommend;
        this.importCompressPrivateKeyRunnable = importRunnable;
        this.importUncompressedPrivateKeyRunnable = importUncompressedPrivateKeyRunnable;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_import_private_key_address_validation);
        tvRecommendAddressTitle = (TextView) findViewById(R.id.tv_private_key_recommend_address);
        tvNotRecommendAddressTitle = (TextView) findViewById(R.id.tv_private_key_not_recommend_address);
        tvRecommendAddress = (TextView) findViewById(R.id.tv_recommend_address);
        tvNotRecommendAddress = (TextView) findViewById(R.id.tv_not_recommend_address);
        if (isCompressedKeyRecommend) {
            String recommendAddressTitle = context.getString(R.string.private_key_compressed_address) + context.getString(R.string.private_key_recommend);
            tvRecommendAddressTitle.setText(recommendAddressTitle);
            tvNotRecommendAddressTitle.setText(R.string.private_key_uncompressed_address);
            tvRecommendAddress.setText(WalletUtils.formatHash(compressAddress, 4, 16));
            tvNotRecommendAddress.setText(WalletUtils.formatHash(uncompressedAddress, 4, 16));
        } else {
            String recommendAddressTitle = context.getString(R.string.private_key_uncompressed_address) + context.getString(R.string.private_key_recommend);
            tvRecommendAddressTitle.setText(recommendAddressTitle);
            tvNotRecommendAddressTitle.setText(R.string.private_key_compressed_address);
            tvRecommendAddress.setText(WalletUtils.formatHash(uncompressedAddress, 4, 16));
            tvNotRecommendAddress.setText(WalletUtils.formatHash(compressAddress, 4, 16));
        }
        findViewById(R.id.btn_import_recommend).setOnClickListener(this);
        findViewById(R.id.btn_import_not_recommend).setOnClickListener(this);

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
        if (clickedId == R.id.btn_import_recommend && importCompressPrivateKeyRunnable != null) {
            if (isCompressedKeyRecommend) {
                if (importCompressPrivateKeyRunnable != null) {
                    importCompressPrivateKeyRunnable.run();
                }
            } else {
                if (importUncompressedPrivateKeyRunnable != null) {
                    importUncompressedPrivateKeyRunnable.run();
                }
            }
        } else if (clickedId == R.id.btn_import_not_recommend) {
            if (isCompressedKeyRecommend) {
                if (importUncompressedPrivateKeyRunnable != null) {
                    importUncompressedPrivateKeyRunnable.run();
                }
            } else {
                if (importCompressPrivateKeyRunnable != null) {
                    importCompressPrivateKeyRunnable.run();
                }
            }
        }
    }

}
