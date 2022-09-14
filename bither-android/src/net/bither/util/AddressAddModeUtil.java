package net.bither.util;

import net.bither.R;
import net.bither.bitherj.core.Address;

public class AddressAddModeUtil {

    static public int getImgRes(Address.AddMode addMode, boolean isFromXRandom) {
        switch (addMode) {
            case Create:
            case DiceCreate:
            case BinaryCreate:
                return R.drawable.address_add_mode_bither_create_selector;
            case Import:
                return R.drawable.address_add_mode_import_selector;
            case Clone:
                return R.drawable.address_add_mode_clone_selector;
            default:
                if (isFromXRandom) {
                    return R.drawable.address_add_mode_bither_create_selector;
                } else {
                    return R.drawable.address_add_mode_other_selector;
                }
        }
    }

    static public int getDes(Address.AddMode addMode, boolean isFromXRandom) {
        int res;
        switch (addMode) {
            case Create:
                res = R.string.address_add_mode_create_des;
                break;
            case DiceCreate:
                res = R.string.address_add_mode_dice_create_des;
                break;
            case BinaryCreate:
                res = R.string.address_add_mode_binary_create_des;
                break;
            case Import:
                res = R.string.address_add_mode_import_des;
                break;
            case Clone:
                res = R.string.address_add_mode_clone_des;
                break;
            default:
                if (isFromXRandom) {
                    res = R.string.address_add_mode_create_des;
                } else {
                    res = R.string.address_add_mode_other_des;
                }
                break;
        }
        return res;
    }

}
