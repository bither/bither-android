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

package net.bither.ui.base.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.view.View;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.fragment.Refreshable;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.util.KeyUtil;

public class DialogAddressWatchOnlyLongClick extends CenterDialog implements View
        .OnClickListener, DialogInterface.OnDismissListener {
    private Activity activity;
    private Address address;
    private int clickedId = 0;
    private DialogProgress dp;

    public DialogAddressWatchOnlyLongClick(Activity context, Address address) {
        super(context);
        setContentView(R.layout.dialog_address_watch_only_long_click);
        this.activity = context;
        this.address = address;
        setOnDismissListener(this);
        findViewById(R.id.tv_delete).setOnClickListener(this);

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
                new DialogConfirmTask(getContext(), getContext().getString(R.string
                        .address_delete_confirmation), new Runnable() {
                    @Override
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dp = new DialogProgress(activity, R.string.please_wait);
                                dp.show();
                                ThreadNeedService threadNeedService = new ThreadNeedService(dp, activity) {
                                    @Override
                                    public void runWithService(BlockchainService service) {
                                        KeyUtil.stopMonitor(service, address);
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dp.dismiss();
                                                Fragment f = BitherApplication.hotActivity.getFragmentAtIndex
                                                        (1);
                                                if (f instanceof HotAddressFragment) {
                                                    HotAddressFragment hotAddressFragment = (HotAddressFragment) f;
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
                break;

        }
    }
}
