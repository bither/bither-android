package net.bither.service;

import android.os.Binder;


public class LocalBinder extends Binder {
    private BlockchainService blockchainService;

    public LocalBinder(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    public BlockchainService getService() {
        return this.blockchainService;
    }
}
