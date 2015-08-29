///*
// * Copyright 2014 http://Bither.net
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package net.bither.db;
//
//import android.database.sqlite.SQLiteOpenHelper;
//
//import net.bither.BitherApplication;
//import net.bither.bitherj.db.IDesktopAddressProvider;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Created by nn on 15/6/17.
// */
//public class DesktopAddressProvider implements IDesktopAddressProvider {
//
//    private static DesktopAddressProvider desktopAddressProvider = new DesktopAddressProvider(BitherApplication.mAddressDbHelper);
//
//    public static DesktopAddressProvider getInstance() {
//        return desktopAddressProvider;
//    }
//
//    private SQLiteOpenHelper mDb;
//
//
//    private DesktopAddressProvider(SQLiteOpenHelper db) {
//        this.mDb = db;
//    }
//
//    @Override
//    public int addHDKey(String encryptedMnemonicSeed, String encryptHdSeed, String firstAddress, boolean isXrandom, String addressOfPS, byte[] externalPub, byte[] internalPub) {
//        return 0;
//    }
//
//    @Override
//    public void addHDMPub(List<byte[]> externalPubs, List<byte[]> internalPubs) {
//
//    }
//
//    @Override
//    public List<byte[]> getExternalPubs() {
//        return new ArrayList<byte[]>();
//    }
//
//    @Override
//    public List<byte[]> getInternalPubs() {
//        return new ArrayList<byte[]>();
//    }
//
//    @Override
//    public boolean isHDSeedFromXRandom(int hdSeedId) {
//        return false;
//    }
//
//    @Override
//    public String getEncryptMnemonicSeed(int hdSeedId) {
//        return null;
//    }
//
//    @Override
//    public String getEncryptHDSeed(int hdSeedId) {
//        return null;
//    }
//
//    @Override
//    public String getHDMFristAddress(int hdSeedId) {
//        return null;
//    }
//
//    @Override
//    public List<Integer> getDesktopKeyChainSeed() {
//        return new ArrayList<Integer>();
//    }
//}
