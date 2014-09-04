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
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Peer;
import net.bither.bitherj.core.PeerManager;
import net.bither.bitherj.utils.NotificationUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.Nonnull;

public final class PeerListFragment extends ListFragment {
    private Activity activity;
    private LoaderManager loaderManager;

    private ArrayAdapter<Peer> adapter;

    private final Handler handler = new Handler();

    private static final long REFRESH_MS = DateUtils.SECOND_IN_MILLIS;

    private static final int ID_PEER_LOADER = 0;
    private static final int ID_REVERSE_DNS_LOADER = 1;

    private final Map<InetAddress, String> hostnames = new WeakHashMap<InetAddress, String>();

    private static final Logger log = LoggerFactory
            .getLogger(PeerListFragment.class);

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        this.activity = activity;
        this.loaderManager = getLoaderManager();
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setEmptyText("No peers connected");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new ArrayAdapter<Peer>(activity, 0) {
            @Override
            public View getView(final int position, View row,
                                final ViewGroup parent) {
                if (row == null)
                    row = getLayoutInflater(null).inflate(
                            R.layout.fragment_peer_list_row, null);

                final Peer peer = getItem(position);

                final boolean isDownloading = peer.getDownloadData();
                final TextView rowIp = (TextView) row
                        .findViewById(R.id.peer_list_row_ip);
                final InetAddress address = peer.getAddress().getAddr();
                final String hostname = hostnames.get(address);
                rowIp.setText(hostname != null ? hostname : address
                        .getHostAddress());

                final TextView rowHeight = (TextView) row
                        .findViewById(R.id.peer_list_row_height);
                final long bestHeight = peer.getDisplayLastBlockHeight();
                rowHeight.setText(bestHeight > 0 ? bestHeight + " blocks"
                        : null);
                rowHeight.setTypeface(isDownloading ? Typeface.DEFAULT_BOLD
                        : Typeface.DEFAULT);

                final TextView rowVersion = (TextView) row
                        .findViewById(R.id.peer_list_row_version);
                rowVersion.setText(peer.getSubVersion());
                rowVersion.setTypeface(isDownloading ? Typeface.DEFAULT_BOLD
                        : Typeface.DEFAULT);

                final TextView rowProtocol = (TextView) row
                        .findViewById(R.id.peer_list_row_protocol);
                rowProtocol
                        .setText("protocol: " + peer.getClientVersion());
                rowProtocol.setTypeface(isDownloading ? Typeface.DEFAULT_BOLD
                        : Typeface.DEFAULT);

                final TextView rowPing = (TextView) row
                        .findViewById(R.id.peer_list_row_ping);
                final long pingTime = peer.pingTime;
                rowPing.setText(pingTime < Long.MAX_VALUE ? getString(
                        R.string.peer_list_row_ping_time, pingTime) : null);
                rowPing.setTypeface(isDownloading ? Typeface.DEFAULT_BOLD
                        : Typeface.DEFAULT);

                return row;
            }

            @Override
            public boolean isEnabled(final int position) {
                return false;
            }
        };
        setListAdapter(adapter);
        loaderManager.initLoader(ID_PEER_LOADER, null, peerLoaderCallbacks);
    }

    @Override
    public void onResume() {
        super.onResume();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();

                final Loader<String> loader = loaderManager
                        .getLoader(ID_REVERSE_DNS_LOADER);
                final boolean loaderRunning = loader != null
                        && loader.isStarted();

                if (!loaderRunning) {
                    for (int i = 0; i < adapter.getCount(); i++) {
                        final Peer peer = adapter.getItem(i);
                        final InetAddress address = peer.getAddress().getAddr();

                        if (!hostnames.containsKey(address)) {
                            final Bundle args = new Bundle();
                            args.putSerializable("address", address);
                            loaderManager.initLoader(ID_REVERSE_DNS_LOADER,
                                    args, reverseDnsLoaderCallbacks)
                                    .forceLoad();

                            break;
                        }
                    }
                }

                handler.postDelayed(this, REFRESH_MS);
            }
        }, REFRESH_MS);
    }

    @Override
    public void onPause() {
        handler.removeCallbacksAndMessages(null);

        super.onPause();
    }

    @Override
    public void onDestroy() {

        loaderManager.destroyLoader(ID_REVERSE_DNS_LOADER);
        loaderManager.destroyLoader(ID_PEER_LOADER);

        super.onDestroy();
    }


    private static class PeerLoader extends AsyncTaskLoader<List<Peer>> {
        private Context context;


        private PeerLoader(final Context context) {
            super(context);

            this.context = context.getApplicationContext();

        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();

            context.registerReceiver(broadcastReceiver, new IntentFilter(
                    NotificationUtil.ACTION_PEER_STATE));
        }

        @Override
        protected void onStopLoading() {
            context.unregisterReceiver(broadcastReceiver);

            super.onStopLoading();
        }

        @Override
        public List<Peer> loadInBackground() {
            return PeerManager.instance().getConnectedPeers();
        }

        private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                try {
                    forceLoad();
                } catch (final RejectedExecutionException x) {
                    log.info("rejected execution: "
                            + PeerLoader.this.toString());
                }
            }
        };
    }

    private final LoaderCallbacks<List<Peer>> peerLoaderCallbacks = new LoaderCallbacks<List<Peer>>() {
        @Override
        public Loader<List<Peer>> onCreateLoader(final int id, final Bundle args) {
            return new PeerLoader(activity);
        }

        @Override
        public void onLoadFinished(final Loader<List<Peer>> loader,
                                   final List<Peer> peers) {
            adapter.clear();

            if (peers != null)
                for (final Peer peer : peers)
                    adapter.add(peer);
        }

        @Override
        public void onLoaderReset(final Loader<List<Peer>> loader) {
            adapter.clear();
        }
    };

    private static class ReverseDnsLoader extends AsyncTaskLoader<String> {
        public final InetAddress address;

        public ReverseDnsLoader(final Context context,
                                @Nonnull final InetAddress address) {
            super(context);

            this.address = address;
        }

        @Override
        public String loadInBackground() {
            return address.getCanonicalHostName();
        }
    }

    private final LoaderCallbacks<String> reverseDnsLoaderCallbacks = new LoaderCallbacks<String>() {
        @Override
        public Loader<String> onCreateLoader(final int id, final Bundle args) {
            final InetAddress address = (InetAddress) args
                    .getSerializable("address");

            return new ReverseDnsLoader(activity, address);
        }

        @Override
        public void onLoadFinished(final Loader<String> loader,
                                   final String hostname) {
            final InetAddress address = ((ReverseDnsLoader) loader).address;
            hostnames.put(address, hostname);

            loaderManager.destroyLoader(ID_REVERSE_DNS_LOADER);
        }

        @Override
        public void onLoaderReset(final Loader<String> loader) {
        }
    };
}
