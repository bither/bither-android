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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.utils.Utils;
import net.bither.ui.base.CurrencyTextView;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;


public class TransactionsOfBlockListAdapter extends BaseAdapter {

    public static final int MAX_NUM_CONFIRMATIONS = 7;
    private final Context context;
    private final LayoutInflater inflater;
    private final Address address;
    private final int maxConnectedPeers;

    private final List<Tx> transactions = new ArrayList<Tx>();
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

    public TransactionsOfBlockListAdapter(final Context context,
                                          @Nonnull final Address address, final int maxConnectedPeers,
                                          final boolean showBackupWarning) {
        this.context = context;
        inflater = LayoutInflater.from(context);

        this.address = address;
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

    public void replace(@Nonnull final Tx tx) {
        transactions.clear();
        transactions.add(tx);
        notifyDataSetChanged();
    }

    public void replace(@Nonnull final Collection<Tx> transactions) {
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
    public Tx getItem(final int position) {
        if (position == transactions.size() && showBackupWarning)
            return null;

        return transactions.get(position);
    }

    @Override
    public long getItemId(final int position) {
        if (position == transactions.size() && showBackupWarning)
            return 0;

        return Utils.longHash(transactions.get(position).getTxHash());
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

            final Tx tx = getItem(position);
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

    public void bindView(@Nonnull final View row, @Nonnull final Tx tx) {
        final Tx confidence = tx;

        final boolean isOwn = true;
        final boolean isCoinBase = tx.isCoinBase();
        final boolean isInternal = WalletUtils.isInternal(tx);

        final long value = tx.deltaAmountFrom(address);
        final boolean sent = value < 0;

        final TextView rowConfidenceCircular = (TextView) row
                .findViewById(R.id.transaction_row_confidence_circular);
        final TextView rowConfidenceTextual = (TextView) row
                .findViewById(R.id.transaction_row_confidence_textual);
        String formatString = "progress:%d,maxProgress:%d,peers:%d,maxSize%d";
        // confidence
        if (tx.getConfirmationCount() > 0) {
            rowConfidenceCircular.setVisibility(View.VISIBLE);
            rowConfidenceTextual.setVisibility(View.GONE);

            rowConfidenceCircular.setText(StringUtil.format(formatString, 1, 1,
                    tx.getSawByPeerCnt(), maxConnectedPeers / 2));

        } else if (tx.getConfirmationCount() == 0) {
            rowConfidenceCircular.setVisibility(View.VISIBLE);
            rowConfidenceTextual.setVisibility(View.GONE);
            rowConfidenceCircular.setText(StringUtil.format(
                    formatString,
                    tx.getConfirmationCount(),
                    isCoinBase, MAX_NUM_CONFIRMATIONS, 1, 1));

        } else {
            rowConfidenceCircular.setVisibility(View.GONE);
            rowConfidenceTextual.setVisibility(View.VISIBLE);

            rowConfidenceTextual.setText(CONFIDENCE_SYMBOL_UNKNOWN);
            rowConfidenceTextual.setTextColor(colorInsignificant);
        }

        // spendability
        final int textColor;

        textColor = colorInsignificant;


        final TextView rowTime = (TextView) row
                .findViewById(R.id.transaction_row_time);
        if (rowTime != null) {
            final Date time = new Date(tx.getTxTime() * 1000);
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
        final String label;
        if (isCoinBase)
            label = textCoinBase;
        else if (isInternal)
            label = textInternal;
        else if (address != null)
            label = resolveLabel(address.getAddress());
        else
            label = "?";
        rowAddress.setTextColor(textColor);
        rowAddress.setText(label != null ? label : address.getAddress());
        rowAddress.setTypeface(label != null ? Typeface.DEFAULT
                : Typeface.MONOSPACE);

        // value
        final CurrencyTextView rowValue = (CurrencyTextView) row
                .findViewById(R.id.transaction_row_value);
        rowValue.setTextColor(textColor);
        rowValue.setAlwaysSigned(true);
        rowValue.setPrecision(precision, shift);
        rowValue.setAmount(BigInteger.valueOf(value));

        // extended message
        final View rowExtend = row.findViewById(R.id.transaction_row_extend);
        if (rowExtend != null) {
            final TextView rowMessage = (TextView) row
                    .findViewById(R.id.transaction_row_message);
            final boolean isTimeLocked = tx.isTimeLocked();
            rowExtend.setVisibility(View.GONE);

            if (isOwn) {
                rowExtend.setVisibility(View.VISIBLE);
                rowMessage.setText("transaction_row_message_own_unbroadcasted");
                rowMessage.setTextColor(colorInsignificant);
            } else if (!isOwn && tx.getConfirmationCount() == 0) {
                rowExtend.setVisibility(View.VISIBLE);
                rowMessage.setText("transaction_row_message_received_direct");
                rowMessage.setTextColor(colorInsignificant);
            } else if (!sent
                    && value < 0) {
                rowExtend.setVisibility(View.VISIBLE);
                rowMessage.setText("transaction_row_message_received_dust");
                rowMessage.setTextColor(colorInsignificant);
            } else if (!sent && isTimeLocked) {
                rowExtend.setVisibility(View.VISIBLE);
                rowMessage
                        .setText("transaction_row_message_received_unconfirmed_locked");
                rowMessage.setTextColor(colorError);
            } else if (!sent && !isTimeLocked) {
                rowExtend.setVisibility(View.VISIBLE);
                rowMessage
                        .setText("transaction_row_message_received_unconfirmed_unlocked");
                rowMessage.setTextColor(colorInsignificant);
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
