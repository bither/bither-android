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

package net.bither.activity.hot;

import java.math.BigInteger;

import net.bither.BitherSetting;
import net.bither.BitherSetting.MarketType;
import net.bither.R;
import net.bither.ScanActivity;
import net.bither.model.BitherAddress;
import net.bither.model.QRCodeTxTransport;
import net.bither.model.Ticker;
import net.bither.model.UnSignTransaction;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.CommitTransactionRunnable;
import net.bither.runnable.CompleteTransactionRunnable;
import net.bither.runnable.HandlerMessage;
import net.bither.ui.base.CurrencyAmountView;
import net.bither.ui.base.CurrencyCalculatorLink;
import net.bither.ui.base.DialogProgress;
import net.bither.ui.base.DialogSendConfirm;
import net.bither.ui.base.DialogSendConfirm.SendConfirmListener;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.listener.BackClickListener;
import net.bither.util.BroadcastUtil;
import net.bither.util.CurrencySymbolUtil;
import net.bither.util.GenericUtils;
import net.bither.util.InputParser.StringInputParser;
import net.bither.util.MarketUtil;
import net.bither.util.StringUtil;
import net.bither.util.TransactionsUtil;
import net.bither.util.WalletUtils;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.core.Wallet.SendRequest;

public class GenerateUnsignedTxActivity extends SwipeRightActivity {
	private static final String ADDRESS_POSITION_SAVE_KEY = "address_position";

	private int addressPosition;
	private BitherAddress address;
	private TextView tvAddressLabel;
	private EditText etAddress;
	private ImageButton ibtnScan;
	private CurrencyCalculatorLink amountCalculatorLink;
	private Button btnSend;
	private DialogProgress dp;
	private TextView tvBalance;
	private ImageView ivBalanceSymbol;

	private Transaction tx;

	private boolean isDonate = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		overridePendingTransition(R.anim.slide_in_right, 0);
		setContentView(R.layout.activity_generate_unsigned_tx);
		if (getIntent().getExtras().containsKey(
				BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG)) {
			addressPosition = getIntent().getExtras().getInt(
					BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG);
			if (addressPosition >= 0
					&& addressPosition < WalletUtils.getWatchOnlyAddressList()
							.size()) {
				address = WalletUtils.getWatchOnlyAddressList().get(
						addressPosition);
			}
		}
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(ADDRESS_POSITION_SAVE_KEY)) {
			addressPosition = savedInstanceState
					.getInt(ADDRESS_POSITION_SAVE_KEY);
			if (addressPosition >= 0
					&& addressPosition < WalletUtils.getWatchOnlyAddressList()
							.size()) {
				address = WalletUtils.getWatchOnlyAddressList().get(
						addressPosition);
			}
			if (address != null) {
				UnSignTransaction utx = TransactionsUtil
						.getUnsignTxFromCache(address.getAddress());
				if (utx != null) {
					tx = utx.getTx();
				}
			}
		}
		if (address == null) {
			finish();
			return;
		}
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(ADDRESS_POSITION_SAVE_KEY)) {
			tx = (Transaction) savedInstanceState
					.getSerializable(ADDRESS_POSITION_SAVE_KEY);
		}
		initView();
		processIntent();
		configureDonate();
	}

	private void initView() {
		findViewById(R.id.btn_cancel).setOnClickListener(
				new BackClickListener());
		tvAddressLabel = (TextView) findViewById(R.id.tv_address_label);
		etAddress = (EditText) findViewById(R.id.et_address);
		ibtnScan = (ImageButton) findViewById(R.id.ibtn_scan);
		btnSend = (Button) findViewById(R.id.btn_send);
		tvBalance = (TextView) findViewById(R.id.tv_balance);
		ivBalanceSymbol = (ImageView) findViewById(R.id.iv_balance_symbol);
		tvBalance.setText(GenericUtils.formatValue(address.getAddressInfo()
				.getBalance()));
		ivBalanceSymbol.setImageBitmap(CurrencySymbolUtil
				.getBtcSymbol(tvBalance));
		final CurrencyAmountView btcAmountView = (CurrencyAmountView) findViewById(R.id.cav_btc);
		btcAmountView.setCurrencySymbol(getString(R.string.bitcoin_symbol));
		btcAmountView.setInputPrecision(8);
		btcAmountView.setHintPrecision(4);
		btcAmountView.setShift(0);

		final CurrencyAmountView localAmountView = (CurrencyAmountView) findViewById(R.id.cav_local);
		localAmountView.setInputPrecision(2);
		localAmountView.setHintPrecision(2);
		amountCalculatorLink = new CurrencyCalculatorLink(btcAmountView,
				localAmountView);
		ReceivingAddressListener addressListener = new ReceivingAddressListener();
		etAddress.setOnFocusChangeListener(addressListener);
		etAddress.addTextChangedListener(addressListener);
		dp = new DialogProgress(this, R.string.please_wait);
		ibtnScan.setOnClickListener(scanClick);
		btnSend.setOnClickListener(sendClick);
	}

	private OnClickListener scanClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(GenerateUnsignedTxActivity.this,
					ScanActivity.class);
			startActivityForResult(intent,
					BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
		}
	};

	private SendConfirmListener sendConfirmListener = new SendConfirmListener() {

		@Override
		public void onConfirm(SendRequest request) {
			GenerateUnsignedTxActivity.this.tx = request.tx;
			Intent intent = new Intent(GenerateUnsignedTxActivity.this,
					UnsignedTxQrCodeActivity.class);
			intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING,
					QRCodeTxTransport.getPreSignString(QRCodeTxTransport
							.fromSendRequestWithUnsignedTransaction(request,
									address.getAddress())));
			intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
					getString(R.string.unsigned_transaction_qr_code_title));
			startActivityForResult(intent,
					BitherSetting.INTENT_REF.SIGN_TX_REQUEST_CODE);
		}

		@Override
		public void onCancel() {

		}
	};

	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(ADDRESS_POSITION_SAVE_KEY, addressPosition);
		super.onSaveInstanceState(outState);
	};

	private Handler completeTransactionHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case HandlerMessage.MSG_SUCCESS:
				if (dp.isShowing()) {
					dp.dismiss();
				}
				if (msg.obj != null && msg.obj instanceof SendRequest) {
					SendRequest request = (SendRequest) msg.obj;
					DialogSendConfirm dialog = new DialogSendConfirm(
							GenerateUnsignedTxActivity.this, request,
							sendConfirmListener);
					dialog.show();
				} else {
					DropdownMessage.showDropdownMessage(
							GenerateUnsignedTxActivity.this,
							R.string.password_wrong);
				}
				break;
			case HandlerMessage.MSG_PASSWORD_WRONG:
				if (dp.isShowing()) {
					dp.dismiss();
				}
				DropdownMessage.showDropdownMessage(
						GenerateUnsignedTxActivity.this,
						R.string.password_wrong);
				break;
			case HandlerMessage.MSG_FAILURE:
				if (dp.isShowing()) {
					dp.dismiss();
				}
				String msgError = getString(R.string.send_failed);
				if (msg.obj instanceof String) {
					msgError = (String) msg.obj;
				}
				DropdownMessage.showDropdownMessage(
						GenerateUnsignedTxActivity.this, msgError);
				break;
			default:
				break;
			}
		}
	};

	private Handler commitTransactionHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case HandlerMessage.MSG_SUCCESS:
				if (dp.isShowing()) {
					dp.dismiss();
				}
				Transaction tx = null;
				if (msg.obj instanceof Transaction) {
					tx = (Transaction) msg.obj;
				}
				Intent intent = getIntent();
				if (tx != null) {
					intent.putExtra(
							SelectAddressToSendActivity.INTENT_EXTRA_TRANSACTION,
							tx.getHashAsString());
				}
				setResult(RESULT_OK, intent);
				finish();
				break;
			case HandlerMessage.MSG_FAILURE:
				if (dp.isShowing()) {
					dp.dismiss();
				}
				btnSend.setEnabled(true);
				DropdownMessage.showDropdownMessage(
						GenerateUnsignedTxActivity.this, R.string.send_failed);
				break;
			default:
				break;
			}
		}
	};

	private OnClickListener sendClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			final BigInteger btc = amountCalculatorLink.getAmount();
			if (btc != null) {
				if (StringUtil.validBicoinAddress(etAddress.getText()
						.toString())) {
					try {
						SendRequest request = SendRequest.to(new Address(
								address.getNetworkParameters(), etAddress
										.getText().toString()), btc);
						request.emptyWallet = btc.equals(address
								.getBalance(BalanceType.AVAILABLE));
						CompleteTransactionRunnable completeRunnable = new CompleteTransactionRunnable(
								addressPosition, request, null);
						completeRunnable.setHandler(completeTransactionHandler);
						Thread thread = new Thread(completeRunnable);
						dp.setThread(thread);
						if (!dp.isShowing()) {
							dp.show();
						}
						thread.start();
					} catch (Exception e) {
						e.printStackTrace();
						DropdownMessage.showDropdownMessage(
								GenerateUnsignedTxActivity.this,
								R.string.send_failed);
					}
				} else {
					DropdownMessage.showDropdownMessage(
							GenerateUnsignedTxActivity.this,
							R.string.send_failed);
				}
			}
		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE
				&& resultCode == Activity.RESULT_OK) {
			final String input = data
					.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
			new StringInputParser(input) {
				@Override
				protected void bitcoinRequest(final Address address,
						final String addressLabel, final BigInteger amount,
						final String bluetoothMac) {
					etAddress.setText(address.toString());
					if (amount != null && amount.compareTo(BigInteger.ZERO) > 0) {
						amountCalculatorLink.setBtcAmount(amount);
					}
					amountCalculatorLink.requestFocus();
					validateValues();
				}

				@Override
				protected void directTransaction(final Transaction tx) {
					DropdownMessage.showDropdownMessage(
							GenerateUnsignedTxActivity.this,
							R.string.scan_watch_only_address_error);
				}

				@Override
				protected void error(final int messageResId,
						final Object... messageArgs) {
					DropdownMessage.showDropdownMessage(
							GenerateUnsignedTxActivity.this,
							R.string.scan_watch_only_address_error);
				}
			}.parse();
			return;
		}
		if (requestCode == BitherSetting.INTENT_REF.SIGN_TX_REQUEST_CODE
				&& resultCode == RESULT_OK) {
			final String qr = data
					.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
			btnSend.setEnabled(false);
			btnSend.postDelayed(new Runnable() {
				@Override
				public void run() {
					try {
						TransactionsUtil.signTransaction(tx, qr);
						CommitTransactionRunnable runnable = new CommitTransactionRunnable(
								addressPosition, tx, false);
						runnable.setHandler(commitTransactionHandler);
						Thread thread = new Thread(runnable);
						dp.setThread(thread);
						if (!dp.isShowing()) {
							dp.show();
						}
						thread.start();
					} catch (Exception e) {
						if (dp.isShowing()) {
							dp.dismiss();
						}
						btnSend.setEnabled(true);
						DropdownMessage.showDropdownMessage(
								GenerateUnsignedTxActivity.this,
								R.string.unsigned_transaction_sign_failed);
						e.printStackTrace();
					}
				}
			}, 500);
		}
	}

	private final class ReceivingAddressListener implements
			OnFocusChangeListener, TextWatcher {
		@Override
		public void onFocusChange(final View v, final boolean hasFocus) {
			if (!hasFocus)
				validateValues();
		}

		@Override
		public void afterTextChanged(final Editable s) {
			validateValues();
		}

		@Override
		public void beforeTextChanged(final CharSequence s, final int start,
				final int count, final int after) {
		}

		@Override
		public void onTextChanged(final CharSequence s, final int start,
				final int before, final int count) {
		}
	}

	private final CurrencyAmountView.Listener amountsListener = new CurrencyAmountView.Listener() {
		@Override
		public void changed() {
			validateValues();
		}

		@Override
		public void done() {
			validateValues();
			btnSend.requestFocusFromTouch();
		}

		@Override
		public void focusChanged(final boolean hasFocus) {
			if (!hasFocus) {
				validateValues();
			}
		}
	};

	private void validateValues() {
		boolean isValidAmounts = false;

		final BigInteger amount = amountCalculatorLink.getAmount();

		if (amount == null) {
		} else if (amount.signum() > 0) {
			isValidAmounts = true;
		} else {
		}
		boolean isValidAddress = StringUtil.validBicoinAddress(etAddress
				.getText().toString());
		btnSend.setEnabled(isValidAddress && isValidAmounts);
	}

	private double getExchangeRate() {
		Ticker ticker = MarketUtil.getTickerOfDefaultMarket();
		if (ticker != null) {
			return ticker.getDefaultExchangePrice();
		}
		return 0;
	}

	private BroadcastReceiver marketBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			amountCalculatorLink.setExchangeRate(getExchangeRate());
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		amountCalculatorLink.setListener(amountsListener);
		amountCalculatorLink.setExchangeRate(getExchangeRate());
		IntentFilter marketFilter = new IntentFilter(
				BroadcastUtil.ACTION_MARKET);
		registerReceiver(marketBroadcastReceiver, marketFilter);
	}

	@Override
	protected void onPause() {
		amountCalculatorLink.setListener(null);
		unregisterReceiver(marketBroadcastReceiver);
		super.onPause();
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, R.anim.slide_out_right);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void processIntent() {
		isDonate = false;
		Intent intent = getIntent();
		if (intent.hasExtra(SelectAddressToSendActivity.INTENT_EXTRA_ADDRESS)) {
			String address = intent.getExtras().getString(
					SelectAddressToSendActivity.INTENT_EXTRA_ADDRESS);
			if (StringUtil.validBicoinAddress(address)) {
				if (StringUtil.compareString(address,
						BitherSetting.DONATE_ADDRESS)) {
					isDonate = true;
				}
				etAddress.setText(address);
				BigInteger btc = (BigInteger) intent
						.getExtras()
						.getSerializable(
								SelectAddressToSendActivity.INTENT_EXTRA_AMOUNT);
				if (btc != null && btc.signum() > 0) {
					amountCalculatorLink.setBtcAmount(btc);
				}
				validateValues();
			}
		}
	}

	private void configureDonate() {
		if (isDonate) {
			btnSend.setText(R.string.donate_unsigned_transaction_verb);
			tvAddressLabel.setText(R.string.donate_receiving_address_label);
			etAddress.setEnabled(false);
			etAddress.clearFocus();
			ibtnScan.setVisibility(View.GONE);
		} else {
			btnSend.setText(R.string.address_detail_send);
			tvAddressLabel
					.setText(R.string.send_coins_fragment_receiving_address_label);
			etAddress.setEnabled(true);
			ibtnScan.setVisibility(View.VISIBLE);
		}
	}
}
