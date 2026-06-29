package com.personaflow.commerce.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    COMMON_VALIDATION_FAILED("COMMON_VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Request validation failed"),
    COMMON_BAD_REQUEST("COMMON_BAD_REQUEST", HttpStatus.BAD_REQUEST, "Bad request"),
    COMMON_INTERNAL_ERROR("COMMON_INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    ACCOUNT_USERNAME_EXISTS("ACCOUNT_USERNAME_EXISTS", HttpStatus.CONFLICT, "Username already exists"),
    ACCOUNT_INVALID_CREDENTIALS("ACCOUNT_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    ACCOUNT_UNSUPPORTED_IDENTITY_TYPE("ACCOUNT_UNSUPPORTED_IDENTITY_TYPE", HttpStatus.BAD_REQUEST, "Unsupported identity type"),
    ACCOUNT_UNAUTHORIZED("ACCOUNT_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    ACCOUNT_FORBIDDEN("ACCOUNT_FORBIDDEN", HttpStatus.FORBIDDEN, "Access is denied"),
    ACCOUNT_USER_NOT_FOUND("ACCOUNT_USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Current user was not found"),
    ACCOUNT_ADDRESS_NOT_FOUND("ACCOUNT_ADDRESS_NOT_FOUND", HttpStatus.NOT_FOUND, "Address not found"),
    ACCOUNT_CURRENT_PASSWORD_INVALID("ACCOUNT_CURRENT_PASSWORD_INVALID", HttpStatus.BAD_REQUEST, "Current password is invalid"),
    ACCOUNT_PASSWORD_INVALID("ACCOUNT_PASSWORD_INVALID", HttpStatus.BAD_REQUEST, "Password format is invalid"),
    ADDRESS_NOT_FOUND("ADDRESS_NOT_FOUND", HttpStatus.NOT_FOUND, "Address not found"),
    ADDRESS_NOT_OWNED("ADDRESS_NOT_OWNED", HttpStatus.FORBIDDEN, "Address does not belong to current user"),

    CATALOG_PRODUCT_NOT_FOUND("CATALOG_PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND, "Product not found"),
    CATALOG_SKU_NOT_FOUND("CATALOG_SKU_NOT_FOUND", HttpStatus.NOT_FOUND, "SKU not found"),
    CATALOG_PRODUCT_NOT_SELLABLE("CATALOG_PRODUCT_NOT_SELLABLE", HttpStatus.CONFLICT, "Product is not sellable"),

    SHOPPING_CART_ITEM_NOT_FOUND("SHOPPING_CART_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "Cart item not found"),
    SHOPPING_INVALID_QUANTITY("SHOPPING_INVALID_QUANTITY", HttpStatus.BAD_REQUEST, "Quantity must be greater than 0"),

    TRADE_ORDER_NOT_FOUND("TRADE_ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "Order not found"),
    TRADE_ORDER_STATUS_NOT_ALLOWED("TRADE_ORDER_STATUS_NOT_ALLOWED", HttpStatus.CONFLICT, "Order status is not allowed"),
    TRADE_INVALID_QUANTITY("TRADE_INVALID_QUANTITY", HttpStatus.BAD_REQUEST, "Quantity must be greater than 0"),
    TRADE_ORDER_EMPTY_ITEMS("TRADE_ORDER_EMPTY_ITEMS", HttpStatus.BAD_REQUEST, "Order items must not be empty"),
    TRADE_DUPLICATE_SKU("TRADE_DUPLICATE_SKU", HttpStatus.BAD_REQUEST, "Order items contain duplicate SKU"),
    TRADE_STOCK_NOT_FOUND("TRADE_STOCK_NOT_FOUND", HttpStatus.NOT_FOUND, "Stock was not found"),
    TRADE_STOCK_NOT_ENOUGH("TRADE_STOCK_NOT_ENOUGH", HttpStatus.CONFLICT, "Stock is not enough"),
    TRADE_STOCK_STATE_INVALID("TRADE_STOCK_STATE_INVALID", HttpStatus.CONFLICT, "Stock state is invalid"),
    TRADE_PAYMENT_RECORD_EXISTS("TRADE_PAYMENT_RECORD_EXISTS", HttpStatus.CONFLICT, "Payment record already exists"),

    AGENT_SERVICE_UNAVAILABLE("AGENT_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Agent service is unavailable"),
    AGENT_PROFILE_BUILD_FAILED("AGENT_PROFILE_BUILD_FAILED", HttpStatus.BAD_GATEWAY, "Agent profile build failed");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
