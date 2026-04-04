package com.example.common.transaction;

/**
 * TCC事务阶段枚举
 */
public enum TccPhase {
    TRY("try"),
    CONFIRM("confirm"),
    CANCEL("cancel");

    private final String value;

    TccPhase(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TccPhase fromValue(String value) {
        for (TccPhase phase : TccPhase.values()) {
            if (phase.value.equalsIgnoreCase(value)) {
                return phase;
            }
        }
        throw new IllegalArgumentException("Unknown TCC phase: " + value);
    }
}