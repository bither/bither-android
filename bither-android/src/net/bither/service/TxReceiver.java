package net.bither.service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.bither.BitherSetting;
import net.bither.NotificationAndroidImpl;
import net.bither.R;
import net.bither.activity.hot.HotActivity;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.utils.GenericUtils;
import net.bither.bitherj.utils.Utils;
import net.bither.util.SystemUtil;


public class TxReceiver extends BroadcastReceiver {

    private BlockchainService blockchainService;
    private NotificationManager nm;
    private TickReceiver tickReceiver;

    public TxReceiver(BlockchainService service, TickReceiver tickReceiver) {
        this.blockchainService = service;
        this.tickReceiver = tickReceiver;
        nm = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
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
        if (txNotificationType == Tx.TxNotificationType.txReceive.getValue()) {
            boolean isReceived = amount > 0;
            amount = Math.abs(amount);
            notifyCoins(address, amount, isReceived);
        }

    }

    private void notifyCoins(String address, final long amount,
                             boolean isReceived) {
        String contentText = address;
        String title = GenericUtils.formatValue(amount) + " BTC";
        if (isReceived) {
            title = blockchainService.getString(R.string.feed_received_btc) + " " + title;
        } else {
            title = blockchainService.getString(R.string.feed_send_btc) + " " + title;
        }
        Intent intent = new Intent(blockchainService, HotActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.NOTIFICATION_ADDRESS, address);
        SystemUtil.nmNotifyOfWallet(nm, blockchainService,
                BitherSetting.NOTIFICATION_ID_COINS_RECEIVED, intent, title,
                contentText, R.drawable.ic_launcher, R.raw.coins_received);

    }
}
