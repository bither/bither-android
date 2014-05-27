package net.bither.model;

import net.bither.BitherSetting;
import net.bither.util.FileUtil;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PriceAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final byte[] paLock = new byte[0];
    private static List<PriceAlert> priceAlertList = null;

    private BitherSetting.MarketType marketType;
    private double limit;
    private double caps;

    public BitherSetting.MarketType getMarketType() {
        return this.marketType;
    }

    public void setMarketType(BitherSetting.MarketType marketType) {
        this.marketType = marketType;

    }

    public double getLimit() {
        return this.limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public double getCaps() {
        return this.caps;
    }

    public void setCaps(double caps) {
        this.caps = caps;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PriceAlert) {
            PriceAlert priceAlert = (PriceAlert) o;
            return getMarketType() == priceAlert.getMarketType();
        }
        return false;
    }

    public static void addPriceAlert(PriceAlert priceAlert) {
        synchronized (paLock) {
            File file = FileUtil.getPriceAlertFile();
            if (priceAlertList == null && file.exists()) {
                priceAlertList = (List<PriceAlert>) FileUtil.deserialize(file);
            }

            if (priceAlertList == null) {
                priceAlertList = new ArrayList<PriceAlert>();
            }
            if (priceAlertList.contains(priceAlert)) {
                priceAlertList.remove(priceAlert);
            }
            priceAlertList.add(priceAlert);
            FileUtil.serializeObject(file, priceAlertList);
        }
    }

    public static List<PriceAlert> getPriceAlertList() {
        synchronized (paLock) {
            return priceAlertList;
        }
    }
}
