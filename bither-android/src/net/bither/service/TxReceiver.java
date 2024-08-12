package net.bither.service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.bither.BitherSetting;
import net.bither.NotificationAndroidImpl;
import net.bither.R;
import net.bither.activity.hot.HotActivity;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.util.SystemUtil;
import net.bither.util.UnitUtilWrapper;


public class TxReceiver extends BroadcastReceiver {

    private Context mContext;
    private NotificationManager nm;
    private TickReceiver tickReceiver;

    public TxReceiver(Context context, TickReceiver tickReceiver) {
        this.mContext = context;
        this.tickReceiver = tickReceiver;
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Utils.compareString(intent.getAction(), NotificationAndroidImpl.ACTION_ADDRESS_BALANCE)) {
            return;
        }
        if (tickReceiver != null) {
            tickReceiver.setTransactionsReceived();
        }
        String address = intent.getStringExtra(NotificationAndroidImpl.MESSAGE_ADDRESS);
        long amount = intent.getLongExtra(NotificationAndroidImpl.MESSAGE_DELTA_BALANCE, 0);
        int txNotificationType = intent.getIntExtra(NotificationAndroidImpl.MESSAGE_TX_NOTIFICATION_TYPE, 0);
        if (txNotificationType == Tx.TxNotificationType.txReceive.getValue() && amount != 0) {
            boolean isReceived = amount > 0;
            amount = Math.abs(amount);
            notifyCoins(address, amount, isReceived);
        }

    }

    private void notifyCoins(String address, final long amount,
                             boolean isReceived) {
        String contentText = address;
        if (Utils.compareString(address, HDAccount.HDAccountPlaceHolder)) {
            contentText = mContext.getString(R.string.address_group_hd);
        } else if (Utils.compareString(address, HDAccount.HDAccountMonitoredPlaceHolder)) {
            contentText = mContext.getString(R.string.address_group_hd_monitored);
        }
        String title = UnitUtilWrapper.formatValue(amount) + " " + AppSharedPreference.getInstance().getBitcoinUnit().name();
        if (isReceived) {
            title = mContext.getString(R.string.feed_received_btc) + " " + title;
        } else {
            title = mContext.getString(R.string.feed_send_btc) + " " + title;
        }
        Intent intent = new Intent(mContext, HotActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.NOTIFICATION_ADDRESS, address);
        SystemUtil.nmNotifyOfWallet(nm, mContext,
                BitherSetting.NOTIFICATION_ID_COINS_RECEIVED, intent, title,
                contentText, R.drawable.ic_launcher, R.raw.coins_received);

    }
}
