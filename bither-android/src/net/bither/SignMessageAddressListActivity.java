package net.bither;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.bither.bitherj.core.AbstractHD;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.enums.SignMessageTypeSelect;
import net.bither.ui.base.SmoothScrollListRunnable;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.DetectAnotherAssetsUtil;
import net.bither.util.UnitUtilWrapper;
import net.bither.util.WalletUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ltq on 2017/7/21.
 */

public class SignMessageAddressListActivity extends SwipeRightFragmentActivity {
    public static final String SignMgsTypeSelect = "SignMgsTypeSelect";
    public static final String PassWord = "PassWord";
    public static final String IsHdAccountHot = "IsHdAccountHot";
    public static final String IsDetectBcc = "IsDetectBcc";

    private int page = 1;
    private boolean hasMore = true;
    private boolean isLoading = false;

    private CharSequence password;
    private SignMessageTypeSelect signMessageTypeSelect;
    private AbstractHD.PathType pathType;
    private boolean isHot;
    private boolean isDetectBcc;
    private ListView lv;
    private FrameLayout flTitleBar;
    private TextView tvTitle;
    private ArrayList<Address> addresses = new ArrayList<Address>();
    private ArrayList<HDAccount.HDAccountAddress> hdAccountAddresses = new ArrayList<>();
    private HDAccount hdAccount;
    private HDAccountCold hdAccountCold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_sign_message_select_address);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        hdAccount = AddressManager.getInstance().getHDAccountHot();
        hdAccountCold = AddressManager.getInstance().getHDAccountCold();
        signMessageTypeSelect = (SignMessageTypeSelect) getIntent().getSerializableExtra(SignMgsTypeSelect);
        isHot = (boolean) getIntent().getSerializableExtra(IsHdAccountHot);
        isDetectBcc = (boolean) getIntent().getSerializableExtra(IsDetectBcc);
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
        flTitleBar.setOnClickListener(scrollToTopClick);
        lv.setAdapter(adapter);
        switch (signMessageTypeSelect) {
            case Hot:
                addresses.clear();
                final List<Address> all = AddressManager.getInstance().getPrivKeyAddresses();
                addresses.addAll(all);
                adapter.notifyDataSetChanged();
                break;
            case HdReceive:
                pathType = AbstractHD.PathType.EXTERNAL_ROOT_PATH;
                lv.setOnScrollListener(new AbsListView.OnScrollListener() {
                    private int lastFirstVisibleItem;

                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                        if (firstVisibleItem + visibleItemCount >= totalItemCount - 6
                                && hasMore && !isLoading
                                && lastFirstVisibleItem < firstVisibleItem) {
                            page++;
                            loadAddress();
                        }
                        lastFirstVisibleItem = firstVisibleItem;
                    }
                });
                loadData();
                break;
            case HdChange:
                pathType = AbstractHD.PathType.INTERNAL_ROOT_PATH;
                lv.setOnScrollListener(new AbsListView.OnScrollListener() {
                    private int lastFirstVisibleItem;

                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {

                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if (firstVisibleItem + visibleItemCount >= totalItemCount - 6
                                && hasMore && !isLoading
                                && lastFirstVisibleItem < firstVisibleItem) {
                            page++;
                            loadAddress();
                        }
                        lastFirstVisibleItem = firstVisibleItem;

                    }
                });
                loadData();
                break;
        }

    }

    private void loadData() {
        page = 1;
        hasMore = true;
        loadAddress();
    }

    private void loadAddress() {
        if (!isLoading && hasMore) {
            isLoading = true;
            List<HDAccount.HDAccountAddress> address;
            if (isHot) {
                address = hdAccount.getHdHotAddresses(page, pathType, password);
            } else {
                address = hdAccountCold.getHdColdAddresses(page, pathType, password);
            }
            if (page == 1) {
                hdAccountAddresses.clear();
            }
            if (address != null && address.size() > 0) {
                hdAccountAddresses.addAll(address);
                hasMore = true;
            } else {
                hasMore = false;
            }
            adapter.notifyDataSetChanged();
            isLoading = false;
        }
    }

    private BaseAdapter adapter = new BaseAdapter() {
        private LayoutInflater inflater;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (inflater == null) {
                inflater = LayoutInflater.from(SignMessageAddressListActivity.this);
            }
            ViewHolder h;
            if (convertView != null && convertView.getTag() != null && convertView.getTag()
                    instanceof ViewHolder) {
                h = (ViewHolder) convertView.getTag();
            } else {
                convertView = inflater.inflate(R.layout
                        .list_item_dialog_sign_message_select_address, null);
                h = new ViewHolder(convertView);
                convertView.setTag(h);
            }
            switch (signMessageTypeSelect) {
                case Hot:
                    Address a = (Address) getItem(position);
                    h.tvAddress.setText(WalletUtils.formatHash(a.getAddress(), 4, 20));
                    h.tvIndex.setText(String.valueOf(position));
                    h.tvBalance.setText(UnitUtilWrapper.formatValue(a.getBalance()));
                    convertView.setOnClickListener(new ListItemClick(a));
                    break;
                case HdReceive:
                    HDAccount.HDAccountAddress hdar = (HDAccount.HDAccountAddress) getItem(position);
                    h.tvAddress.setText(WalletUtils.formatHash(hdar.getAddress(), 4, 20));
                    h.tvIndex.setText(String.valueOf(hdar.getIndex()));
                    h.tvIndex.setText(String.valueOf(hdar.getIndex()));
                    if (isHot) {
                        h.tvBalance.setText(UnitUtilWrapper.formatValue(hdar.getBalance()));
                    } else {
                        h.llBalance.setVisibility(View.GONE);
                    }
                    convertView.setOnClickListener(new HdAddressListItemClick(hdar));
                    break;
                case HdChange:
                    HDAccount.HDAccountAddress hdac = (HDAccount.HDAccountAddress) getItem(position);
                    h.tvAddress.setText(WalletUtils.formatHash(hdac.getAddress(), 4, 20));
                    h.tvIndex.setText(String.valueOf(hdac.getIndex()));
                    if (isHot) {
                        h.tvBalance.setText(UnitUtilWrapper.formatValue(hdac.getBalance()));
                    } else {
                        h.llBalance.setVisibility(View.GONE);
                    }
                    convertView.setOnClickListener(new HdAddressListItemClick(hdac));
                    break;
            }
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            switch (signMessageTypeSelect) {
                case HdReceive:
                    return hdAccountAddresses.get(position);
                case HdChange:
                    return hdAccountAddresses.get(position);
                case Hot:
                    return addresses.get(position);

            }
            return addresses.get(position);
        }

        @Override
        public int getCount() {
            switch (signMessageTypeSelect) {
                case HdReceive:
                    return hdAccountAddresses.size();
                case HdChange:
                    return hdAccountAddresses.size();
                case Hot:
                    return addresses.size();
            }
            return 0;
        }

        class ViewHolder {
            TextView tvAddress;
            TextView tvIndex;
            TextView tvBalance;
            LinearLayout llBalance;

            public ViewHolder(View v) {
                tvAddress = (TextView) v.findViewById(R.id.tv_address);
                tvIndex = (TextView) v.findViewById(R.id.tv_index);
                tvBalance = (TextView) v.findViewById(R.id.tv_balance);
                llBalance = (LinearLayout) v.findViewById(R.id.ll_balance);
            }
        }
    };

    private View.OnClickListener scrollToTopClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (lv.getFirstVisiblePosition() != 0) {
                lv.post(new SmoothScrollListRunnable(lv, 0, null));
            }
        }
    };

    private class ListItemClick implements View.OnClickListener {
        private Address address;

        public ListItemClick(Address address) {
            this.address = address;
        }

        @Override
        public void onClick(View v) {
            if (!isDetectBcc) {
                Intent intent = new Intent(SignMessageAddressListActivity.this, SignMessageActivity.class);
                intent.putExtra(SignMessageActivity.AddressKey, address.getAddress());
                SignMessageAddressListActivity.this.startActivity(intent);
            } else {
                DetectAnotherAssetsUtil detectAnotherAssetsUtil = new DetectAnotherAssetsUtil(
                        SignMessageAddressListActivity.this);
                detectAnotherAssetsUtil.getBCCUnspentOutputs(address.getAddress());
            }
        }
    }

    private class HdAddressListItemClick implements View.OnClickListener {
        private HDAccount.HDAccountAddress hdAccountAddress;

        public HdAddressListItemClick(HDAccount.HDAccountAddress hdAccountAddress) {
            this.hdAccountAddress = hdAccountAddress;
        }

        @Override
        public void onClick(View v) {
            if (!isDetectBcc) {
                Intent intent = new Intent(SignMessageAddressListActivity.this, SignMessageActivity.class);
                intent.putExtra(SignMessageActivity.HdAccountPathType, hdAccountAddress.getPathType().getValue());
                intent.putExtra(SignMessageActivity.HdAddressIndex, hdAccountAddress.getIndex());
                intent.putExtra(IsHdAccountHot, isHot);
                SignMessageAddressListActivity.this.startActivity(intent);
            } else {
                DetectAnotherAssetsUtil detectAnotherAssetsUtil = new DetectAnotherAssetsUtil(
                        SignMessageAddressListActivity.this);
                detectAnotherAssetsUtil.getBCCHDUnspentOutputs(hdAccountAddress.getAddress(),
                        hdAccountAddress.getPathType(), hdAccountAddress.getIndex());

            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }
}
