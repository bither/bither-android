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

package net.bither.adapter.hot;

import android.annotation.TargetApi;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.hot.AddHDMAddressActivity;
import net.bither.activity.hot.AddressDetailActivity;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.ui.base.AddressFragmentListItemView;
import net.bither.ui.base.PinnedHeaderAddressExpandableListView;
import net.bither.ui.base.PinnedHeaderExpandableListView.PinnedExpandableListViewAdapter;
import net.bither.ui.base.dialog.DialogAddressWatchOnlyLongClick;
import net.bither.ui.base.dialog.DialogAddressWithShowPrivateKey;
import net.bither.ui.base.dialog.DialogHDMSeedOptions;
import net.bither.ui.base.dialog.DialogProgress;

import java.util.List;

public class HotAddressFragmentListAdapter extends BaseExpandableListAdapter implements
        PinnedExpandableListViewAdapter {
    private static final int PrivateGroupTag = 0;
    private static final int WatchOnlyGroupTag = 1;
    private static final int HDMGroupTag = 2;

    private FragmentActivity activity;
    private List<Address> watchOnlys;
    private List<Address> privates;
    private List<HDMAddress> hdms;
    private LayoutInflater mLayoutInflater;
    private PinnedHeaderAddressExpandableListView mListView;
    private boolean isHeaderNeedChange = false;

    public HotAddressFragmentListAdapter(FragmentActivity activity, List<Address> watchOnlys,
                                         List<Address> privates, List<HDMAddress> hdms,
                                         PinnedHeaderAddressExpandableListView listView) {
        this.activity = activity;
        this.watchOnlys = watchOnlys;
        this.privates = privates;
        this.hdms = hdms;
        mLayoutInflater = LayoutInflater.from(activity);
        mListView = listView;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        this.notifyHeaderChange();
    }

    public void notifyHeaderChange() {
        isHeaderNeedChange = true;
    }

    /**
     * Some other configuration
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public Integer getGroup(int groupPosition) {
        return Integer.valueOf((int) getGroupId(groupPosition));
    }

    @Override
    public int getGroupCount() {
        int count = 0;
        if (privates != null && privates.size() > 0) {
            count++;
        }
        if (watchOnlys != null && watchOnlys.size() > 0) {
            count++;
        }
        if (hdms != null && hdms.size() > 0) {
            count++;
        }
        return count;
    }

    @Override
    public long getGroupId(int groupPosition) {
        if (groupPosition == getPrivateGroupIndex()) {
            return PrivateGroupTag;
        }
        if (groupPosition == getWatchOnlyGroupIndex()) {
            return WatchOnlyGroupTag;
        }
        if (groupPosition == getHDMGroupIndex()) {
            return HDMGroupTag;
        }
        return -1;
    }

    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        GroupViewHolder holder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_item_address_group, null);
            holder = new GroupViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (GroupViewHolder) convertView.getTag();
        }
        holder.show((int) getGroupId(groupPosition));
        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListView.isGroupExpanded(groupPosition)) {
                    mListView.collapseGroup(groupPosition);
                } else {
                    mListView.expandGroup(groupPosition);
                }
            }
        });
        return convertView;
    }

    private class GroupViewHolder {
        public GroupViewHolder(View v) {
            tvGroup = (TextView) v.findViewById(R.id.tv_group);
            ivType = (ImageView) v.findViewById(R.id.iv_type);
            indicator = (ImageView) v.findViewById(R.id.iv_indicator);
            llHDM = (LinearLayout) v.findViewById(R.id.ll_hdm);
            flHDMAdd = (FrameLayout) v.findViewById(R.id.fl_hdm_add);
            flHDMSeed = (FrameLayout) v.findViewById(R.id.fl_hdm_seed);
        }

        public void show(int groupTag) {
            show(groupTag, false);
        }

        public void show(int groupTag, boolean touch) {
            switch (groupTag) {
                case PrivateGroupTag:
                    tvGroup.setText(R.string.address_group_private);
                    ivType.setImageResource(R.drawable.address_type_private);
                    llHDM.setVisibility(View.GONE);
                    ivType.setVisibility(View.VISIBLE);
                    return;
                case WatchOnlyGroupTag:
                    tvGroup.setText(R.string.address_group_watch_only);
                    ivType.setImageResource(R.drawable.address_type_watchonly);
                    llHDM.setVisibility(View.GONE);
                    ivType.setVisibility(View.VISIBLE);
                    return;
                case HDMGroupTag:
                    tvGroup.setText(R.string.address_group_hdm_hot);
                    ivType.setVisibility(View.GONE);
                    llHDM.setVisibility(View.VISIBLE);
                    configureHDM(touch);
                    return;
            }
        }

        private void configureHDM(boolean touch) {
            if (!touch) {
                flHDMSeed.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hdmSeed();
                    }
                });

                flHDMAdd.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hdmAdd();
                    }
                });
            }
        }

        private void hdmAdd() {
            activity.startActivityForResult(new Intent(activity, AddHDMAddressActivity.class),
                    BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
        }

        private void hdmSeed() {
            DialogProgress dp = new DialogProgress(flHDMSeed.getContext(), R.string.please_wait);
            dp.setCancelable(false);
            new DialogHDMSeedOptions(flHDMSeed.getContext(), AddressManager.getInstance()
                    .getHdmKeychain(), dp).show();
        }

        public ImageView indicator;
        public TextView tvGroup;
        public ImageView ivType;
        public LinearLayout llHDM;
        public FrameLayout flHDMSeed;
        public FrameLayout flHDMAdd;
    }

    public Address getChild(int groupPosition, int childPosition) {
        switch ((int) getGroupId(groupPosition)) {
            case PrivateGroupTag:
                return privates.get(childPosition);
            case WatchOnlyGroupTag:
                return watchOnlys.get(childPosition);
            case HDMGroupTag:
                return hdms.get(childPosition);
            default:
                return null;
        }
    }

    public long getChildId(int groupPosition, int childPosition) {
        Address a = getChild(groupPosition, childPosition);
        return a.getAddress().hashCode();
    }

    public int getChildrenCount(int groupPosition) {
        switch ((int) getGroupId(groupPosition)) {
            case PrivateGroupTag:
                return privates == null ? 0 : privates.size();
            case WatchOnlyGroupTag:
                return watchOnlys == null ? 0 : watchOnlys.size();
            case HDMGroupTag:
                return hdms == null ? 0 : hdms.size();
            default:
                return -1;
        }
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        AddressFragmentListItemView view;
        if (convertView == null || !(convertView instanceof AddressFragmentListItemView)) {
            convertView = new AddressFragmentListItemView(activity);
        }
        view = (AddressFragmentListItemView) convertView;
        Address a = getChild(groupPosition, childPosition);
        view.ivType.setOnLongClickListener(new AddressLongClick(childPosition, (int)
                getGroupId(groupPosition)));
        view.setAddress(a);
        view.setOnClickListener(new AddressDetailClick(childPosition, a.hasPrivKey(), a.isHDM()));
        return convertView;
    }

    private class AddressLongClick implements OnLongClickListener {
        private int position;
        private int groupTag;

        public AddressLongClick(int position, int groupTag) {
            this.position = position;
            this.groupTag = groupTag;
        }

        @Override
        public boolean onLongClick(View v) {
            switch (groupTag) {
                case PrivateGroupTag:
                    new DialogAddressWithShowPrivateKey(activity, privates.get(position)).show();
                case WatchOnlyGroupTag:
                    new DialogAddressWatchOnlyLongClick(activity, watchOnlys.get(position)).show();
                case HDMGroupTag:
                    //TODO hdm address dialog
            }
            return true;
        }
    }

    private class AddressDetailClick implements OnClickListener {
        private int position;
        private boolean isPrivate;
        private boolean isHDM;
        private boolean clicked = false;

        public AddressDetailClick(int position, boolean isPrivate, boolean isHDM) {
            this.position = position;
            this.isPrivate = isPrivate;
            this.isHDM = isHDM;
        }

        @Override
        public void onClick(View v) {
            if (!clicked) {
                clicked = true;
                Intent intent = new Intent(activity, AddressDetailActivity.class);
                intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG, position);
                intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_HAS_PRIVATE_KEY_PASS_VALUE_TAG,
                        isPrivate);
                intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_IS_HDM_KEY_PASS_VALUE_TAG, isHDM);
                activity.startActivity(intent);
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clicked = false;
                    }
                }, 500);
            }
        }
    }

    public int getPinnedHeaderState(int groupPosition, int childPosition) {
        if (groupPosition < 0 || groupPosition >= 2) {
            return PINNED_HEADER_GONE;
        }
        final int childCount = getChildrenCount(groupPosition);
        if (childPosition == childCount - 1) {
            return PINNED_HEADER_PUSHED_UP;
        } else if (childPosition == -1 && !this.mListView.isGroupExpanded(groupPosition)) {
            return PINNED_HEADER_GONE;
        } else {
            return PINNED_HEADER_VISIBLE;
        }
    }

    private int mGroupPosition = -1;

    @TargetApi(11)
    public void configurePinnedHeader(final View header, final int groupPosition,
                                      int childPosition, int alpha) {
        if (groupPosition != mGroupPosition || isHeaderNeedChange) {
            if (groupPosition >= 0 && groupPosition < getGroupCount()) {
                GroupViewHolder holder = new GroupViewHolder(header);
                holder.show((int) getGroupId(groupPosition), true);
                holder.indicator.setVisibility(View.VISIBLE);
                try {
                    if (android.os.Build.VERSION.SDK_INT > 10) {
                        header.setAlpha(0.8f);
                    } else {
                        header.getBackground().setAlpha((int) (255 * 0.8f));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mGroupPosition = groupPosition;
            }
        }
        isHeaderNeedChange = false;
    }

    public int getPrivateGroupIndex() {
        if (privates == null || privates.size() == 0) {
            return -1;
        }
        return 0;
    }

    public int getWatchOnlyGroupIndex() {
        if (watchOnlys == null || watchOnlys.size() == 0) {
            return -1;
        }
        int index = 0;
        if (privates != null && privates.size() > 0) {
            index++;
        }
        return index;
    }

    public int getHDMGroupIndex() {
        if (hdms == null || hdms.size() == 0) {
            return -1;
        }
        int index = 0;
        if (privates != null && privates.size() > 0) {
            index++;
        }
        if (watchOnlys != null && watchOnlys.size() > 0) {
            index++;
        }
        return index;
    }
}
