package net.bither.util;

import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Utils;

public class DynamicFeeUtils {

    public static Long getFinalDynamicFeeBase(Long dynamicFeeBase) {
        if (dynamicFeeBase == null || dynamicFeeBase <= 0) {
            return dynamicFeeBase;
        }
        String dynamicFeeBaseHex = Long.toHexString(dynamicFeeBase);
        if (Utils.isEmpty(dynamicFeeBaseHex)) {
            return null;
        }
        boolean isAddress = false;
        if (dynamicFeeBaseHex.length() % 2 == 0) {
            try {
                String address = Base58.hexToBase58WithAddress(dynamicFeeBaseHex);
                isAddress = Utils.validBicoinAddress(address);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!isAddress) {
            return dynamicFeeBase;
        }
        try {
            byte[] bytes = Utils.hexStringToByteArray(dynamicFeeBaseHex);
            int first = bytes[0] + 1;
            byte[] newBytes = new byte[bytes.length];
            newBytes[0] = (byte) first;
            Long dynamicFee = Long.parseLong(Utils.bytesToHexString(newBytes), 16);
            return dynamicFee;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
