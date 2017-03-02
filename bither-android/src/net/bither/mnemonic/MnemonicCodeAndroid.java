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

package net.bither.mnemonic;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.bitherj.crypto.mnemonic.MnemonicCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 14/12/31.
 */
public class MnemonicCodeAndroid extends MnemonicCode {
    private String word;

    public MnemonicCodeAndroid(String word) throws IOException {
        super();
        if (word != null) {
            this.word = word;
            this.wordList = openWordList();
        }
    }

    @Override
    protected ArrayList<String> openWordList() throws IOException, IllegalArgumentException {
        if (word == null) {
            return getWordListForInputStream(BitherApplication.mContext.getResources().openRawResource(R.raw.mnemonic_wordlist_english));
        }
        InputStream[] inputStreams = new InputStream[]{
                BitherApplication.mContext.getResources().openRawResource(R.raw.mnemonic_wordlist_english),
                BitherApplication.mContext.getResources().openRawResource(R.raw.mnemonic_wordlist_zh_cn),
                BitherApplication.mContext.getResources().openRawResource(R.raw.mnemonic_wordlist_zh_tw)};
        for (InputStream inputStream: inputStreams) {
            ArrayList<String> words = getWordListForInputStream(inputStream);
            if (words.contains(word)) {
                return words;
            }
        }
        return null;
    }

}
