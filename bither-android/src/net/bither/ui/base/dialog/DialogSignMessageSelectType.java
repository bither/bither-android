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
import net.bither.bitherj.core.BitpieHDAccountCold;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.enums.SignMessageTypeSelect;
import net.bither.ui.base.listener.IDialogPasswordListener;

import static net.bither.SignMessageAddressListActivity.IsHdAccountHot;
import static net.bither.SignMessageAddressListActivity.IsSignHash;
import static net.bither.SignMessageAddressListActivity.PassWord;
import static net.bither.SignMessageAddressListActivity.SignMgsTypeSelect;

/**
 * Created by ltq on 2017/7/21.
 */

public class DialogSignMessageSelectType extends CenterDialog {

    private TextView tvTitle;
    private LinearLayout llHd;
    private LinearLayout llHdReceive;
    private LinearLayout llHdChange;
    private LinearLayout llBitpieCold;
    private LinearLayout llBitpieColdReceive;
    private LinearLayout llBitpieColdChange;
    private View vHotLine;
    private LinearLayout llHot;
    private TextView tvGroupHot;
    private HDAccount hdAccount;
    private HDAccountCold hdAccountCold;
    private BitpieHDAccountCold bitpieHDAccountCold;

    public DialogSignMessageSelectType(Context context, final boolean isHot, final boolean isSignHash) {
        super(context);
        setContentView(R.layout.dialog_sign_message_select_type);
        tvTitle = findViewById(R.id.tv_title);
        llHd = findViewById(R.id.ll_hd);
        llHdReceive = findViewById(R.id.ll_hd_receive);
        llHdChange = findViewById(R.id.ll_hd_change);
        llBitpieCold = findViewById(R.id.ll_bitpie_cold);
        llBitpieColdReceive = findViewById(R.id.ll_bitpie_cold_receive);
        llBitpieColdChange = findViewById(R.id.ll_bitpie_cold_change);
        vHotLine = findViewById(R.id.v_hot_line);
        llHot = findViewById(R.id.ll_hot);
        tvGroupHot = findViewById(R.id.tv_group_hot);

        hdAccount = AddressManager.getInstance().getHDAccountHot();
        hdAccountCold = AddressManager.getInstance().getHDAccountCold();
        bitpieHDAccountCold = AddressManager.getInstance().getBitpieHDAccountCold();

        tvTitle.setText(isSignHash ? R.string.sign_hash_select_address : R.string.sign_message_select_address);

        if (isHot) {
            if (hdAccount == null) {
                llHd.setVisibility(View.GONE);
            }
        } else {
            if (hdAccountCold == null) {
                llHd.setVisibility(View.GONE);
            }
            if (bitpieHDAccountCold != null) {
                llBitpieCold.setVisibility(View.VISIBLE);
            }
        }

        if (AddressManager.getInstance().getPrivKeyAddresses().size() <= 0) {
            vHotLine.setVisibility(View.GONE);
            llHot.setVisibility(View.GONE);
        } else {
            if (isHot) {
                tvGroupHot.setText(R.string.address_group_private);
            } else {
                tvGroupHot.setText(R.string.address_cold);
            }
        }

        llHdReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogPassword(getContext(), new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(final SecureCharSequence password) {
                        Intent intent = new Intent(getContext(), SignMessageAddressListActivity.class);
                        intent.putExtra(SignMgsTypeSelect, SignMessageTypeSelect.HdReceive);
                        intent.putExtra(IsHdAccountHot, isHot);
                        intent.putExtra(IsSignHash, isSignHash);
                        intent.putExtra(PassWord, password);
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
                        intent.putExtra(IsHdAccountHot, isHot);
                        intent.putExtra(IsSignHash, isSignHash);
                        intent.putExtra(PassWord, password);
                        getContext().startActivity(intent);
                    }
                }).show();
                dismiss();
            }
        });

        llBitpieColdReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogPassword(getContext(), new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(final SecureCharSequence password) {
                        Intent intent = new Intent(getContext(), SignMessageAddressListActivity.class);
                        intent.putExtra(SignMgsTypeSelect, SignMessageTypeSelect.BitpieColdReceive);
                        intent.putExtra(IsHdAccountHot, isHot);
                        intent.putExtra(IsSignHash, isSignHash);
                        intent.putExtra(PassWord, password);
                        getContext().startActivity(intent);
                    }
                }).show();
                dismiss();
            }
        });

        llBitpieColdChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogPassword(getContext(), new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(final SecureCharSequence password) {
                        Intent intent = new Intent(getContext(), SignMessageAddressListActivity.class);
                        intent.putExtra(SignMgsTypeSelect, SignMessageTypeSelect.BitpieColdChange);
                        intent.putExtra(IsHdAccountHot, isHot);
                        intent.putExtra(IsSignHash, isSignHash);
                        intent.putExtra(PassWord, password);
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
                intent.putExtra(IsSignHash, isSignHash);
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
