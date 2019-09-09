package net.bither.enums;

/**
 * Created by ltq on 2017/7/21.
 */

public enum SignMessageTypeSelect {
    HdReceive(0), HdChange(1), Hot(2), BitpieColdReceive(3), BitpieColdChange(4);

    private int value;

    SignMessageTypeSelect(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean isBitpieCold() {
        return this == BitpieColdReceive || this == BitpieColdChange;
    }

}
