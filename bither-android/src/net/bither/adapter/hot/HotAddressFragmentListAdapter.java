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
import android.app.Dialog;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.hot.AddressDetailActivity;
import net.bither.model.BitherAddress;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.ui.base.AddressFragmentListItemView;
import net.bither.ui.base.DialogAddressWatchOnlyOption;
import net.bither.ui.base.DialogAddressWithPrivateKeyOption;
import net.bither.ui.base.DialogAddressWithShowPrivateKey;
import net.bither.ui.base.PinnedHeaderAddressExpandableListView;
import net.bither.ui.base.PinnedHeaderExpandableListView.PinnedExpandableListViewAdapter;

import java.util.List;

public class HotAddressFragmentListAdapter extends BaseExpandableListAdapter implements
        PinnedExpandableListViewAdapter {
    private FragmentActivity activity;
    private List<BitherAddress> watchOnlys;
    private List<BitherAddressWithPrivateKey> privates;
    private LayoutInflater mLayoutInflater;
    private PinnedHeaderAddressExpandableListView mListView;
    private boolean isHeaderNeedChange = false;

    public HotAddressFragmentListAdapter(FragmentActivity activity,
                                         List<BitherAddress> watchOnlys,
                                         List<BitherAddressWithPrivateKey> privates,
                                         PinnedHeaderAddressExpandableListView listView) {
        this.activity = activity;
        this.watchOnlys = watchOnlys;
        this.privates = privates;
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
    public Boolean getGroup(int groupPosition) {
        return isPrivate(groupPosition);
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
        return count;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
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
        holder.show(isPrivate(groupPosition));
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

    private static class GroupViewHolder {
        public GroupViewHolder(View v) {
            tvGroup = (TextView) v.findViewById(R.id.tv_group);
            ivType = (ImageView) v.findViewById(R.id.iv_type);
            indicator = (ImageView) v.findViewById(R.id.iv_indicator);
        }

        public void show(boolean isPrivate) {
            if (isPrivate) {
                tvGroup.setText(R.string.address_group_private);
                ivType.setImageResource(R.drawable.address_type_private);
            } else {
                tvGroup.setText(R.string.address_group_watch_only);
                ivType.setImageResource(R.drawable.address_type_watchonly);
            }
        }

        public ImageView indicator;
        public TextView tvGroup;
        public ImageView ivType;
    }

    public BitherAddress getChild(int groupPosition, int childPosition) {
        if (isPrivate(groupPosition)) {
            return privates.get(childPosition);
        } else {
            return watchOnlys.get(childPosition);
        }
    }

    public long getChildId(int groupPosition, int childPosition) {
        BitherAddress a = getChild(groupPosition, childPosition);
        return a.getAddress().hashCode();
    }

    public int getChildrenCount(int groupPosition) {
        if (isPrivate(groupPosition)) {
            if (privates == null) {
                return 0;
            } else {
                return privates.size();
            }
        } else {
            if (watchOnlys == null) {
                return 0;
            } else {
                return watchOnlys.size();
            }
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
        BitherAddress a;
        if (isPrivate(groupPosition)) {
            a = privates.get(childPosition);
            view.ivType.setOnLongClickListener(new AddressLongClick(childPosition, isPrivate(groupPosition)));
        } else {
            a = watchOnlys.get(childPosition);
        }
        view.setAddress(a, childPosition, isPrivate(groupPosition));
        view.setOnClickListener(new AddressDetailClick(childPosition, isPrivate(groupPosition)));
        return convertView;
    }

    private class AddressLongClick implements OnLongClickListener {
        private int position;
        private boolean isPrivate;

        public AddressLongClick(int position, boolean isPrivate) {
            this.position = position;
            this.isPrivate = isPrivate;
        }

        @Override
        public boolean onLongClick(View v) {
            DialogAddressWithShowPrivateKey dialog = new DialogAddressWithShowPrivateKey(activity, privates.get(position));
            dialog.show();
            return true;
        }

    }

    private class AddressDetailClick implements OnClickListener {
        private int position;
        private boolean isPrivate;
        private boolean clicked = false;

        public AddressDetailClick(int position, boolean isPrivate) {
            this.position = position;
            this.isPrivate = isPrivate;
        }

        @Override
        public void onClick(View v) {
            if (!clicked) {
                clicked = true;
                Intent intent = new Intent(activity, AddressDetailActivity.class);
                intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG, position);
                intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_HAS_PRIVATE_KEY_PASS_VALUE_TAG,
                        isPrivate);
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
                holder.show(isPrivate(groupPosition));
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

    private boolean isPrivate(int groupPosition) {
        if (getGroupCount() == 1) {
            return privates != null && privates.size() > 0;
        } else {
            return groupPosition == 0;
        }
    }
}
