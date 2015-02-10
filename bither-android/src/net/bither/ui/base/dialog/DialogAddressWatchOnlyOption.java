/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.ui.base.dialog;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.bitherj.api.http.BitherUrl;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.util.KeyUtil;
import net.bither.util.UIUtil;

import java.util.Locale;

public class DialogAddressWatchOnlyOption extends CenterDialog {
    private DialogFancyQrCode dialogQr;
    private Address address;
    private TextView tvViewOnBlockchainInfo;
    private TextView tvBlockMeta;
    private TextView tvClose;
    private ImageView ivBlockMeta;
    private Activity activity;
    private Runnable afterDelete;


    public DialogAddressWatchOnlyOption(Activity context, Address address,
                                        Runnable afterDelete) {
        super(context);
        this.activity = context;
        this.address = address;
        this.afterDelete = afterDelete;
        setContentView(R.layout.dialog_address_watch_only_option);
        tvViewOnBlockchainInfo = (TextView) findViewById(R.id.tv_view_on_blockchaininfo);
        tvBlockMeta = (TextView) findViewById(R.id.tv_view_on_blockmeta);
        tvClose = (TextView) findViewById(R.id.tv_close);
        ivBlockMeta = (ImageView) findViewById(R.id.iv_blockmeta);
        tvViewOnBlockchainInfo.setOnClickListener(viewOnBlockchainInfoClick);
        tvBlockMeta.setOnClickListener(viewOnBlockMetaClick);
        findViewById(R.id.tv_delete).setOnClickListener(deleteClick);
        tvClose.setOnClickListener(closeClick);
        dialogQr = new DialogFancyQrCode(context, address.getAddress(), false, true);
        String defaultCountry = Locale.getDefault().getCountry();
        if (Utils.compareString(defaultCountry, "CN") || Utils.compareString
                (defaultCountry, "cn")) {
        } else {
            tvBlockMeta.setVisibility(View.GONE);
            ivBlockMeta.setVisibility(View.GONE);
        }
    }

    @Override
    public void show() {
        super.show();
    }

    private View.OnClickListener viewOnBlockchainInfoClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            dismiss();
            UIUtil.gotoBrower(activity,
                    BitherUrl.BLOCKCHAIN_INFO_ADDRESS_URL + address.getAddress());

        }
    };
    private View.OnClickListener viewOnBlockMetaClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismiss();
            UIUtil.gotoBrower(activity,
                    BitherUrl.BLOCKMETA_ADDRESS_URL + address.getAddress());

        }
    };

    private View.OnClickListener deleteClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            new DialogConfirmTask(getContext(), getContext().getString(R.string
                    .address_delete_confirmation), new Runnable() {
                @Override
                public void run() {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DialogProgress dp = new DialogProgress(activity, R.string.please_wait);
                            dp.show();
                            ThreadNeedService threadNeedService = new ThreadNeedService(dp,
                                    activity) {
                                @Override
                                public void runWithService(BlockchainService service) {
                                    KeyUtil.stopMonitor(service, address);
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dp.dismiss();
                                            afterDelete.run();
                                            Fragment f = BitherApplication.hotActivity
                                                    .getFragmentAtIndex(1);
                                            if (f instanceof HotAddressFragment) {
                                                HotAddressFragment hotAddressFragment =
                                                        (HotAddressFragment) f;
                                                hotAddressFragment.refresh();
                                            }
                                        }
                                    });
                                }
                            };
                            threadNeedService.start();
                        }
                    });
                }
            }).show();
        }
    };

    private View.OnClickListener closeClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            dismiss();
        }
    };
}
