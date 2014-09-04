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

package net.bither.ui.base;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Address;

public class AddAddressWithPrivateKeyListItem extends FrameLayout {

    private TextView tvAddress;
    private ImageButton ibtnDelete;
    private Address address;

    public AddAddressWithPrivateKeyListItem(Context context) {
        super(context);
        initView();
    }

    private AddAddressWithPrivateKeyListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private AddAddressWithPrivateKeyListItem(Context context,
                                             AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        removeAllViews();
        addView(LayoutInflater.from(getContext()).inflate(
                        R.layout.list_item_add_address_with_private_key, null),
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        ibtnDelete = (ImageButton) findViewById(R.id.ibtn_delete);
        ibtnDelete.setOnClickListener(deleteClick);
    }

    public void setAddress(Address address) {
        this.address = address;
        tvAddress.setText(address.getAddress());
    }

    public Address getAddress() {
        return address;
    }

    private OnClickListener deleteClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            ViewParent parent = getParent();
            if (parent != null && parent instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) parent;
                group.removeView(AddAddressWithPrivateKeyListItem.this);
            }
        }
    };

}
