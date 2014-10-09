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

package net.bither.model;

import net.bither.bitherj.core.Tx;
import net.bither.bitherj.utils.Utils;

import java.io.Serializable;

public class UnSignTransaction implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public UnSignTransaction(Tx tx, String address) {
        this.mTx = tx;
        this.mAddress = address;
    }

    private Tx mTx;

    private String mAddress;

    public Tx getTx() {
        return mTx;
    }

    public void setTx(Tx mTx) {
        this.mTx = mTx;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String mAddress) {
        this.mAddress = mAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof UnSignTransaction) {
            UnSignTransaction unSignTransaction = (UnSignTransaction) o;
            return Utils.compareString(getAddress(),
                    unSignTransaction.getAddress());
        }
        return super.equals(o);
    }

}
