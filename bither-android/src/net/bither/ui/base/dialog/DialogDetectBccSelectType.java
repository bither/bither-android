package net.bither.ui.base.dialog;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BCCAssetsDetectListActivity;
import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.enums.SignMessageTypeSelect;
import net.bither.ui.base.listener.IDialogPasswordListener;

import static net.bither.BCCAssetsDetectListActivity.IsDetectBcc;
import static net.bither.BCCAssetsDetectListActivity.IsHdAccountHot;
import static net.bither.BCCAssetsDetectListActivity.IsMonitored;
import static net.bither.BCCAssetsDetectListActivity.PassWord;
import static net.bither.BCCAssetsDetectListActivity.SignMgsTypeSelect;
/**
 * Created by ltq on 2017/9/20.
 */

public class DialogDetectBccSelectType extends CenterDialog {

    private LinearLayout llHdReceive;
    private LinearLayout llHdChange;
    private LinearLayout llHot;
    private TextView tvDetectBcc;
    private View vLineReceive;
    private View vLineChange;
    private HDAccount hdAccount;
    private HDAccount hdAccountMonitored;

    public DialogDetectBccSelectType(Context context, final boolean isHotHD, final boolean isDetectBcc,final boolean isMonitored) {
        super(context);
        setContentView(R.layout.dialog_sign_message_select_type);
        llHdReceive = (LinearLayout) findViewById(R.id.ll_hd_receive);
        llHdChange = (LinearLayout) findViewById(R.id.ll_hd_change);
        llHot = (LinearLayout) findViewById(R.id.ll_hot);
        tvDetectBcc = (TextView)findViewById(R.id.tv_title);
        vLineReceive = (View) findViewById(R.id.v_line_receive);
        vLineChange = (View) findViewById(R.id.v_line_change);
        hdAccount = AddressManager.getInstance().getHDAccountHot();
        hdAccountMonitored = AddressManager.getInstance().getHDAccountMonitored();
        llHot.setVisibility(View.GONE);
        tvDetectBcc.setText(getContext().getString(R.string.detect_another_BCC_assets_select_address));

        if (isHotHD) {
            if (hdAccount == null) {
                llHdReceive.setVisibility(View.GONE);
                vLineReceive.setVisibility(View.GONE);
                llHdChange.setVisibility(View.GONE);
                vLineChange.setVisibility(View.GONE);
            }
        } else {
            if (hdAccountMonitored == null) {
                llHdReceive.setVisibility(View.GONE);
                vLineReceive.setVisibility(View.GONE);
                llHdChange.setVisibility(View.GONE);
                vLineChange.setVisibility(View.GONE);
            }
        }

        if (AddressManager.getInstance().getPrivKeyAddresses().size() <= 0) {
            llHot.setVisibility(View.GONE);
        }

        llHdReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMonitored) {
                    Intent intent = new Intent(getContext(), BCCAssetsDetectListActivity.class);
                    intent.putExtra(SignMgsTypeSelect, SignMessageTypeSelect.HdReceive);
                    intent.putExtra(PassWord, "");
                    intent.putExtra(IsHdAccountHot, isHotHD);
                    intent.putExtra(IsDetectBcc, isDetectBcc);
                    intent.putExtra(IsMonitored, isMonitored);
                    getContext().startActivity(intent);
            } else {
                    new DialogPassword(getContext(), new IDialogPasswordListener() {
                        @Override
                        public void onPasswordEntered(final SecureCharSequence password) {

                            Intent intent = new Intent(getContext(), BCCAssetsDetectListActivity.class);
                            intent.putExtra(SignMgsTypeSelect, SignMessageTypeSelect.HdReceive);
                            intent.putExtra(PassWord, password.toString());
                            intent.putExtra(IsHdAccountHot, isHotHD);
                            intent.putExtra(IsDetectBcc, isDetectBcc);
                            intent.putExtra(IsMonitored, isMonitored);
                            getContext().startActivity(intent);
                        }
                    }).show();
                }
                dismiss();
            }
        });

        llHdChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMonitored) {
                    Intent intent = new Intent(getContext(), BCCAssetsDetectListActivity.class);
                    intent.putExtra(SignMgsTypeSelect, SignMessageTypeSelect.HdChange);
                    intent.putExtra(PassWord, "");
                    intent.putExtra(IsHdAccountHot, isHotHD);
                    intent.putExtra(IsDetectBcc, isDetectBcc);
                    intent.putExtra(IsMonitored, isMonitored);
                    getContext().startActivity(intent);
                } else {
                    new DialogPassword(getContext(), new IDialogPasswordListener() {
                        @Override
                        public void onPasswordEntered(final SecureCharSequence password) {
                            Intent intent = new Intent(getContext(), BCCAssetsDetectListActivity.class);
                            intent.putExtra(SignMgsTypeSelect, SignMessageTypeSelect.HdChange);
                            intent.putExtra(PassWord, password);
                            intent.putExtra(IsHdAccountHot, isHotHD);
                            intent.putExtra(IsDetectBcc,isDetectBcc);
                            intent.putExtra(IsMonitored,isMonitored);
                            getContext().startActivity(intent);
                        }
                    }).show();
                }
                dismiss();
            }
        });
    }

    @Override
    public void show() {
        super.show();
    }
}
