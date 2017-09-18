package net.bither.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import net.bither.BCCAssetsDetectHDActivity;
import net.bither.BCCAssetsDetectHotActivtity;
import net.bither.R;
import net.bither.bitherj.api.UnspentOutputsApi;
import net.bither.bitherj.core.AbstractHD;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.utils.UnitUtil;
import net.bither.model.ExtractBccUtxo;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogProgress;

import org.json.JSONArray;

import java.io.Serializable;
import java.util.List;

/**
 * Created by ltq on 2017/9/14.
 */

public class DetectAnotherAssetsUtil {
    private Activity activity;
    private DialogProgress dp;

    public DetectAnotherAssetsUtil(Activity activity) {
        this.activity = activity;
         dp = new DialogProgress(activity, R.string.please_wait);
    }

    public void getBCCUnspentOutputs(final String address) {
        dp.show();
        Runnable BTCUnspentOutputs = new Runnable() {
            @Override
            public void run() {
                UnspentOutputsApi unspentOutputsApi = new UnspentOutputsApi(address);
                try {
                    unspentOutputsApi.handleHttpGet();
                    JSONArray jsonArray = new JSONArray(unspentOutputsApi.getResult());
                    List<ExtractBccUtxo> extractBccUtxos = ExtractBccUtxo.format(jsonArray);
                    final List<Out> rawOutList = ExtractBccUtxo.rawOutList(extractBccUtxos);
                    extractBcc(extractBccUtxos,rawOutList,address);
                    dp.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                    dp.dismiss();
                }
            }
        };
        new Thread(BTCUnspentOutputs).start();
    }

    public void getBCCHDUnspentOutputs(final String address, final AbstractHD.PathType path, final int index) {
        dp.show();
        Runnable BTCUnspentOutputs = new Runnable() {
            @Override
            public void run() {
                UnspentOutputsApi unspentOutputsApi = new UnspentOutputsApi(address);
                try {
                    unspentOutputsApi.handleHttpGet();
                    JSONArray jsonArray = new JSONArray(unspentOutputsApi.getResult());
                    List<ExtractBccUtxo> extractBccUtxos = ExtractBccUtxo.format(jsonArray);
                    final List<Out> rawOutList = ExtractBccUtxo.rawOutList(extractBccUtxos);
                    extractHDBcc(extractBccUtxos,rawOutList, path, index,
                            address);
                    dp.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                    dp.dismiss();
                }
            }
        };
        new Thread(BTCUnspentOutputs).start();
    }


    private void extractBcc(final List<ExtractBccUtxo> extractBccUtxos, final List<Out> rawOutList, final String toAddress) {
        final Runnable alertTaskRunnable = new Runnable() {
            @Override
            public void run() {
                if (getAmount(rawOutList) > 0) {
                    DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(activity, String.format(activity.getString(R.string.detect_exist_another_assets_alert),
                            UnitUtil.formatValue(getAmount(rawOutList), UnitUtil.BitcoinUnit.BTC), "BCC"),
                            activity.getString(R.string.extract_assets),
                            activity.getString(R.string.cancel),
                            new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(activity, BCCAssetsDetectHotActivtity.class);
                                    intent.putExtra(BCCAssetsDetectHotActivtity.DECTECTED_BCC_AMOUNT_TAG,(Serializable)extractBccUtxos);
                                    activity.startActivity(intent);
                                }
                            });
                    dialogConfirmTask.show();
                } else {
                    DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(activity,
                            activity.getString(R.string.detect_no_assets_alert), new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
                    dialogConfirmTask.show();
                }
            }
        };
        new Thread(){
            public void run() {
                new Handler(Looper.getMainLooper()).post(alertTaskRunnable);
            }
        }.start();
    }

    private void extractHDBcc(final List<ExtractBccUtxo> extractBccUtxos, final List<Out> rawOutList, final AbstractHD.PathType path, final int index, final String toAddress) {
        final Runnable alertTaskRunnable = new Runnable() {
            @Override
            public void run() {
                if (getAmount(rawOutList) > 0) {
                    DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(activity, String.format(activity.getString(R.string.detect_exist_another_assets_alert),
                            UnitUtil.formatValue(getAmount(rawOutList), UnitUtil.BitcoinUnit.BTC), "BCC"),
                            activity.getString(R.string.extract_assets),
                            activity.getString(R.string.cancel),
                            new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(activity, BCCAssetsDetectHDActivity.class);
                                    intent.putExtra(BCCAssetsDetectHotActivtity.DECTECTED_BCC_AMOUNT_TAG,(Serializable)extractBccUtxos);
                                    intent.putExtra(BCCAssetsDetectHotActivtity.DECTECTED_BCC_HD_PATH_TYPE, path.getValue());
                                    intent.putExtra(BCCAssetsDetectHotActivtity.DECTECTED_BCC_HD_ADDRESS_INDEX, index);
                                    activity.startActivity(intent);
                                }
                            });
                    dialogConfirmTask.show();
                } else {
                    DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(activity,
                            activity.getString(R.string.detect_no_assets_alert), new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
                    dialogConfirmTask.show();
                }
            }
        };
        new Thread(){
            public void run() {
                new Handler(Looper.getMainLooper()).post(alertTaskRunnable);
            }
        }.start();
    }


    private long getAmount(List<Out> outs) {
        long amount = 0;
        for (Out out : outs) {
            amount += out.getOutValue();
        }
        return amount;
    }
}
