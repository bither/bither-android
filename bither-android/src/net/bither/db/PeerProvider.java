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

package net.bither.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.bither.BitherApplication;
import net.bither.bitherj.core.Peer;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.IPeerProvider;
import net.bither.bitherj.utils.Utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PeerProvider implements IPeerProvider {
    private static PeerProvider peerProvider = new PeerProvider(BitherApplication.mTxDbHelper);

    public static PeerProvider getInstance() {
        return peerProvider;
    }

    private SQLiteOpenHelper mDb;

    public PeerProvider(SQLiteOpenHelper db) {
        this.mDb = db;
    }

    public List<Peer> getAllPeers() {
        List<Peer> peers = new ArrayList<Peer>();
        String sql = "select * from peers";
        SQLiteDatabase db = mDb.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            Peer peer = applyCursor(c);
            if (peer != null) {
                peers.add(peer);
            }
        }
        c.close();
        return peers;

    }

    public void deletePeersNotInAddresses(List<InetAddress> peerAddrsses) {
        List<Long> needDeletePeers = new ArrayList<Long>();
        String sql = "select peer_address from peers";
        SQLiteDatabase db = mDb.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            int idColumn = c.getColumnIndex(AbstractDb.PeersColumns.PEER_ADDRESS);
            if (idColumn != -1) {
                long peerAddress = c.getLong(idColumn);
                boolean in = false;
                for (InetAddress a : peerAddrsses) {
                    if (Utils.parseLongFromAddress(a) == peerAddress) {
                        in = true;
                        break;
                    }
                }
                if (!in) {
                    needDeletePeers.add(peerAddress);
                }
            }

        }
        c.close();
        db = mDb.getWritableDatabase();
        db.beginTransaction();
        for (long i : needDeletePeers) {
            db.delete(AbstractDb.Tables.PEERS, AbstractDb.PeersColumns
                    .PEER_ADDRESS + "=?", new String[]{Long.toString(i)});
        }
        db.setTransactionSuccessful();
        db.endTransaction();

    }

    public ArrayList<InetAddress> exists(ArrayList<InetAddress> peerAddresses) {
        ArrayList<InetAddress> exists = new ArrayList<InetAddress>();
        List<Peer> peerItemList = getAllPeers();
        for (Peer item : peerItemList) {
            if (peerAddresses.contains(item.getPeerAddress())) {
                exists.add(item.getPeerAddress());
            }
        }
        return exists;
    }

    public void addPeers(List<Peer> items) {
        List<Peer> addItems = new ArrayList<Peer>();
        List<Peer> allItems = getAllPeers();
        for (Peer peerItem : items) {
            if (!allItems.contains(peerItem) && !addItems.contains(peerItem)) {
                addItems.add(peerItem);
            }
        }
        if (addItems.size() > 0) {
            SQLiteDatabase db = this.mDb.getWritableDatabase();
            db.beginTransaction();

            for (Peer item : addItems) {

                ContentValues cv = new ContentValues();
                applyContentValues(item, cv);
                db.insert(AbstractDb.Tables.PEERS, null, cv);
            }

            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    public void updatePeersTimestamp(List<InetAddress> peerAddresses) {
        long timestamp = new Date().getTime();
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        for (InetAddress peerAddress : peerAddresses) {
            ContentValues cv = new ContentValues();
            cv.put(AbstractDb.PeersColumns.PEER_TIMESTAMP, timestamp);
            db.update(AbstractDb.Tables.PEERS, cv, AbstractDb.PeersColumns
                    .PEER_ADDRESS + "=?", new String[]{Long.toString(Utils.parseLongFromAddress
                    (peerAddress))});
        }
        db.setTransactionSuccessful();
        db.endTransaction();

    }

    public void removePeer(InetAddress address) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(AbstractDb.Tables.PEERS, AbstractDb.PeersColumns
                    .PEER_ADDRESS + "=?", new String[]{Long.toString(Utils.parseLongFromAddress
                    (address))});
        }
    }

    public void conncetFail(InetAddress address) {
        long addressLong = Utils.parseLongFromAddress(address);
        String sql = "select count(0) cnt from peers where peer_address=" + Long.toString
                (addressLong) + " and peer_connected_cnt=0";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        int cnt = 0;
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("cnt");
            if (idColumn != -1) {
                cnt = c.getInt(idColumn);
            }

        }
        c.close();
        db = this.mDb.getWritableDatabase();
        if (cnt == 0) {
            sql = "update peers set peer_connected_cnt=peer_connected_cnt+1 where peer_address="
                    + Long.toString(addressLong);
            db.execSQL(sql);
        } else {
            sql = "update peers set peer_connected_cnt=2 where peer_address=" + Long.toString
                    (addressLong);
            db.execSQL(sql);
        }

    }

    public void connectSucceed(InetAddress address) {
        long addressLong = Utils.parseLongFromAddress(address);
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.PeersColumns.PEER_CONNECTED_CNT, 1);
        cv.put(AbstractDb.PeersColumns.PEER_TIMESTAMP, new Date().getTime());
        db.update(AbstractDb.Tables.PEERS, cv, "peer_address=?",
                new String[]{Long.toString(addressLong)});
    }

    public List<Peer> getPeersWithLimit(int limit) {
        List<Peer> peerItemList = new ArrayList<Peer>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from peers order by peer_address limit " + Integer.toString(limit);
        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            Peer peer = applyCursor(c);
            if (peer != null) {
                peerItemList.add(peer);
            }
        }
        c.close();

        return peerItemList;
    }

    public void clearIPV6() {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.delete(AbstractDb.Tables.PEERS, "peer_address>? or peer_address<? or peer_address=0"
                , new String[]{Integer.toString(Integer.MAX_VALUE), Integer.toString(Integer.MIN_VALUE)});
    }

    public void cleanPeers() {
        int maxPeerSaveCnt = 12;
        String disconnectingPeerCntSql = "select count(0) cnt from peers where " +
                "peer_connected_cnt<>1";
        int disconnectingPeerCnt = 0;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(disconnectingPeerCntSql, null);
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("cnt");
            if (idColumn != -1) {
                disconnectingPeerCnt = c.getInt(idColumn);
            }
        }
        c.close();
        if (disconnectingPeerCnt > maxPeerSaveCnt) {
            String sql = "select peer_timestamp from peers where peer_connected_cnt<>1 " +
                    " limit 1 offset " + Integer.toString
                    (maxPeerSaveCnt);
            c = db.rawQuery(sql, null);
            long timestamp = 0;
            if (c.moveToNext()) {
                int idColumn = c.getColumnIndex(AbstractDb.PeersColumns.PEER_TIMESTAMP);
                if (idColumn != -1) {
                    timestamp = c.getLong(idColumn);
                }
            }
            c.close();
            if (timestamp > 0) {
                db = this.mDb.getWritableDatabase();
                db.delete(AbstractDb.Tables.PEERS, "peer_connected_cnt<>1 and " +
                        "peer_timestamp<=?", new String[]{Long.toString(timestamp)});
            }
        }
    }

    private void applyContentValues(Peer item, ContentValues cv) {
        cv.put(AbstractDb.PeersColumns.PEER_ADDRESS, Utils.parseLongFromAddress(item
                .getPeerAddress()));
        cv.put(AbstractDb.PeersColumns.PEER_CONNECTED_CNT, item.getPeerConnectedCnt());
        cv.put(AbstractDb.PeersColumns.PEER_PORT, item.getPeerPort());
        cv.put(AbstractDb.PeersColumns.PEER_SERVICES, item.getPeerServices());
        cv.put(AbstractDb.PeersColumns.PEER_TIMESTAMP, item.getPeerTimestamp());

    }

    private void deleteUnknowHost(long address) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.delete(AbstractDb.Tables.PEERS, "peer_address=?"
                , new String[]{Long.toString(address)});

    }

    private Peer applyCursor(Cursor c) {
        InetAddress address = null;
        int idColumn = c.getColumnIndex(AbstractDb.PeersColumns.PEER_ADDRESS);
        if (idColumn != -1) {
            long addressLong = c.getLong(idColumn);
            try {
                if (addressLong >= Integer.MIN_VALUE && addressLong <= Integer.MAX_VALUE) {
                    address = Utils.parseAddressFromLong(c.getLong(idColumn));
                } else {
                    clearIPV6();
                }
            } catch (UnknownHostException e) {
                deleteUnknowHost(addressLong);
                e.printStackTrace();
                return null;
            }
        }
        Peer peerItem = new Peer(address);
        idColumn = c.getColumnIndex(AbstractDb.PeersColumns.PEER_CONNECTED_CNT);
        if (idColumn != -1) {
            peerItem.setPeerConnectedCnt(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.PeersColumns.PEER_PORT);
        if (idColumn != -1) {
            peerItem.setPeerPort(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.PeersColumns.PEER_SERVICES);
        if (idColumn != -1) {
            peerItem.setPeerServices(c.getLong(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.PeersColumns.PEER_TIMESTAMP);
        if (idColumn != -1) {
            peerItem.setPeerTimestamp(c.getInt(idColumn));
        }
        return peerItem;

    }

    public void recreate() {
        SQLiteDatabase db = mDb.getWritableDatabase();
        db.beginTransaction();
        db.execSQL("drop table " + AbstractDb.Tables.PEERS + ";");
        db.execSQL(AbstractDb.CREATE_PEER_SQL);
        db.setTransactionSuccessful();
        db.endTransaction();
    }
}
