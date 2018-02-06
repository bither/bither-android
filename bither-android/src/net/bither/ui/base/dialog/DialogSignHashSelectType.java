package net.bither.ui.base.dialog;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.SignMessageAddressListActivity;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.enums.SignMessageTypeSelect;
import net.bither.ui.base.listener.IDialogPasswordListener;

import static net.bither.SignMessageAddressListActivity.IsHdAccountHot;
import static net.bither.SignMessageAddressListActivity.IsSignHash;
import static net.bither.SignMessageAddressListActivity.PassWord;
import static net.bither.SignMessageAddressListActivity.SignMgsTypeSelect;

public class DialogSignHashSelectType extends CenterDialog {

    private LinearLayout llHdReceive;
    private LinearLayout llHdChange;
    private LinearLayout llHot;
    private View vLineReceive;
    private View vLineChange;
    private HDAccount hdAccount;
    private HDAccountCold hdAccountCold;
    private TextView tvGroupHot;

    public DialogSignHashSelectType(Context context, final boolean isHot) {
        super(context);
        setContentView(R.layout.dialog_sign_hash_select_type);
        llHdReceive = (LinearLayout) findViewById(R.id.ll_hd_receive);
        llHdChange = (LinearLayout) findViewById(R.id.ll_hd_change);
        llHot = (LinearLayout) findViewById(R.id.ll_hot);
        vLineReceive = findViewById(R.id.v_line_receive);
        vLineChange = findViewById(R.id.v_line_change);
        tvGroupHot = (TextView) findViewById(R.id.tv_group_hot);
        hdAccount = AddressManager.getInstance().getHDAccountHot();
        hdAccountCold = AddressManager.getInstance().getHDAccountCold();

        if (isHot) {
            if (hdAccount == null) {
                llHdReceive.setVisibility(View.GONE);
                vLineReceive.setVisibility(View.GONE);
                llHdChange.setVisibility(View.GONE);
                vLineChange.setVisibility(View.GONE);
            }
        } else {
            if (hdAccountCold == null) {
                llHdReceive.setVisibility(View.GONE);
                vLineReceive.setVisibility(View.GONE);
                llHdChange.setVisibility(View.GONE);
                vLineChange.setVisibility(View.GONE);
            }
        }

        if (AddressManager.getInstance().getPrivKeyAddresses().size() <= 0) {
            llHot.setVisibility(View.GONE);
        }

        if (isHot) {
            tvGroupHot.setText(R.string.address_group_private);
        } else {
            tvGroupHot.setText(R.string.address_cold);
        }

        llHdReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogPassword(getContext(), new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(final SecureCharSequence password) {
                        Intent intent = new Intent(getContext(), SignMessageAddressListActivity.class);
                        intent.putExtra(SignMgsTypeSelect, SignMessageTypeSelect.HdReceive);
                        intent.putExtra(PassWord, password.toString());
                        intent.putExtra(IsHdAccountHot, isHot);
                        intent.putExtra(IsSignHash, true);
                        getContext().startActivity(intent);
                    }
                }).show();
                dismiss();
            }
        });

        llHdChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogPassword(getContext(), new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(final SecureCharSequence password) {
                        Intent intent = new Intent(getContext(), SignMessageAddressListActivity.class);
                        intent.putExtra(SignMgsTypeSelect, SignMessageTypeSelect.HdChange);
                        intent.putExtra(PassWord, password);
                        intent.putExtra(IsHdAccountHot, isHot);
                        intent.putExtra(IsSignHash, true);
                        getContext().startActivity(intent);
                    }
                }).show();
                dismiss();
            }
        });

        llHot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), SignMessageAddressListActivity.class);
                intent.putExtra(SignMgsTypeSelect, SignMessageTypeSelect.Hot);
                intent.putExtra(IsHdAccountHot, isHot);
                intent.putExtra(IsSignHash, true);
                getContext().startActivity(intent);
                dismiss();
            }
        });
    }

    @Override
    public void show() {
        super.show();
    }
}
