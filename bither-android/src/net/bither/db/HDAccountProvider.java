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

import android.database.sqlite.SQLiteOpenHelper;

import net.bither.BitherApplication;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.db.IHDAccountProvider;

import java.util.HashMap;
import java.util.List;

public class HDAccountProvider implements IHDAccountProvider {
    private static HDAccountProvider hdAccountProvider = new HDAccountProvider(BitherApplication.mAddressDbHelper, BitherApplication.mHDDbHelper);

    public static HDAccountProvider getInstance() {
        return hdAccountProvider;
    }

    private SQLiteOpenHelper addressDB;
    private SQLiteOpenHelper hdAccountDB;


    private HDAccountProvider(SQLiteOpenHelper addressDB, SQLiteOpenHelper hdAccountDB) {
        this.addressDB = addressDB;
        this.hdAccountDB = hdAccountDB;
    }

    @Override
    public int addHDKey(String encryptSeed, String encryptHdSeed, String firstAddress, boolean isXrandom, String addressOfPS, byte[] externalPub, byte[] internalPub) {
        return 0;
    }

    @Override
    public void addExternalAddress(List<HDAccount.HDAccountAddress> hdAccountAddresses) {

    }

    @Override
    public void addInternalAddress(List<HDAccount.HDAccountAddress> hdAccountAddresses) {

    }

    @Override
    public int externalIssuedIndex() {
        return 0;
    }

    @Override
    public int internalIssuedIndex() {
        return 0;
    }

    @Override
    public byte[] getExternalPub() {
        return new byte[0];
    }

    @Override
    public byte[] getInternalPub() {
        return new byte[0];
    }

    @Override
    public String externalAddress() {
        return null;
    }

    @Override
    public List<HashMap<String, byte[]>> getAddressPub() {
        return null;
    }

    @Override
    public List<Tx> getUnspentTxs() {
        return null;
    }

    @Override
    public void addTx(Tx tx) {

    }

    @Override
    public void addTxs(List<Tx> txList) {

    }

    @Override
    public String getEncryptSeed(int hdSeedId) {
        return null;
    }

    @Override
    public String getEncryptHDSeed(int hdSeedId) {
        return null;
    }
}
