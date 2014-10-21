/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.runnable;

import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.Tx;

import java.util.Random;

/**
 * Created by songchenwen on 14-10-20.
 */
public class RCheckRunnable extends BaseRunnable {
    private Address address;
    private Tx tx;

    public RCheckRunnable(Address address, Tx tx) {
        this.address = address;
        this.tx = tx;
    }

    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
            obtainMessage(HandlerMessage.MSG_FAILURE);
        }
        if (new Random().nextInt() % 2 == 0) {
            obtainMessage(HandlerMessage.MSG_SUCCESS, tx);
        } else {
            obtainMessage(HandlerMessage.MSG_FAILURE);
        }
    }
}
