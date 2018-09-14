package net.bither.model;

import net.bither.bitherj.BitherjSettings.MarketType;
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
    private MarketType marketType;
    private ExchangeUtil.Currency currency;
    private double lower;
    private double higher;

    public PriceAlert(MarketType marketType, double limit, double higher) {
        this.marketType = marketType;
        this.lower = limit;
        this.higher = higher;
        this.currency = AppSharedPreference.getInstance().getDefaultExchangeType();
    }

    public MarketType getMarketType() {
        return this.marketType;
    }

    public ExchangeUtil.Currency getCurrency() {
        return this.currency;
    }

    public double getExchangeLower() {
        return this.lower * ExchangeUtil.getRate(getCurrency());
    }

    public double getExchangeHigher() {
        return this.higher * ExchangeUtil.getRate(getCurrency());
    }

    public double getLower() {
        return this.lower;
    }

    public double getHigher() {
        return this.higher;
    }

    public void setLower(double lower) {
        this.lower = lower;
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

    @Override
    public String toString() {
        return "h:" + this.higher + ",he:" + getExchangeHigher() + ",l:" + this.lower + "," +
                "le:" + getExchangeLower() + "," + getCurrency().getSymbol();
    }

    public static PriceAlert getPriceAlert(MarketType marketType) {
        for (PriceAlert priceAlert : priceAlertList) {
            if (priceAlert.getMarketType() == marketType) {
                return priceAlert;
            }
        }
        return null;
    }

    public static void removePriceAlert(PriceAlert priceAlert) {
        if (priceAlert==null){
            return;
        }
        synchronized (paLock) {
            boolean removed = false;
            if (priceAlert.getHigher() <= 0 && priceAlert.getLower() <= 0) {
                if (priceAlertList.contains(priceAlert)) {
                    priceAlertList.remove(priceAlert);
                    removed = true;
                }
                if (removed) {
                    saveFile();
                }
            } else {
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
                            isAdd = true;
                        }
                    }
                }
            } else {
                isAdd = true;
            }
            if (isAdd) {
                LogUtil.d("price", priceAlert.toString());
                priceAlertList.remove(priceAlert);
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
        }).start();
    }

    public static List<PriceAlert> getPriceAlertList() {
        synchronized (paLock) {
            return priceAlertList;
        }
    }

    private static List<PriceAlert> getPriceAlertFromFile() {
        try {
            File file = FileUtil.getPriceAlertFile();
            List<PriceAlert> priceAlertList = new ArrayList<>();
            if (file == null) {
                return priceAlertList;
            }
            priceAlertList = (List<PriceAlert>) FileUtil.deserialize(file);
            if (priceAlertList == null) {
                priceAlertList = new ArrayList<>();
            }
            return priceAlertList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
