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

import java.io.Serializable;

import net.bither.R;
import net.bither.util.ConfidenceUtil;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.TransactionConfidence.Listener;

public class TransactionConfidenceButton extends Button {

	private TransactionConfidence confidence;
	private ConfidenceListener listener = new ConfidenceListener();

	public TransactionConfidenceButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TransactionConfidenceButton(Context context) {
		super(context);
	}

	public TransactionConfidenceButton(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setConfidence(TransactionConfidence confidence) {
		if (this.confidence != null) {
			this.confidence.removeEventListener(listener);
		}
		this.confidence = confidence;
		if (confidence != null) {
			this.confidence.addEventListener(listener);
		}
		showConfidence();
	}

	public TransactionConfidence getConfidence() {
		return confidence;
	}

	public void onPause() {
		if (confidence != null) {
			this.confidence.removeEventListener(listener);
		}
	}

	public void onResume() {
		if (confidence != null) {
			this.confidence.addEventListener(listener);
		}
		showConfidence();
	}

	private void showConfidence() {
		if (confidence == null) {
			return;
		}
		if (confidence.getConfidenceType() != ConfidenceType.BUILDING) {
			setBackgroundResource(R.drawable.btn_small_red_selector);
			setText(R.string.unconfirmed);
		} else {
			int depth = ConfidenceUtil.getDepthInChain(confidence);
			if (depth >= 6) {
				setBackgroundResource(R.drawable.btn_small_blue_selector);
			} else {
				setBackgroundResource(R.drawable.btn_small_red_selector);
			}
			if (depth <= 0) {
				setText(R.string.unconfirmed);
			} else if (depth == 1) {
				setText("1" + " "
						+ getContext().getString(R.string.confirmation_postfix));
			} else {
				setText(Integer.toString(depth)
						+ " "
						+ getContext()
								.getString(R.string.confirmations_postfix));

			}
		}
	}

	private class ConfidenceListener implements Listener, Serializable {

		@Override
		public void onConfidenceChanged(Transaction tx, ChangeReason reason) {
			confidence = tx.getConfidence();
			post(new Runnable() {
				@Override
				public void run() {
					showConfidence();
				}
			});
		}
	}

}
