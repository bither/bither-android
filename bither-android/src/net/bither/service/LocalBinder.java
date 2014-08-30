package net.bither.service;

import android.os.Binder;

/**
 * Created by nn on 14-8-23.
 */
public class LocalBinder extends Binder {
    private BlockchainService blockchainService;

    public LocalBinder(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    public BlockchainService getService() {
        return this.blockchainService;
    }
}
