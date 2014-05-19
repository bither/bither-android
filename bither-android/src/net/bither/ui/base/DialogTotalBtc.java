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

package net.bither.ui.base;

import java.math.BigInteger;

import net.bither.R;
import net.bither.util.GenericUtils;
import android.content.Context;
import android.widget.TextView;

public class DialogTotalBtc extends DialogWithArrow {

	private TextView tvBtc;

	public DialogTotalBtc(Context context) {
		super(context);
		setContentView(R.layout.dialog_total_btc);
		tvBtc = (TextView) findViewById(R.id.tv_btc);
	}

	public void setBigInteger(BigInteger btc) {

		if (btc != null) {
			tvBtc.setText(GenericUtils.formatValue(btc));
		}
	}

}
