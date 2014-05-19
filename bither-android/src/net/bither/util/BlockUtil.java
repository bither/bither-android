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

package net.bither.util;

import java.math.BigInteger;
import java.util.ArrayList;

import net.bither.BitherSetting;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;

public class BlockUtil {

	private static final String VER = "ver";
	private static final String PREV_BLOCK = "prev_block";
	private static final String MRKL_ROOT = "mrkl_root";
	private static final String TIME = "time";
	private static final String BITS = "bits";
	private static final String NONCE = "nonce";
	private static final String BLOCK_NO = "block_no";

	public static StoredBlock formatStoredBlock(JSONObject jsonObject)
			throws JSONException {
		long ver = jsonObject.getLong(VER);
		int height = jsonObject.getInt(BLOCK_NO);
		String prevBlock = jsonObject.getString(PREV_BLOCK);
		String mrklRoot = jsonObject.getString(MRKL_ROOT);
		long time = jsonObject.getLong(TIME);
		long difficultyTarget = jsonObject.getLong(BITS);
		long nonce = jsonObject.getLong(NONCE);
		LogUtil.d("http", jsonObject.toString());
		return BlockUtil.getStoredBlock(ver, prevBlock, mrklRoot, time,
				difficultyTarget, nonce, height);
	}

	public static StoredBlock formatStoredBlock(JSONObject jsonObject, int hegih)
			throws JSONException {

		long ver = jsonObject.getLong(VER);
		String prevBlock = jsonObject.getString(PREV_BLOCK);
		String mrklRoot = jsonObject.getString(MRKL_ROOT);
		long time = jsonObject.getLong(TIME);
		long difficultyTarget = jsonObject.getLong(BITS);
		long nonce = jsonObject.getLong(NONCE);
		LogUtil.d("http", jsonObject.toString());
		return BlockUtil.getStoredBlock(ver, prevBlock, mrklRoot, time,
				difficultyTarget, nonce, hegih);

	}

	public static StoredBlock getStoredBlock(long ver, String prevBlock,
			String mrklRoot, long time, long difficultyTarget, long nonce,
			int hegiht) {
		Block b = new Block(BitherSetting.NETWORK_PARAMETERS, ver,
				new Sha256Hash(prevBlock), new Sha256Hash(mrklRoot), time,
				difficultyTarget, nonce, new ArrayList<Transaction>());
		StoredBlock storedBlock = new StoredBlock(b, BigInteger.valueOf(nonce),
				hegiht);
		return storedBlock;
	}

}
