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
import net.bither.bitherj.core.AbstractHD;
import net.bither.bitherj.core.DesktopHDMAddress;
import net.bither.bitherj.core.DesktopHDMKeychain;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.In;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.db.IDesktopTxProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by nn on 15/6/17.
 */
public class DesktopTxProvider implements IDesktopTxProvider {

    private static DesktopTxProvider txProvider = new DesktopTxProvider(BitherApplication
            .mAddressDbHelper);

    public static DesktopTxProvider getInstance() {
        return txProvider;
    }

    private SQLiteOpenHelper mDb;


    private DesktopTxProvider(SQLiteOpenHelper db) {
        this.mDb = db;
    }

    @Override
    public void addAddress(List<DesktopHDMAddress> address) {

    }

    @Override
    public int maxHDMAddressPubIndex() {
        return 0;
    }

    @Override
    public String externalAddress() {
        return null;
    }

    @Override
    public boolean hasAddress() {
        return false;
    }

    @Override
    public long getHDAccountConfirmedBanlance(int hdSeedId) {
        return 0;
    }

    @Override
    public HashSet<String> getBelongAccountAddresses(List<String> addressList) {
        return new HashSet<String>();
    }

    @Override
    public void updateIssuedIndex(AbstractHD.PathType pathType, int index) {

    }

    @Override
    public int issuedIndex(AbstractHD.PathType pathType) {
        return 0;
    }

    @Override
    public int allGeneratedAddressCount(AbstractHD.PathType pathType) {
        return 0;
    }

    @Override
    public void updateSyncdForIndex(AbstractHD.PathType pathType, int index) {

    }

    @Override
    public void updateSyncdComplete(DesktopHDMAddress address) {

    }

    @Override
    public List<Tx> getHDAccountUnconfirmedTx() {
        return new ArrayList<Tx>();
    }

    @Override
    public List<HDMAddress.Pubs> getPubs(AbstractHD.PathType pathType) {
        return new ArrayList<HDMAddress.Pubs>();
    }

    @Override
    public int getUnspendOutCountByHDAccountWithPath(int hdAccountId, AbstractHD.PathType
            pathType) {
        return 0;
    }

    @Override
    public List<Out> getUnspendOutByHDAccountWithPath(int hdAccountId, AbstractHD.PathType
            pathType) {
        return new ArrayList<Out>();
    }

    @Override
    public DesktopHDMAddress addressForPath(DesktopHDMKeychain keychain, AbstractHD.PathType
            type, int index) {
        return null;
    }

    @Override
    public List<DesktopHDMAddress> getSigningAddressesForInputs(DesktopHDMKeychain keychain,
                                                                List<In> inList) {
        return new ArrayList<DesktopHDMAddress>();
    }

    @Override
    public List<DesktopHDMAddress> belongAccount(DesktopHDMKeychain keychain, List<String>
            addresses) {
        return new ArrayList<DesktopHDMAddress>();
    }

    @Override
    public List<Out> getUnspendOutByHDAccount(int hdAccountId) {
        return new ArrayList<Out>();
    }

    @Override
    public int unSyncedAddressCount() {
        return 0;
    }
}
