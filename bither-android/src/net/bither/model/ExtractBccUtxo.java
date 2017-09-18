package net.bither.model;

import net.bither.bitherj.core.Out;
import net.bither.bitherj.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ltq on 2017/9/14.
 */

public class ExtractBccUtxo implements Serializable {

    private String address;
    private String txid;
    private int vout;
    private String scriptPubKey;
    private double amount;
    private long satoshis;
    private long height;
    private long confirmations;

    public int getVout() {
        return vout;
    }

    public String getScriptPubKey() {
        return scriptPubKey;
    }

    public double getAmount() {
        return amount;
    }

    public long getSatoshis() {
        return satoshis;
    }

    public String getAddress() {
        return address;
    }

    public String getTxid() {
        return txid;
    }

    public ExtractBccUtxo(String address, String txid, int vout, String scriptPubKey
    ,Double amount, long satoshis, long height, long confirmations) {
        this.address = address;
        this.txid = txid;
        this.vout = vout;
        this.scriptPubKey = scriptPubKey;
        this.amount = amount;
        this.satoshis = satoshis;
        this.height = height;
        this.confirmations = confirmations;
    }


    public static List<ExtractBccUtxo> format(JSONArray jsonArray)
            throws JSONException {
        final List<ExtractBccUtxo> bccOutList = new ArrayList<ExtractBccUtxo>();

       for (int i=0; i< jsonArray.length(); i++) {
           JSONObject object = jsonArray.getJSONObject(i);
           String address = object.getString("address");
           String txid = object.getString("txid");
           int vout = object.getInt("vout");
           String scriptPubKey = object.getString("scriptPubKey");
           Double amount = object.getDouble("amount");
           long satoshis = object.getLong("satoshis");
           long height = object.getLong("height");
           long confirmations = object.getLong("confirmations");
           ExtractBccUtxo extractBccUtxo = new ExtractBccUtxo(address,txid,vout,scriptPubKey,
                   amount,satoshis,height,confirmations);
           bccOutList.add(extractBccUtxo);
       }
        return bccOutList;
    }

    public static List<Out> rawOutList(List<ExtractBccUtxo> extractBccUtxos) {
        final List<Out> rawOutList = new ArrayList<Out>();
        for (ExtractBccUtxo extractBccUtxo : extractBccUtxos) {
            Out rawOut = new Out();
            rawOut.setOutValue(extractBccUtxo.getSatoshis());
            rawOut.setOutSn(extractBccUtxo.getVout());
            rawOut.setOutScript(Utils.hexStringToByteArray(extractBccUtxo.getScriptPubKey()));
            rawOut.setTxHash(Utils.reverseBytes(Utils.hexStringToByteArray(extractBccUtxo.getTxid())));
            rawOutList.add(rawOut);
        }
        return rawOutList;
    }
}

