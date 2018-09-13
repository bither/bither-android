package net.bither.ui.base;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.SplitCoin;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.utils.UnitUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.util.WalletUtils;

/**
 * Created by ltq on 2017/7/28.
 */

public class ObtainBCCListItemView extends FrameLayout {

    private FragmentActivity activity;
    private TextView tvAddress;
    private TextView tvIndex;
    private TextView tvIndexTitle;
    private TextView tvBalance;
    private TextView tvBalanceTitle;

    private Address address;
    private boolean isDetectBcc;

    public ObtainBCCListItemView(FragmentActivity activity) {
        super(activity);
        this.activity = activity;
        View v = LayoutInflater.from(activity).inflate(R.layout.list_item_dialog_sign_message_select_address,
                null);
        addView(v, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        initView();
    }

    private void initView() {
        tvAddress = (TextView) findViewById(R.id.tv_address);
        tvIndex = (TextView) findViewById(R.id.tv_index);
        tvIndexTitle = (TextView)findViewById(R.id.tv_index_title);
        tvBalance = (TextView) findViewById(R.id.tv_balance);
        tvBalanceTitle = (TextView)findViewById(R.id.tv_balance_title);
        tvIndex.setVisibility(GONE);
        tvIndexTitle.setVisibility(GONE);
    }

    public void setAddress(Address address, SplitCoin splitCoin) {
        this.address = address;
        if (address != null) {
            showAddressInfo(splitCoin);
            tvBalanceTitle.setText(getResources().getString(R.string.get_split_coin));
        }
    }

    public void setObtainAddress(Address address, SplitCoin splitCoin) {
        this.address = address;
        if (address != null) {
            tvAddress.setText(WalletUtils.formatHash(address.getAddress(), 4, 20));
            tvBalanceTitle.setText(Utils.format(getResources().getString(R.string.you_already_get_split_coin), splitCoin.getName()));
        }
    }

    public void setDetectBcc(boolean detectBcc,Address address) {
        this.address = address;
        isDetectBcc = detectBcc;
        if (address != null) {
            tvAddress.setText(WalletUtils.formatHash(address.getAddress(), 4, 20));
            tvBalanceTitle.setVisibility(GONE);
        }
    }

    private void showAddressInfo(SplitCoin splitCoin) {
        if (address == null) {
            return;
        }
        tvAddress.setText(WalletUtils.formatHash(address.getAddress(), 4, 20));
        long amount = 0;
        if (address.isHDAccount() && !address.hasPrivKey()) {
            amount = AddressManager.getInstance().getAmount(AbstractDb.
                    hdAccountAddressProvider.getUnspentOutputByBlockNo(splitCoin.getForkBlockHeight(), AddressManager.getInstance()
                    .getHDAccountMonitored().getHdSeedId()));
        } else if (address.isHDAccount()) {
            amount = AddressManager.getInstance().getAmount(AbstractDb.
                    hdAccountAddressProvider.getUnspentOutputByBlockNo(splitCoin.getForkBlockHeight(), AddressManager.getInstance().
                    getHDAccountHot().getHdSeedId()));
        } else {
            amount = AddressManager.getInstance().getAmount(AbstractDb.
                    txProvider.getUnspentOutputByBlockNo(splitCoin.getForkBlockHeight(), address.getAddress()));
        }
        tvBalance.setText(UnitUtil.formatValue(amount, splitCoin.getBitcoinUnit()) + " " + splitCoin.getName());
    }

    public ObtainBCCListItemView(Context context) {
        super(context);
    }

    public ObtainBCCListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ObtainBCCListItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}
