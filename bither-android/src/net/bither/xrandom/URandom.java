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

package net.bither.xrandom;

import net.bither.bitherj.crypto.IRandom;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class URandom implements IRandom {

    public static File urandomFile = new File("/dev/urandom");

    public synchronized byte[] nextBytes(int length) {
        byte[] bytes = new byte[length];
        if (!urandomFile.exists()) {
            throw new RuntimeException("Unable to generate URandom bytes on this Android device");
        }
        try {
            FileInputStream stream = new FileInputStream(urandomFile);
            DataInputStream dis = new DataInputStream(stream);
            dis.readFully(bytes);
            dis.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to generate URandom bytes on this Android device", e);
        }
        return bytes;
    }

}
