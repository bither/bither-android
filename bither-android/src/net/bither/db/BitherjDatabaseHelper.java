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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BitherjDatabaseHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;
    private static final String DB_NAME = "bitherj.db";

    public BitherjDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createBlocksTable(db);
        createTxsTable(db);
        createAddressTxsTable(db);
        createInsTable(db);
        createOutsTable(db);
        createPeersTable(db);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public interface Tables {
        public static final String BLOCKS = "blocks";
        public static final String TXS = "txs";
        public static final String ADDRESSES_TXS = "addresses_txs";
        public static final String INS = "ins";
        public static final String OUTS = "outs";
        public static final String PEERS = "peers";
    }

    public interface BlocksColumns {
        public static final String BLOCK_NO = "block_no";
        public static final String BLOCK_HASH = "block_hash";
        public static final String BLOCK_ROOT = "block_root";
        public static final String BLOCK_VER = "block_ver";
        public static final String BLOCK_BITS = "block_bits";
        public static final String BLOCK_NONCE = "block_nonce";
        public static final String BLOCK_TIME = "block_time";
        public static final String BLOCK_PREV = "block_prev";
        public static final String IS_MAIN = "is_main";
    }

    public interface TxsColumns {
        public static final String TX_HASH = "tx_hash";
        public static final String TX_VER = "tx_ver";
        public static final String TX_LOCKTIME = "tx_locktime";
        public static final String TX_TIME = "tx_time";
        public static final String BLOCK_NO = "block_no";
        public static final String SOURCE = "source";
    }

    public interface AddressesTxsColumns {
        public static final String ADDRESS = "address";
        public static final String TX_HASH = "tx_hash";
    }

    public interface InsColumns {
        public static final String TX_HASH = "tx_hash";
        public static final String IN_SN = "in_sn";
        public static final String PREV_TX_HASH = "prev_tx_hash";
        public static final String PREV_OUT_SN = "prev_out_sn";
        public static final String IN_SIGNATURE = "in_signature";
        public static final String IN_SEQUENCE = "in_sequence";
    }

    public interface OutsColumns {
        public static final String TX_HASH = "tx_hash";
        public static final String OUT_SN = "out_sn";
        public static final String OUT_SCRIPT = "out_script";
        public static final String OUT_VALUE = "out_value";
        public static final String OUT_STATUS = "out_status";
        public static final String OUT_ADDRESS = "out_address";
    }

    public interface PeersColumns {
        public static final String PEER_ADDRESS = "peer_address";
        public static final String PEER_PORT = "peer_port";
        public static final String PEER_SERVICES = "peer_services";
        public static final String PEER_TIMESTAMP = "peer_timestamp";
        public static final String PEER_CONNECTED_CNT = "peer_connected_cnt";

    }


    private void createBlocksTable(SQLiteDatabase db) {
        String sql = "create table if not exists blocks " +
                "(block_no integer not null" +
                ", block_hash text not null primary key" +
                ", block_root text not null" +
                ", block_ver integer not null" +
                ", block_bits integer not null" +
                ", block_nonce integer not null" +
                ", block_time integer not null" +
                ", block_prev text" +
                ", is_main integer not null);";
        db.execSQL(sql);
        db.execSQL("create index idx_blocks_block_no on blocks (block_no);");
        db.execSQL("create index idx_blocks_block_prev on blocks (block_prev);");

    }

    private void createTxsTable(SQLiteDatabase db) {
        String sql = "create table if not exists txs " +
                "(tx_hash text primary key" +
                ", tx_ver integer" +
                ", tx_locktime integer" +
                ", tx_time integer" +
                ", block_no integer" +
                ", source integer);";
        db.execSQL(sql);

    }

    private void createAddressTxsTable(SQLiteDatabase db) {
        String sql = "create table if not exists addresses_txs " +
                "(address text not null" +
                ", tx_hash text not null" +
                ", primary key (address, tx_hash));";
        db.execSQL(sql);

    }

    private void createInsTable(SQLiteDatabase db) {
        String sql = "create table if not exists ins " +
                "(tx_hash text not null" +
                ", in_sn integer not null" +
                ", prev_tx_hash text" +
                ", prev_out_sn integer" +
                ", in_signature text" +
                ", in_sequence integer" +
                ", primary key (tx_hash, in_sn));";
        db.execSQL(sql);
    }

    private void createOutsTable(SQLiteDatabase db) {
        String sql = "create table if not exists outs " +
                "(tx_hash text not null" +
                ", out_sn integer not null" +
                ", out_script text not null" +
                ", out_value integer not null" +
                ", out_status integer not null" +
                ", out_address text" +
                ", primary key (tx_hash, out_sn));";
        db.execSQL(sql);

    }

    private void createPeersTable(SQLiteDatabase db) {
        String sql = "create table if not exists peers " +
                "(peer_address integer primary key" +
                ", peer_port integer not null" +
                ", peer_services integer not null" +
                ", peer_timestamp integer not null" +
                ", peer_connected_cnt integer not null);";
        db.execSQL(sql);
    }
}
