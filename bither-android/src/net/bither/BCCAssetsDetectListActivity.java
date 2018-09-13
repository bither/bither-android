package net.bither;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.bither.bitherj.core.AbstractHD;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.enums.SignMessageTypeSelect;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.DetectAnotherAssetsUtil;
import net.bither.util.UnitUtilWrapper;
import net.bither.util.WalletUtils;

import java.util.ArrayList;

/**
 * Created by ltq on 2017/9/20.
 */

public class BCCAssetsDetectListActivity extends SwipeRightFragmentActivity {
    public static final String SignMgsTypeSelect = "SignMgsTypeSelect";
    public static final String PassWord = "PassWord";
    public static final String IsHdAccountHot = "IsHdAccountHot";
    public static final String IsDetectBcc = "IsDetectBcc";
    public static final String IsMonitored = "IsMonitored";

    private CharSequence password;
    private SignMessageTypeSelect signMessageTypeSelect;
    private AbstractHD.PathType pathType;
    private boolean isHot;
    private boolean isDetectBcc;
    private boolean isMonitored;
    private ListView lv;
    private FrameLayout flTitleBar;
    private TextView tvTitle;
    private ArrayList<HDAccount.HDAccountAddress> hdAccountAddresses = new ArrayList<>();
    private HDAccount hdAccount;
    private HDAccountCold hdAccountCold;
    private HDAccount hdAccountMonitored;
    private static final int MAX_CACHE_SIZE = 500;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_sign_message_select_address);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        hdAccount = AddressManager.getInstance().getHDAccountHot();
        hdAccountMonitored = AddressManager.getInstance().getHDAccountMonitored();
        signMessageTypeSelect = (SignMessageTypeSelect) getIntent().getSerializableExtra(SignMgsTypeSelect);
        isHot = (boolean) getIntent().getSerializableExtra(IsHdAccountHot);
        isDetectBcc = (boolean) getIntent().getSerializableExtra(IsDetectBcc);
        isMonitored = (boolean) getIntent().getSerializableExtra(IsMonitored);
        if (!isDetectBcc) {
            tvTitle.setText(R.string.sign_message_select_address);
        } else {
            tvTitle.setText(R.string.detect_another_BCC_assets_select_address);
        }
        if (signMessageTypeSelect != SignMessageTypeSelect.Hot) {
            String tempString = getIntent().getStringExtra(PassWord);
            password = tempString.subSequence(0, tempString.length());
        }
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(
                new IBackClickListener(0, R.anim.slide_out_right));
        lv = (ListView) findViewById(R.id.lv);
        flTitleBar = (FrameLayout) findViewById(R.id.fl_title_bar);
        switch (signMessageTypeSelect) {
            case HdReceive:
                pathType = AbstractHD.PathType.EXTERNAL_ROOT_PATH;
                break;
            case HdChange:
                pathType = AbstractHD.PathType.INTERNAL_ROOT_PATH;
                break;
        }
        lv.setAdapter(adapter);
    }

    private BaseAdapter adapter = new BaseAdapter() {

        @Override
        public int getCount() {
            if (pathType == AbstractHD.PathType.EXTERNAL_ROOT_PATH || pathType == AbstractHD.PathType.EXTERNAL_BIP49_PATH) {
                return issuedExternalAddressCount(pathType);
            } else {
                return issuedInternalAddressCount(pathType);
            }
        }


        @Override
        public Object getItem(int position) {
            HDAccount.HDAccountAddress a = addressForIndex(position);
                if (hdAccountAddresses.size() >= MAX_CACHE_SIZE) {
                    hdAccountAddresses.clear();
                }
                hdAccountAddresses.add(a);
            return a;
        }


        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Item view;
            if (convertView == null || !(convertView instanceof Item)) {
                convertView = new Item(BCCAssetsDetectListActivity.this);
            }
            view = (Item) convertView;
            view.setAddress((HDAccount.HDAccountAddress) getItem(position));
            return convertView;
        }

    };

    private int issuedExternalAddressCount(AbstractHD.PathType pathType) {
        if (isMonitored) {
            return hdAccountMonitored.issuedExternalIndex(pathType) + 1;
        } else {
            return hdAccount.issuedExternalIndex(pathType) + 1;
        }
    }

    private int issuedInternalAddressCount(AbstractHD.PathType pathType) {
        if (isMonitored) {
            return hdAccountMonitored.issuedInternalIndex(pathType) + 1;
        } else {
            return hdAccount.issuedInternalIndex(pathType) + 1;
        }
    }

    private HDAccount.HDAccountAddress addressForIndex(int index) {
        if (isMonitored) {
            return hdAccountMonitored.addressForPath(pathType, index);
        } else {
            return hdAccount.addressForPath(pathType, index);
        }
    }

    private void showMsg(int msg) {
        DropdownMessage.showDropdownMessage(BCCAssetsDetectListActivity.this, msg);
    }

    private class Item extends FrameLayout {
        TextView tvAddress;
        TextView tvIndex;
        TextView tvBalance;
        LinearLayout llBalance;

        private HDAccount.HDAccountAddress address;

        public Item(Context context) {
            super(context);
            removeAllViews();
            addView(LayoutInflater.from(context).inflate(R.layout
                    .list_item_dialog_sign_message_select_address, null), new LayoutParams(LayoutParams
                    .MATCH_PARENT, LayoutParams
                    .MATCH_PARENT));
            tvAddress = (TextView) findViewById(R.id.tv_address);
            tvIndex = (TextView) findViewById(R.id.tv_index);
            tvBalance = (TextView) findViewById(R.id.tv_balance);
            llBalance = (LinearLayout) findViewById(R.id.ll_balance);
        }

        public void setAddress(HDAccount.HDAccountAddress address) {
            this.address = address;
            tvAddress.setText(WalletUtils.formatHash(address.getAddress(), 4, 20));
            tvIndex.setText(String.valueOf(address.getIndex()));
            tvBalance.setText(UnitUtilWrapper.formatValue(address.getBalance()));
           setOnClickListener(new BCCAssetsDetectListActivity.HdAddressListItemClick(address));
        }
    }

    private class HdAddressListItemClick implements View.OnClickListener {
        private HDAccount.HDAccountAddress hdAccountAddress;

        public HdAddressListItemClick(HDAccount.HDAccountAddress hdAccountAddress) {
            this.hdAccountAddress = hdAccountAddress;
        }

        @Override
        public void onClick(View v) {
            DetectAnotherAssetsUtil detectAnotherAssetsUtil = new DetectAnotherAssetsUtil(
                    BCCAssetsDetectListActivity.this);
            detectAnotherAssetsUtil.getBCCHDUnspentOutputs(hdAccountAddress.getAddress(),
                    hdAccountAddress.getPathType(), hdAccountAddress.getIndex(),isMonitored);

        }
    }

}
