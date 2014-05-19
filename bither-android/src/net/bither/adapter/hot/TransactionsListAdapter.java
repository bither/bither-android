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

package net.bither.adapter.hot;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.ui.base.CurrencyTextView;
import net.bither.util.ConfidenceUtil;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Transaction.Purpose;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.wallet.DefaultCoinSelector;

public class TransactionsListAdapter extends BaseAdapter {

	public static final int MAX_NUM_CONFIRMATIONS = 7;
	private final Context context;
	private final LayoutInflater inflater;
	private final Wallet wallet;
	private final int maxConnectedPeers;

	private final List<Transaction> transactions = new ArrayList<Transaction>();
	private int precision = 0;
	private int shift = 0;
	private boolean showEmptyText = false;
	private boolean showBackupWarning = false;

	private final int colorSignificant;
	private final int colorInsignificant;
	private final int colorError;

	private final String textCoinBase;
	private final String textInternal;

	private final Map<String, String> labelCache = new HashMap<String, String>();
	private final static String CACHE_NULL_MARKER = "";

	private static final String CONFIDENCE_SYMBOL_DEAD = "\u271D"; // latin
																	// cross
	private static final String CONFIDENCE_SYMBOL_UNKNOWN = "?";

	private static final int VIEW_TYPE_TRANSACTION = 0;
	private static final int VIEW_TYPE_WARNING = 1;

	public TransactionsListAdapter(final Context context,
			@Nonnull final Wallet wallet, final int maxConnectedPeers,
			final boolean showBackupWarning) {
		this.context = context;
		inflater = LayoutInflater.from(context);

		this.wallet = wallet;
		this.maxConnectedPeers = maxConnectedPeers;
		this.showBackupWarning = showBackupWarning;

		final Resources resources = context.getResources();
		colorSignificant = resources.getColor(R.color.fg_significant);
		colorInsignificant = resources.getColor(R.color.fg_insignificant);
		colorError = resources.getColor(R.color.fg_error);
		textCoinBase = "mined";
		textInternal = "internal";
	}

	public void setPrecision(final int precision, final int shift) {
		this.precision = precision;
		this.shift = shift;

		notifyDataSetChanged();
	}

	public void clear() {
		transactions.clear();

		notifyDataSetChanged();
	}

	public void replace(@Nonnull final Transaction tx) {
		transactions.clear();
		transactions.add(tx);

		notifyDataSetChanged();
	}

	public void replace(@Nonnull final Collection<Transaction> transactions) {
		this.transactions.clear();
		this.transactions.addAll(transactions);

		showEmptyText = true;

		notifyDataSetChanged();
	}

	@Override
	public boolean isEmpty() {
		return showEmptyText && super.isEmpty();
	}

	@Override
	public int getCount() {
		int count = transactions.size();

		if (count == 1 && showBackupWarning)
			count++;

		return count;
	}

	@Override
	public Transaction getItem(final int position) {
		if (position == transactions.size() && showBackupWarning)
			return null;

		return transactions.get(position);
	}

	@Override
	public long getItemId(final int position) {
		if (position == transactions.size() && showBackupWarning)
			return 0;

		return WalletUtils.longHash(transactions.get(position).getHash());
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(final int position) {
		if (position == transactions.size() && showBackupWarning)
			return VIEW_TYPE_WARNING;
		else
			return VIEW_TYPE_TRANSACTION;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getView(final int position, View row, final ViewGroup parent) {
		final int type = getItemViewType(position);

		if (type == VIEW_TYPE_TRANSACTION) {
			if (row == null)
				row = inflater.inflate(R.layout.transaction_row_extended, null);

			final Transaction tx = getItem(position);
			bindView(row, tx);
		} else if (type == VIEW_TYPE_WARNING) {
			if (row == null)
				row = inflater.inflate(R.layout.transaction_row_warning, null);

			final TextView messageView = (TextView) row
					.findViewById(R.id.transaction_row_warning_message);
			messageView
					.setText(Html
							.fromHtml("Congratulations, you received your first payment! Have you already &lt;u&gt;backed up your wallet&lt;/u&gt;, to protect against loss?"));
		} else {
			throw new IllegalStateException("unknown type: " + type);
		}

		return row;
	}

	public void bindView(@Nonnull final View row, @Nonnull final Transaction tx) {
		final TransactionConfidence confidence = tx.getConfidence();
		final ConfidenceType confidenceType = confidence.getConfidenceType();
		final boolean isOwn = confidence.getSource().equals(
				TransactionConfidence.Source.SELF);
		final boolean isCoinBase = tx.isCoinBase();
		final boolean isInternal = WalletUtils.isInternal(tx);

		final BigInteger value = tx.getValue(wallet);
		final boolean sent = value.signum() < 0;

		final TextView rowConfidenceCircular = (TextView) row
				.findViewById(R.id.transaction_row_confidence_circular);
		final TextView rowConfidenceTextual = (TextView) row
				.findViewById(R.id.transaction_row_confidence_textual);
		String formatString = "progress:%d,maxProgress:%d,peers:%d,maxSize%d";
		// confidence
		if (confidenceType == ConfidenceType.PENDING) {
			rowConfidenceCircular.setVisibility(View.VISIBLE);
			rowConfidenceTextual.setVisibility(View.GONE);

			rowConfidenceCircular.setText(StringUtil.format(formatString, 1, 1,
					confidence.numBroadcastPeers(), maxConnectedPeers / 2));

		} else if (confidenceType == ConfidenceType.BUILDING) {
			rowConfidenceCircular.setVisibility(View.VISIBLE);
			rowConfidenceTextual.setVisibility(View.GONE);
			rowConfidenceCircular.setText(StringUtil.format(
					formatString,
					ConfidenceUtil.getDepthInChain(confidence),
					isCoinBase ? BitherSetting.NETWORK_PARAMETERS
							.getSpendableCoinbaseDepth()
							: MAX_NUM_CONFIRMATIONS, 1, 1));

		} else if (confidenceType == ConfidenceType.DEAD) {
			rowConfidenceCircular.setVisibility(View.GONE);
			rowConfidenceTextual.setVisibility(View.VISIBLE);

			rowConfidenceTextual.setText(CONFIDENCE_SYMBOL_DEAD);
			rowConfidenceTextual.setTextColor(Color.RED);
		} else {
			rowConfidenceCircular.setVisibility(View.GONE);
			rowConfidenceTextual.setVisibility(View.VISIBLE);

			rowConfidenceTextual.setText(CONFIDENCE_SYMBOL_UNKNOWN);
			rowConfidenceTextual.setTextColor(colorInsignificant);
		}

		// spendability
		final int textColor;
		if (confidenceType == ConfidenceType.DEAD)
			textColor = Color.RED;
		else
			textColor = DefaultCoinSelector.isSelectable(tx) ? colorSignificant
					: colorInsignificant;

		// time
		final TextView rowTime = (TextView) row
				.findViewById(R.id.transaction_row_time);
		if (rowTime != null) {
			final Date time = tx.getUpdateTime();
			rowTime.setText(time != null ? (DateUtils
					.getRelativeTimeSpanString(context, time.getTime())) : null);
			rowTime.setTextColor(textColor);
		}

		// receiving or sending
		final TextView rowFromTo = (TextView) row
				.findViewById(R.id.transaction_row_fromto);
		if (isInternal)
			rowFromTo.setText("symbol_internal");
		else if (sent)
			rowFromTo.setText("symbol_to");
		else
			rowFromTo.setText("symbol_from");
		rowFromTo.setTextColor(textColor);

		// coinbase
		final View rowCoinbase = row
				.findViewById(R.id.transaction_row_coinbase);
		rowCoinbase.setVisibility(isCoinBase ? View.VISIBLE : View.GONE);

		// address
		final TextView rowAddress = (TextView) row
				.findViewById(R.id.transaction_row_address);
		final Address address = sent ? WalletUtils.getFirstToAddress(tx)
				: WalletUtils.getFirstFromAddress(tx);
		final String label;
		if (isCoinBase)
			label = textCoinBase;
		else if (isInternal)
			label = textInternal;
		else if (address != null)
			label = resolveLabel(address.toString());
		else
			label = "?";
		rowAddress.setTextColor(textColor);
		rowAddress.setText(label != null ? label : address.toString());
		rowAddress.setTypeface(label != null ? Typeface.DEFAULT
				: Typeface.MONOSPACE);

		// value
		final CurrencyTextView rowValue = (CurrencyTextView) row
				.findViewById(R.id.transaction_row_value);
		rowValue.setTextColor(textColor);
		rowValue.setAlwaysSigned(true);
		rowValue.setPrecision(precision, shift);
		rowValue.setAmount(value);

		// extended message
		final View rowExtend = row.findViewById(R.id.transaction_row_extend);
		if (rowExtend != null) {
			final TextView rowMessage = (TextView) row
					.findViewById(R.id.transaction_row_message);
			final boolean isTimeLocked = tx.isTimeLocked();
			rowExtend.setVisibility(View.GONE);

			if (tx.getPurpose() == Purpose.KEY_ROTATION) {
				rowExtend.setVisibility(View.VISIBLE);
				rowMessage
						.setText(Html
								.fromHtml("This transaction strengthens your wallet against theft. &lt;u&gt;More info.&lt;/u&gt;"));
				rowMessage.setTextColor(colorSignificant);
			} else if (isOwn && confidenceType == ConfidenceType.PENDING
					&& confidence.numBroadcastPeers() == 0) {
				rowExtend.setVisibility(View.VISIBLE);
				rowMessage.setText("transaction_row_message_own_unbroadcasted");
				rowMessage.setTextColor(colorInsignificant);
			} else if (!isOwn && confidenceType == ConfidenceType.PENDING
					&& confidence.numBroadcastPeers() == 0) {
				rowExtend.setVisibility(View.VISIBLE);
				rowMessage.setText("transaction_row_message_received_direct");
				rowMessage.setTextColor(colorInsignificant);
			} else if (!sent
					&& value.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0) {
				rowExtend.setVisibility(View.VISIBLE);
				rowMessage.setText("transaction_row_message_received_dust");
				rowMessage.setTextColor(colorInsignificant);
			} else if (!sent && confidenceType == ConfidenceType.PENDING
					&& isTimeLocked) {
				rowExtend.setVisibility(View.VISIBLE);
				rowMessage
						.setText("transaction_row_message_received_unconfirmed_locked");
				rowMessage.setTextColor(colorError);
			} else if (!sent && confidenceType == ConfidenceType.PENDING
					&& !isTimeLocked) {
				rowExtend.setVisibility(View.VISIBLE);
				rowMessage
						.setText("transaction_row_message_received_unconfirmed_unlocked");
				rowMessage.setTextColor(colorInsignificant);
			} else if (!sent && confidenceType == ConfidenceType.DEAD) {
				rowExtend.setVisibility(View.VISIBLE);
				rowMessage.setText("transaction_row_message_received_dead");
				rowMessage.setTextColor(colorError);
			}
		}
	}

	private String resolveLabel(@Nonnull final String address) {
		final String cachedLabel = labelCache.get(address);
		if (cachedLabel == null) {
			final String label = "";
			labelCache.put(address, label);
			return label;
		} else {
			return cachedLabel != CACHE_NULL_MARKER ? cachedLabel : null;
		}
	}

	public void clearLabelCache() {
		labelCache.clear();

		notifyDataSetChanged();
	}
}
