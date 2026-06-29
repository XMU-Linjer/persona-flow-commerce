CREATE TABLE IF NOT EXISTS behavior_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    source_module VARCHAR(40) NOT NULL,
    object_type VARCHAR(40) NULL,
    object_id BIGINT NULL,
    keyword VARCHAR(200) NULL,
    sku_id BIGINT NULL,
    spu_id BIGINT NULL,
    category_id BIGINT NULL,
    order_id BIGINT NULL,
    amount DECIMAL(10,2) NULL,
    payload_json TEXT NULL,
    occurred_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_behavior_event_event_id (event_id),
    KEY idx_behavior_event_user_occurred_at (user_id, occurred_at),
    KEY idx_behavior_event_type_occurred_at (event_type, occurred_at),
    KEY idx_behavior_event_sku_id (sku_id),
    KEY idx_behavior_event_spu_id (spu_id),
    KEY idx_behavior_event_category_id (category_id),
    CONSTRAINT fk_behavior_event_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT chk_behavior_event_type
        CHECK (event_type IN (
            'PRODUCT_VIEW',
            'PRODUCT_SEARCH',
            'FAVORITE_ADD',
            'FAVORITE_REMOVE',
            'CART_ADD',
            'CART_REMOVE',
            'CART_CLEAR',
            'ORDER_CREATED',
            'PAYMENT_SUCCESS',
            'ORDER_CANCELED'
        )),
    CONSTRAINT chk_behavior_event_source_module
        CHECK (source_module IN ('catalog', 'shopping', 'trade', 'behavior'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS behavior_consume_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id VARCHAR(100) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_behavior_consume_log_message_id (message_id),
    KEY idx_behavior_consume_log_event_id (event_id),
    KEY idx_behavior_consume_log_status (status),
    CONSTRAINT chk_behavior_consume_log_status
        CHECK (status IN (10, 20, 30)),
    CONSTRAINT chk_behavior_consume_log_retry_count
        CHECK (retry_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_profile_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    profile_json TEXT NOT NULL,
    summary VARCHAR(1000) NULL,
    source_workflow_id VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_profile_version_user_version (user_id, version_no),
    KEY idx_user_profile_version_user_created_at (user_id, created_at),
    CONSTRAINT fk_user_profile_version_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT chk_user_profile_version_version_no
        CHECK (version_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
