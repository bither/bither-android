package net.bither.util;

import android.app.Activity;

import net.bither.R;
import net.bither.bitherj.core.PeerManager;
import net.bither.ui.base.DropdownMessage;

public class SendUtil {

    static public boolean isCanSend(Activity activity, boolean isSyncComplete) {
        if (!NetworkUtil.isConnected()) {
            DropdownMessage.showDropdownMessage(activity, R.string.tip_network_error);
            return false;
        }
        if (!isSyncComplete) {
            DropdownMessage.showDropdownMessage(activity, activity.getString(R.string.no_sync_complete));
            return false;
        }
        if (PeerManager.instance().getConnectedPeers().size() == 0) {
            DropdownMessage.showDropdownMessage(activity, R.string.tip_no_peers_connected);
            return false;
        }
        long displayLastBlockHeight = PeerManager.instance().getConnectedPeers().get(0).getDisplayLastBlockHeight();
        if (PeerManager.instance().getLastBlockHeight() < displayLastBlockHeight) {
            DropdownMessage.showDropdownMessage(activity, activity.getString(R.string.tip_sync_block_height, displayLastBlockHeight - PeerManager.instance().getLastBlockHeight()));
            return false;
        }
        return true;
    }
}
