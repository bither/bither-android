package net.bither.ui.base.dialog;

import android.content.Context;
import android.view.View;

import net.bither.R;

public class DialogBitpieColdInfo extends CenterDialog implements View.OnClickListener {

    public DialogBitpieColdInfo(Context context) {
        super(context);
        setContentView(R.layout.dialog_bitpie_cold_info);
        findViewById(R.id.btn_ok).setOnClickListener(this);
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_ok) {
            dismiss();
        }
    }

}
