package net.bither.ui.base.dialog;

import static net.bither.bitherj.crypto.mnemonic.MnemonicWordList.English;
import static net.bither.bitherj.crypto.mnemonic.MnemonicWordList.ZhCN;
import static net.bither.bitherj.crypto.mnemonic.MnemonicWordList.ZhTw;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.crypto.mnemonic.MnemonicWordList;

public class DialogImportHdAccountSeedQrCodeSelectLanguage extends CenterDialog {

    public interface Listener {
        void onClicked(MnemonicWordList mnemonicWordList);
        void cancel();
    }

    private TextView tvEn, tvZhCn, tvZhTw, tvCancel;
    private Listener listener;

    public DialogImportHdAccountSeedQrCodeSelectLanguage(Context context, final Listener listener) {
        super(context);
        this.listener = listener;
        setContentView(R.layout.dialog_import_hd_account_seed_qr_code_select_language);
        tvEn = (TextView) findViewById(R.id.tv_en);
        tvZhCn = (TextView) findViewById(R.id.tv_zh_cn);
        tvZhTw = (TextView) findViewById(R.id.tv_zh_tw);
        tvCancel = (TextView) findViewById(R.id.tv_cancel);
        tvEn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClicked(English);
                dismiss();
            }
        });
        tvZhCn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClicked(ZhCN);
                dismiss();
            }
        });
        tvZhTw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClicked(ZhTw);
                dismiss();
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.cancel();
                dismiss();
            }
        });
    }

}
