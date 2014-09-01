package net.bither.service;


public class ActivityHistoryEntry {
    public final int numTransactionsReceived;
    public final int numBlocksDownloaded;

    public ActivityHistoryEntry(final int numTransactionsReceived,
                                final int numBlocksDownloaded) {
        this.numTransactionsReceived = numTransactionsReceived;
        this.numBlocksDownloaded = numBlocksDownloaded;
    }

    @Override
    public String toString() {
        return numTransactionsReceived + "/" + numBlocksDownloaded;
    }
}
