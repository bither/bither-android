package net.bither.activity.hot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.PeerManager;
import net.bither.bitherj.core.SplitCoin;
import net.bither.bitherj.core.Version;
import net.bither.bitherj.utils.Utils;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SettingSelectorView;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.HDMKeychainRecoveryUtil;

/**
 * Created by denmark on 2018/1/19.
 */

public class SplitForkCoinsActivity extends SwipeRightFragmentActivity {
    private LinearLayout btnSplitBcc, btnSplitBtg, btnSplitSbtc, btnSplitBtw,
            btnSplitBcd, btnSplitBtf, btnSplitBtp, btnSplitBtn;

    private TextView tvSplitBcc,tvSplitBtg,tvSplitSbtc,tvSplitBtw,tvSplitBcd,
            tvSplitBtf,tvSplitBtp,tvSplitBtn;

    public static final String SplitCoinKey = "SplitCoin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_split_fork_coins);
        initView();
    }

    private void initView() {

        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());

        btnSplitBcc = (LinearLayout) findViewById(R.id.ll_split_bcc);
        btnSplitBtg = (LinearLayout) findViewById(R.id.ll_split_btg);
        btnSplitSbtc = (LinearLayout) findViewById(R.id.ll_split_sbtc);
        btnSplitBtw = (LinearLayout) findViewById(R.id.ll_split_btw);
        btnSplitBcd = (LinearLayout) findViewById(R.id.ll_split_bcd);
        btnSplitBtf = (LinearLayout) findViewById(R.id.ll_split_btf);
        btnSplitBtp = (LinearLayout) findViewById(R.id.ll_split_btp);
        btnSplitBtn = (LinearLayout) findViewById(R.id.ll_split_btn);
        btnSplitBcc.setOnClickListener(splitClick);
        btnSplitBtg.setOnClickListener(splitClick);
        btnSplitSbtc.setOnClickListener(splitClick);
        btnSplitBtw.setOnClickListener(splitClick);
        btnSplitBcd.setOnClickListener(splitClick);
        btnSplitBtf.setOnClickListener(splitClick);
        btnSplitBtp.setOnClickListener(splitClick);
        btnSplitBtn.setOnClickListener(splitClick);
        tvSplitBcc = (TextView) findViewById(R.id.tv_split_bcc);
        tvSplitBtg = (TextView) findViewById(R.id.tv_split_btg);
        tvSplitSbtc = (TextView) findViewById(R.id.tv_split_sbtc);
        tvSplitBtw = (TextView) findViewById(R.id.tv_split_btw);
        tvSplitBcd = (TextView) findViewById(R.id.tv_split_bcd);
        tvSplitBtf = (TextView) findViewById(R.id.tv_split_btf);
        tvSplitBtp = (TextView) findViewById(R.id.tv_split_btp);
        tvSplitBtn = (TextView) findViewById(R.id.tv_split_btn);
        tvSplitBcc.setText(Utils.format(getString(R.string.get_split_coin_setting_name), SplitCoin.BCC.getName()));
        tvSplitBtg.setText(Utils.format(getString(R.string.get_split_coin_setting_name), SplitCoin.BTG.getName()));
        tvSplitSbtc.setText(Utils.format(getString(R.string.get_split_coin_setting_name), SplitCoin.SBTC.getName()));
        tvSplitBtw.setText(Utils.format(getString(R.string.get_split_coin_setting_name), SplitCoin.BTW.getName()));
        tvSplitBcd.setText(Utils.format(getString(R.string.get_split_coin_setting_name), SplitCoin.BCD.getName()));
        tvSplitBtf.setText(Utils.format(getString(R.string.get_split_coin_setting_name), SplitCoin.BTF.getName()));
        tvSplitBtp.setText(Utils.format(getString(R.string.get_split_coin_setting_name), SplitCoin.BTP.getName()));
        tvSplitBtn.setText(Utils.format(getString(R.string.get_split_coin_setting_name), SplitCoin.BTN.getName()));
    }

    private View.OnClickListener splitClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SplitCoin coin;
            switch (view.getId()) {
                case R.id.ll_split_btn:
                    coin = SplitCoin.BTN;
                    break;
                case R.id.ll_split_btf:
                    coin = SplitCoin.BTF;
                    break;
                case R.id.ll_split_btp:
                    coin = SplitCoin.BTP;
                    break;
                case R.id.ll_split_btg:
                    coin = SplitCoin.BTG;
                    break;
                case R.id.ll_split_sbtc:
                    coin = SplitCoin.SBTC;
                    break;
                case R.id.ll_split_btw:
                    coin = SplitCoin.BTW;
                    break;
                case R.id.ll_split_bcd:
                    coin = SplitCoin.BCD;
                    break;
                default:
                    coin = SplitCoin.BCC;
                    break;

            }

            long lastBlockHeight = PeerManager.instance().getLastBlockHeight();
            long forkBlockHeight = coin.getForkBlockHeight();
            if (lastBlockHeight < forkBlockHeight) {
                DropdownMessage.showDropdownMessage(SplitForkCoinsActivity.this, String.format(getString
                        (R.string.please_firstly_sync_to_block_no), forkBlockHeight));
            } else {
                AddressManager addressManager = AddressManager.getInstance();
                if (!addressManager.hasHDAccountHot() && !addressManager.hasHDAccountMonitored() &&
                        addressManager.getPrivKeyAddresses().size() == 0 && addressManager.getWatchOnlyAddresses().size()
                        == 0) {
                    DropdownMessage.showDropdownMessage(SplitForkCoinsActivity.this, getString(R.string.no_private_key));
                } else {
                    Intent intent = new Intent(SplitForkCoinsActivity.this, SplitBccSelectAddressActivity.class);
                    intent.putExtra(SplitCoinKey, coin);
                    startActivity(intent);
                }
            }
        }
    };

}
