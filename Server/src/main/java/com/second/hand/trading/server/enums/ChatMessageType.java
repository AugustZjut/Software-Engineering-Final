package com.second.hand.trading.server.enums;

import java.util.Arrays;

/**
 * Enumeration for chat message types.
 */
public enum ChatMessageType {
    TEXT(0),
    IMAGE(1),
    PRODUCT_CARD(2),
    SYSTEM(3);

    private final int code;

    ChatMessageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ChatMessageType fromCode(Integer code) {
        if (code == null) {
            return TEXT;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElse(TEXT);
    }
}
