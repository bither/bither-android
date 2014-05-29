package net.bither.model;

import net.bither.BitherSetting;
import net.bither.preference.AppSharedPreference;
import net.bither.util.ExchangeUtil;
import net.bither.util.FileUtil;
import net.bither.util.LogUtil;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PriceAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final byte[] paLock = new byte[0];
    private static List<PriceAlert> priceAlertList = getPriceAlertFromFile();
    private BitherSetting.MarketType marketType;
    private ExchangeUtil.ExchangeType exchangeType;
    private double lower;
    private double higher;

    public PriceAlert(BitherSetting.MarketType marketType, double limit, double higher) {
        this.marketType = marketType;
        this.lower = limit;
        this.higher = higher;
        this.exchangeType = AppSharedPreference.getInstance().getDefaultExchangeType();
    }

    public BitherSetting.MarketType getMarketType() {
        return this.marketType;
    }

    public ExchangeUtil.ExchangeType getExchangeType() {
        return this.exchangeType;
    }

    public double getExchangeLower() {
        return this.lower * ExchangeUtil.getRate(getExchangeType());
    }

    public double getExchangeHigher() {
        return this.higher * ExchangeUtil.getRate(getExchangeType());
    }

    public double getLower() {
        return this.lower;
    }

    public double getHigher() {
        return this.higher;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PriceAlert) {
            PriceAlert priceAlert = (PriceAlert) o;
            return getMarketType() == priceAlert.getMarketType();
        }
        return false;
    }

    @Override
    public String toString() {
        return "h:" + this.higher + ",he:" + getExchangeHigher() + ",l:" + this.lower + "," +
                "le:" + getExchangeLower() + "," + getExchangeType().getSymbol();
    }

    public static PriceAlert getPriceAlert(BitherSetting.MarketType marketType) {
        for (PriceAlert priceAlert : priceAlertList) {
            if (priceAlert.getMarketType() == marketType) {
                return priceAlert;
            }
        }
        return null;
    }

    public static void removePriceAlert(PriceAlert priceAlert) {
        synchronized (paLock) {
            boolean removed = false;
            if (priceAlertList.contains(priceAlert)) {
                priceAlertList.remove(priceAlert);
                removed = true;
            }
            if (removed) {
                saveFile();
            }
        }
    }

    public static void addPriceAlert(PriceAlert priceAlert) {
        synchronized (paLock) {
            boolean isAdd = false;
            if (priceAlertList.contains(priceAlert)) {
                for (PriceAlert cache : priceAlertList) {
                    if (cache.equals(priceAlert)) {
                        if (cache.getLower() != priceAlert.getLower() || cache.getHigher() !=
                                priceAlert.getHigher()) {
                            priceAlertList.remove(cache);
                            isAdd = true;
                        }
                    }
                }
            } else {
                isAdd = true;
            }
            if (isAdd) {
                LogUtil.d("price", priceAlert.toString());
                priceAlertList.add(priceAlert);
                saveFile();
            }
        }
    }

    private static void saveFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (priceAlertList) {
                    File file = FileUtil.getPriceAlertFile();
                    FileUtil.serializeObject(file, priceAlertList);
                }
            }
        });
    }

    public static List<PriceAlert> getPriceAlertList() {
        synchronized (paLock) {
            return priceAlertList;
        }
    }

    private static List<PriceAlert> getPriceAlertFromFile() {
        File file = FileUtil.getPriceAlertFile();
        List<PriceAlert> priceAlertList = (List<PriceAlert>) FileUtil.deserialize(file);
        if (priceAlertList == null) {
            priceAlertList = new ArrayList<PriceAlert>();
        }
        return priceAlertList;
    }
}
