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
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.core.Block;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.db.IBlockProvider;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockProvider implements IBlockProvider {

    private static BlockProvider blockProvider = new BlockProvider(BitherApplication.mDbHelper);

    public static BlockProvider getInstance() {
        return blockProvider;
    }

    private SQLiteOpenHelper mDb;


    private BlockProvider(SQLiteOpenHelper db) {
        this.mDb = db;
    }

    public List<Block> getAllBlocks() {
        List<Block> blockItems = new ArrayList<Block>();
        String sql = "select * from blocks order by block_no desc";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                blockItems.add(applyCursor(c));
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return blockItems;
    }

    @Override
    public List<Block> getLimitBlocks(int limit) {
        List<Block> blockItems = new ArrayList<Block>();
        String sql = "select * from blocks order by block_no desc limit " + limit;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                blockItems.add(applyCursor(c));
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return blockItems;
    }

    public List<Block> getBlocksFrom(int blockNo) {
        List<Block> blockItems = new ArrayList<Block>();
        String sql = "select * from blocks where block_no>" + Integer.toString(blockNo) + " order by block_no desc";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                blockItems.add(applyCursor(c));
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return blockItems;
    }

    public int getBlockCount() {
        String sql = "select count(*) cnt from blocks ";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        int count = 0;
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("cnt");
            if (idColumn != -1) {
                count = c.getInt(idColumn);
            }
        }
        c.close();
        return count;
    }

    public Block getLastBlock() {
        Block item = null;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from blocks where is_main=1 order by block_no desc limit 1";
        Cursor c = db.rawQuery(sql, null);
        try {
            if (c.moveToNext()) {
                item = applyCursor(c);
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return item;
    }

    public Block getLastOrphanBlock() {
        Block item = null;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from blocks where is_main=0 order by block_no desc limit 1";
        Cursor c = db.rawQuery(sql, null);
        try {
            if (c.moveToNext()) {
                item = applyCursor(c);
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return item;
    }

    public Block getBlock(byte[] blockHash) {
        Block item = null;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from blocks where block_hash='" + Base58.encode(blockHash) + "'";
        Cursor c = db.rawQuery(sql, null);
        try {
            if (c.moveToNext()) {
                item = applyCursor(c);
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return item;
    }

    public Block getOrphanBlockByPrevHash(byte[] prevHash) {
        Block item = null;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from blocks where block_prev=" + Base58.encode(prevHash) + " and is_main=0";
        Cursor c = db.rawQuery(sql, null);
        try {
            if (c.moveToNext()) {
                item = applyCursor(c);
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return item;
    }

    public Block getMainChainBlock(byte[] blockHash) {
        Block item = null;
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select * from blocks where block_hash= '" + Base58.encode(blockHash) + "' and is_main=1";
        Cursor c = db.rawQuery(sql, null);
        try {
            if (c.moveToNext()) {
                item = applyCursor(c);
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return item;
    }

    public List<byte[]> exists(List<byte[]> blockHashes) {
        List<byte[]> exists = new ArrayList<byte[]>();
        List<Block> blockItems = getAllBlocks();
        for (Block blockItm : blockItems) {
            for (byte[] bytes : exists) {
                if (Arrays.equals(bytes, blockItm.getBlockHash())) {
                    exists.add(bytes);
                    break;
                }
            }
        }
        return exists;
    }

    public boolean isExist(byte[] blockHash) {
        boolean result = false;
        String sql = "select count(0) cnt from blocks where block_hash='" + Base58.encode(blockHash) + "'";
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("cnt");
            result = c.getInt(idColumn) == 1;
        }
        c.close();
        return result;
    }

    public void addBlocks(List<Block> blockItemList) {
        List<Block> addBlockList = new ArrayList<Block>();
        List<Block> allBlockList = getAllBlocks();
        for (Block item : blockItemList) {
            if (!allBlockList.contains(item)) {
                addBlockList.add(item);
            }
        }
        allBlockList.clear();
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.beginTransaction();
        for (Block item : addBlockList) {
            ContentValues cv = new ContentValues();
            applyContentValues(item, cv);
            db.insert(AbstractDb.Tables.BLOCKS, null, cv);
        }
        db.setTransactionSuccessful();
        db.endTransaction();

    }

    public void addBlock(Block item) {
        boolean blockExists = blockExists(item.getBlockHash());
        if (!blockExists) {
            SQLiteDatabase db = this.mDb.getWritableDatabase();
            ContentValues cv = new ContentValues();
            applyContentValues(item, cv);
            db.insert(AbstractDb.Tables.BLOCKS, null, cv);
        }

    }

    public boolean blockExists(byte[] blockHash) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select count(0) cnt from blocks where block_hash='" + Base58.encode(blockHash) + "'";
        Cursor c = db.rawQuery(sql, null);
        int cnt = 0;
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("cnt");
            cnt = c.getInt(idColumn);
        }
        c.close();
        return cnt > 0;
    }

    public void updateBlock(byte[] blockHash, boolean isMain) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.BlocksColumns.IS_MAIN, isMain ? 1 : 0);
        db.update(AbstractDb.Tables.BLOCKS, cv, "block_hash=?", new String[]{Base58.encode(blockHash)});
    }

    public void removeBlock(byte[] blockHash) {
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.delete(AbstractDb.Tables.BLOCKS, "block_hash=?", new String[]{Base58.encode(blockHash)});
    }

    public void cleanOldBlock() {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        String sql = "select count(0) cnt from blocks";
        Cursor c = db.rawQuery(sql, null);
        int cnt = 0;
        if (c.moveToNext()) {
            int idColumn = c.getColumnIndex("cnt");
            if (idColumn != -1) {
                cnt = c.getInt(idColumn);
            }
        }
        c.close();
        if (cnt > 5000) {
            sql = "select max(block_no) max_block_no from blocks where is_main=1";
            c = db.rawQuery(sql, null);
            int maxBlockNo = 0;
            if (c.moveToNext()) {
                int idColumn = c.getColumnIndex("max_block_no");
                if (idColumn != -1) {
                    maxBlockNo = c.getInt(idColumn);

                }
            }
            c.close();
            int blockNo = (maxBlockNo - BitherjSettings.BLOCK_DIFFICULTY_INTERVAL) - maxBlockNo % BitherjSettings.BLOCK_DIFFICULTY_INTERVAL;
            db = this.mDb.getWritableDatabase();
            db.delete(AbstractDb.Tables.BLOCKS, "block_no<?", new String[]{Integer.toString(blockNo)});
        }

    }

    private void applyContentValues(Block item, ContentValues cv) {
        cv.put(AbstractDb.BlocksColumns.BLOCK_BITS, item.getBlockBits());
        cv.put(AbstractDb.BlocksColumns.BLOCK_HASH, Base58.encode(item.getBlockHash()));
        cv.put(AbstractDb.BlocksColumns.BLOCK_NO, item.getBlockNo());
        cv.put(AbstractDb.BlocksColumns.BLOCK_NONCE, item.getBlockNonce());
        cv.put(AbstractDb.BlocksColumns.BLOCK_PREV, Base58.encode(item.getBlockPrev()));
        cv.put(AbstractDb.BlocksColumns.BLOCK_ROOT, Base58.encode(item.getBlockRoot()));
        cv.put(AbstractDb.BlocksColumns.BLOCK_TIME, item.getBlockTime());
        cv.put(AbstractDb.BlocksColumns.BLOCK_VER, item.getBlockVer());
        cv.put(AbstractDb.BlocksColumns.IS_MAIN, item.isMain() ? 1 : 0);

    }

    private Block applyCursor(Cursor c) throws AddressFormatException {
        byte[] blockHash = null;
        long version = 1;
        byte[] prevBlock = null;
        byte[] merkleRoot = null;
        int timestamp = 0;
        long target = 0;
        long nonce = 0;
        int blockNo = 0;
        boolean isMain = false;
        int idColumn = c.getColumnIndex(AbstractDb.BlocksColumns.BLOCK_BITS);
        if (idColumn != -1) {
            target = c.getLong(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.BlocksColumns.BLOCK_HASH);
        if (idColumn != -1) {
            blockHash = Base58.decode(c.getString(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.BlocksColumns.BLOCK_NO);
        if (idColumn != -1) {
            blockNo = c.getInt(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.BlocksColumns.BLOCK_NONCE);
        if (idColumn != -1) {
            nonce = c.getLong(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.BlocksColumns.BLOCK_PREV);
        if (idColumn != -1) {
            prevBlock = Base58.decode(c.getString(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.BlocksColumns.BLOCK_ROOT);
        if (idColumn != -1) {
            merkleRoot = Base58.decode(c.getString(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.BlocksColumns.BLOCK_TIME);
        if (idColumn != -1) {
            timestamp = c.getInt(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.BlocksColumns.BLOCK_VER);
        if (idColumn != -1) {
            version = c.getLong(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.BlocksColumns.IS_MAIN);
        if (idColumn != -1) {
            isMain = c.getInt(idColumn) == 1;
        }
        return new Block(blockHash, version, prevBlock, merkleRoot, timestamp, target, nonce, blockNo, isMain);

    }
}
