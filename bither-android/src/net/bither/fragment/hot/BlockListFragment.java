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

package net.bither.fragment.hot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.adapter.hot.TransactionsOfBlockListAdapter;
import net.bither.bitherj.android.util.NotificationAndroidImpl;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.core.Block;
import net.bither.bitherj.core.BlockChain;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.utils.Utils;
import net.bither.util.WalletUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.Nonnull;

public final class BlockListFragment extends ListFragment {
    private Activity activity;

    private Address address = null;
    private LoaderManager loaderManager;


    private BlockListAdapter adapter;
    private Set<Tx> transactions;

    private static final int ID_BLOCK_LOADER = 0;
    private static final int ID_TRANSACTION_LOADER = 1;

    private static final int MAX_BLOCKS = 32;

    private static final Logger log = LoggerFactory
            .getLogger(BlockListFragment.class);

    private boolean isRegister = false;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        this.activity = (Activity) activity;

        this.loaderManager = getLoaderManager();
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loaderManager.initLoader(ID_BLOCK_LOADER, null,
                blockLoaderCallbacks);


    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AddressManager.getInstance().getAllAddresses().size() > 0) {
            address = AddressManager.getInstance().getAllAddresses().get(0);
        }
        adapter = new BlockListAdapter();
        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isRegister) {
            activity.registerReceiver(tickReceiver, new IntentFilter(
                    Intent.ACTION_TIME_TICK));
            isRegister = true;
        }
        loaderManager.initLoader(ID_TRANSACTION_LOADER, null,
                transactionLoaderCallbacks);

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        loaderManager.destroyLoader(ID_TRANSACTION_LOADER);
        if (isRegister) {
            activity.unregisterReceiver(tickReceiver);
            isRegister = false;
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {


        super.onDestroy();
    }

    @Override
    public void onListItemClick(final ListView l, final View v,
                                final int position, final long id) {
        final Block storedBlock = adapter.getItem(position);

//        activity.startActionMode(new ActionMode.Callback() {
//            @Override
//            public boolean onCreateActionMode(final ActionMode mode,
//                                              final Menu menu) {
//                final MenuInflater inflater = mode.getMenuInflater();
//                inflater.inflate(R.menu.blocks_context, menu);
//
//                return true;
//            }
//
//            @Override
//            public boolean onPrepareActionMode(final ActionMode mode,
//                                               final Menu menu) {
//                mode.setTitle(Integer.toString(storedBlock.getBlockNo()));
//                mode.setSubtitle(storedBlock.getHashAsString());
//
//                return true;
//            }
//
//            @Override
//            public boolean onActionItemClicked(final ActionMode mode,
//                                               final MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.blocks_context_browse:
//                        startActivity(new Intent(
//                                Intent.ACTION_VIEW,
//                                Uri.parse(Constants.EXPLORE_BASE_URL + "block/"
//                                        + storedBlock.getHashAsString())));
//
//                        mode.finish();
//                        return true;
//                }
//
//                return false;
//            }
//
//            @Override
//            public void onDestroyActionMode(final ActionMode mode) {
//            }
//        });
    }


    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            adapter.notifyDataSetChanged();
        }
    };

    private final class BlockListAdapter extends BaseAdapter {
        private static final int ROW_BASE_CHILD_COUNT = 2;
        private static final int ROW_INSERT_INDEX = 1;
        private final TransactionsOfBlockListAdapter transactionsAdapter = new TransactionsOfBlockListAdapter(
                activity, address, BitherjSettings.MaxPeerConnections, false);

        private final List<Block> blocks = new ArrayList<Block>(
                MAX_BLOCKS);

        public void clear() {
            blocks.clear();

            adapter.notifyDataSetChanged();
        }

        public void replace(@Nonnull final Collection<Block> blocks) {
            this.blocks.clear();
            this.blocks.addAll(blocks);

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return blocks.size();
        }

        @Override
        public Block getItem(final int position) {
            return blocks.get(position);
        }

        @Override
        public long getItemId(final int position) {
            return Utils.longHash(Utils.reverseBytes(blocks.get(position).getBlockHash()));
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(final int position, final View convertView,
                            final ViewGroup parent) {
            final ViewGroup row;
            if (convertView == null)
                row = (ViewGroup) getLayoutInflater(null).inflate(
                        R.layout.fragment_block_row, null);
            else
                row = (ViewGroup) convertView;

            final Block storedBlock = getItem(position);


            final TextView rowHeight = (TextView) row
                    .findViewById(R.id.block_list_row_height);
            final int height = storedBlock.getBlockNo();
            rowHeight.setText(Integer.toString(height));

            final TextView rowTime = (TextView) row
                    .findViewById(R.id.block_list_row_time);
            final long timeMs = storedBlock.getBlockTime()
                    * DateUtils.SECOND_IN_MILLIS;
            rowTime.setText(DateUtils.getRelativeDateTimeString(activity,
                    timeMs, DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS, 0));

            final TextView rowHash = (TextView) row
                    .findViewById(R.id.block_list_row_hash);
            rowHash.setText(WalletUtils.formatHash(null,
                    Utils.bytesToHexString(Utils.reverseBytes(storedBlock.getBlockHash())), 8, 0, ' '));

            final int transactionChildCount = row.getChildCount()
                    - ROW_BASE_CHILD_COUNT;
            int iTransactionView = 0;

            if (transactions != null) {
                final int btcPrecision = 2;
                final int btcShift = 3;

                transactionsAdapter.setPrecision(btcPrecision, btcShift);

                for (final Tx tx : transactions) {
                    if (tx.getBlockNo() == storedBlock.getBlockNo()) {
                        final View view;
                        if (iTransactionView < transactionChildCount) {
                            view = row.getChildAt(ROW_INSERT_INDEX
                                    + iTransactionView);
                        } else {
                            view = getLayoutInflater(null).inflate(
                                    R.layout.transaction_row_oneline, null);
                            row.addView(view, ROW_INSERT_INDEX
                                    + iTransactionView);
                        }

                        transactionsAdapter.bindView(view, tx);

                        iTransactionView++;
                    }
                }
            }

            final int leftoverTransactionViews = transactionChildCount
                    - iTransactionView;
            if (leftoverTransactionViews > 0)
                row.removeViews(ROW_INSERT_INDEX + iTransactionView,
                        leftoverTransactionViews);

            return row;
        }
    }

    private static class BlockLoader extends AsyncTaskLoader<List<Block>> {
        private Context context;


        private BlockLoader(final Context context) {
            super(context);

            this.context = context.getApplicationContext();

        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();

            context.registerReceiver(broadcastReceiver, new IntentFilter(
                    NotificationAndroidImpl.ACTION_SYNC_LAST_BLOCK_CHANGE));
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            context.unregisterReceiver(broadcastReceiver);

            super.onStopLoading();
        }

        @Override
        public List<Block> loadInBackground() {
            return getRecentBlocks(100);
        }

        public List<Block> getRecentBlocks(final int maxBlocks) {
            final List<Block> blocks = new ArrayList<Block>();
            Block block = BlockChain.getInstance().getLastBlock();
            while (block != null) {
                blocks.add(block);
                if (blocks.size() >= maxBlocks) {
                    break;
                }
                block = BlockChain.getInstance().getBlock(block.getBlockPrev());
            }
            return blocks;
        }

        private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                try {
                    forceLoad();
                } catch (final RejectedExecutionException x) {
                    log.info("rejected execution: "
                            + BlockLoader.this.toString());
                }
            }
        };
    }

    private final LoaderCallbacks<List<Block>> blockLoaderCallbacks = new LoaderCallbacks<List<Block>>() {
        @Override
        public Loader<List<Block>> onCreateLoader(final int id,
                                                  final Bundle args) {
            return new BlockLoader(activity);
        }

        @Override
        public void onLoadFinished(final Loader<List<Block>> loader,
                                   final List<Block> blocks) {
            adapter.replace(blocks);

            final Loader<Set<Tx>> transactionLoader = loaderManager
                    .getLoader(ID_TRANSACTION_LOADER);
            if (transactionLoader != null && transactionLoader.isStarted())
                transactionLoader.forceLoad();
        }

        @Override
        public void onLoaderReset(final Loader<List<Block>> loader) {
            adapter.clear();
        }
    };

    private static class TransactionsLoader extends
            AsyncTaskLoader<Set<Tx>> {
        private final Address address;

        private TransactionsLoader(final Context context, final Address address) {
            super(context);

            this.address = address;
        }

        @Override
        public Set<Tx> loadInBackground() {
            final Set<Tx> transactions = new HashSet<Tx>();
            if (address != null) {
                for (Tx tx : address.getTxs()) {
                    transactions.add(tx);
                }
            }
            final Set<Tx> filteredTransactions = new HashSet<Tx>(
                    transactions.size());
            for (final Tx tx : transactions) {

                if (tx.getBlockNo() > 0) // TODO filter by
                    // updateTime
                    filteredTransactions.add(tx);
            }

            return filteredTransactions;
        }
    }

    private final LoaderCallbacks<Set<Tx>> transactionLoaderCallbacks = new LoaderCallbacks<Set<Tx>>() {
        @Override
        public Loader<Set<Tx>> onCreateLoader(final int id,
                                              final Bundle args) {
            return new TransactionsLoader(activity, address);
        }

        @Override
        public void onLoadFinished(final Loader<Set<Tx>> loader,
                                   final Set<Tx> transactions) {
            BlockListFragment.this.transactions = transactions;

            adapter.notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(final Loader<Set<Tx>> loader) {
            BlockListFragment.this.transactions.clear(); // be nice
            BlockListFragment.this.transactions = null;

            adapter.notifyDataSetChanged();
        }
    };
}
