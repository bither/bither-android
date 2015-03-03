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

import android.content.Context;

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.utils.Utils;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/3/3.
 */
public class DialogAddressAlias extends DialogWithActions {
    public static interface DialogAddressAliasDelegate {
        public void onAddressAliasChanged(Address address, String alias);
    }

    private DialogAddressAliasDelegate delegate;
    private Address address;

    public DialogAddressAlias(Context context, Address address, DialogAddressAliasDelegate
            delegate) {
        super(context);
        this.address = address;
        this.delegate = delegate;
    }

    @Override
    public void show() {
        if (Utils.isEmpty(address.getAlias())) {
            new DialogAddressAliasInput(getContext(), address, delegate).show();
            return;
        }
        super.show();
    }

    @Override
    protected List<Action> getActions() {
        boolean hasAlias = !Utils.isEmpty(address.getAlias());
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new Action(hasAlias ? R.string.address_alias_edit : R.string
                .address_alias_add, new Runnable() {
            @Override
            public void run() {
                new DialogAddressAliasInput(getContext(), address, delegate).show();
            }
        }));
        if (hasAlias) {
            actions.add(new Action(R.string.address_alias_remove, new Runnable() {
                @Override
                public void run() {
                    new DialogConfirmTask(getContext(), getContext().getString(R.string
                            .address_alias_remove_confirm), new Runnable() {
                        @Override
                        public void run() {
                            address.removeAlias();
                            ThreadUtil.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (delegate != null) {
                                        delegate.onAddressAliasChanged(address, null);
                                    }
                                }
                            });
                        }
                    }).show();
                }
            }));
        }
        return actions;
    }
}
