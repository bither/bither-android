package net.bither.model;

/**
 * Created by ltq on 2017/10/18.
 */

public enum  AddressType {

    Normal(0),
    Multisig(1),
    P2SHP2WPKH(2);

    private int value;

    public int getValue() {
        return value;
    }

    AddressType(int value) {
        this.value = value;
    }

    public static AddressType fromValue(int value){
        for (AddressType type : AddressType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return Normal;
    }

    public String addressName() {
        switch (this) {
            case Normal:
                return  "address_normal";
            case Multisig:
                return  "address_multisig";
            case P2SHP2WPKH:
                return "address_segwit";
        }
        return "address_segwit";
    }

    public String addressTypeName() {
        switch (this) {
            case Normal:
                return "address_normal_type";
            case Multisig:
                return "address_multisig_type";
            case P2SHP2WPKH:
                return "address_segwit_type";
        }
        return "address_segwit_type";
    }
}
