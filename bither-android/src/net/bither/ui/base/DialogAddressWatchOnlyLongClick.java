/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.ui.base;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.view.View;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.fragment.Refreshable;
import net.bither.model.BitherAddress;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.util.WalletUtils;

/**
 * Created by songchenwen on 14-7-27.
 */
public class DialogAddressWatchOnlyLongClick extends CenterDialog implements View
        .OnClickListener, DialogInterface.OnDismissListener {
    private Activity activity;
    private BitherAddress address;
    private int clickedId = 0;

    public DialogAddressWatchOnlyLongClick(Activity context, BitherAddress address) {
        super(context);
        setContentView(R.layout.dialog_address_watch_only_long_click);
        this.activity = context;
        this.address = address;
        setOnDismissListener(this);
        findViewById(R.id.tv_delete).setOnClickListener(this);
        findViewById(R.id.tv_remonitor).setOnClickListener(this);
        findViewById(R.id.tv_close).setOnClickListener(this);
    }

    @Override
    public void show() {
        clickedId = 0;
        super.show();
    }

    @Override
    public void onClick(View v) {
        clickedId = v.getId();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        switch (clickedId) {
            case R.id.tv_delete:
                if (BitherApplication.getBitherApplication().isCanStopMonitor()) {
                    new DialogConfirmTask(getContext(), getContext().getString(R.string
                            .address_delete_confirmation), new Runnable() {
                        @Override
                        public void run() {
                            BitherApplication.getBitherApplication().setCanStopMonitor(false);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    WalletUtils.removeBitherAddress(address);
                                    Fragment f = BitherApplication.hotActivity.getFragmentAtIndex
                                            (1);
                                    if (f instanceof Refreshable) {
                                        ((Refreshable) f).doRefresh();
                                    }
                                }
                            });
                        }
                    }).show();
                } else {
                    DropdownMessage.showDropdownMessage(activity,
                            R.string.address_detail_cannot_stop_monitoring);
                }
                break;
            case R.id.tv_remonitor:
                if (BitherApplication.getBitherApplication().isCanRemonitor()) {
                    final DialogProgress dp = new DialogProgress(activity, R.string.please_wait);
                    new DialogConfirmTask(getContext(), getContext().getString(R.string
                            .address_remonitor_confirmation), new Runnable() {
                        @Override
                        public void run() {
                            BitherApplication.getBitherApplication().setCanRemonitor(false);
                            new ThreadNeedService(dp, activity) {
                                @Override
                                public void runWithService(BlockchainService service) {
                                    address.reset(service);
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Fragment f = BitherApplication.hotActivity
                                                    .getFragmentAtIndex(1);
                                            if (f instanceof Refreshable) {
                                                ((Refreshable) f).doRefresh();
                                            }
                                        }
                                    });
                                }
                            };
                        }
                    }).show();
                } else {
                    DropdownMessage.showDropdownMessage(activity,
                            R.string.address_detail_cannot_remonitor);
                }
                break;
        }
    }
}
