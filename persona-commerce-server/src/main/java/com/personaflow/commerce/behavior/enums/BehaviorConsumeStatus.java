package com.personaflow.commerce.behavior.enums;

public enum BehaviorConsumeStatus {
    PENDING(10),
    SUCCESS(20),
    FAILED(30);

    private final int code;

    BehaviorConsumeStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
