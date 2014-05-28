package net.bither.model;

import net.bither.BitherSetting;
import net.bither.preference.AppSharedPreference;
import net.bither.util.ExchangeUtil;
import net.bither.util.FileUtil;

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

    public void setMarketType(BitherSetting.MarketType marketType) {
        this.marketType = marketType;

    }

    public ExchangeUtil.ExchangeType getExchangeType() {
        return this.exchangeType;
    }

    public void setExchangeType(ExchangeUtil.ExchangeType exchangeType) {
        this.exchangeType = exchangeType;
    }

    public double getLower() {
        return this.lower;
    }

    public double getExchangeLower() {
        return this.lower * ExchangeUtil.getRate(getExchangeType());
    }

    public void setLower(double lower) {
        this.lower = lower;
    }

    public double getExchangeHigher() {
        return this.higher * ExchangeUtil.getRate(getExchangeType());
    }

    public double getHigher() {
        return this.higher;
    }

    public void setHigher(double higher) {
        this.higher = higher;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PriceAlert) {
            PriceAlert priceAlert = (PriceAlert) o;
            return getMarketType() == priceAlert.getMarketType();
        }
        return false;
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
            if (priceAlertList.contains(priceAlert)) {
                priceAlertList.remove(priceAlert);
            }
            File file = FileUtil.getPriceAlertFile();
            FileUtil.serializeObject(file, priceAlertList);
        }
    }

    public static void addPriceAlert(PriceAlert priceAlert) {
        synchronized (paLock) {
            File file = FileUtil.getPriceAlertFile();
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

    private static List<PriceAlert> getPriceAlertFromFile() {
        File file = FileUtil.getPriceAlertFile();
        List<PriceAlert> priceAlertList = (List<PriceAlert>) FileUtil.deserialize(file);
        if (priceAlertList == null) {
            priceAlertList = new ArrayList<PriceAlert>();
        }
        return priceAlertList;
    }
}
