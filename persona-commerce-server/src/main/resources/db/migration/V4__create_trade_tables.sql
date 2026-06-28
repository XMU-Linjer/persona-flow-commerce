CREATE TABLE IF NOT EXISTS inventory_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku_id BIGINT NOT NULL,
    available_quantity INT NOT NULL,
    locked_quantity INT NOT NULL,
    sold_quantity INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_stock_sku_id (sku_id),
    KEY idx_inventory_stock_sku_id (sku_id),
    CONSTRAINT fk_inventory_stock_sku
        FOREIGN KEY (sku_id) REFERENCES product_sku (id),
    CONSTRAINT chk_inventory_stock_available_quantity
        CHECK (available_quantity >= 0),
    CONSTRAINT chk_inventory_stock_locked_quantity
        CHECK (locked_quantity >= 0),
    CONSTRAINT chk_inventory_stock_sold_quantity
        CHECK (sold_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS trade_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    address_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    province VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    detail_address VARCHAR(300) NOT NULL,
    postal_code VARCHAR(20) NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status TINYINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    canceled_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_order_order_no (order_no),
    KEY idx_trade_order_user_id (user_id),
    KEY idx_trade_order_status (status),
    KEY idx_trade_order_created_at (created_at),
    CONSTRAINT fk_trade_order_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT chk_trade_order_status
        CHECK (status IN (10, 20, 30))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS trade_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    spu_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    sku_name VARCHAR(200) NOT NULL,
    image_url VARCHAR(500) NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_trade_order_item_order_id (order_id),
    KEY idx_trade_order_item_order_no (order_no),
    KEY idx_trade_order_item_user_id (user_id),
    KEY idx_trade_order_item_sku_id (sku_id),
    CONSTRAINT fk_trade_order_item_order
        FOREIGN KEY (order_id) REFERENCES trade_order (id),
    CONSTRAINT fk_trade_order_item_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_trade_order_item_sku
        FOREIGN KEY (sku_id) REFERENCES product_sku (id),
    CONSTRAINT chk_trade_order_item_quantity
        CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_no VARCHAR(64) NOT NULL,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status TINYINT NOT NULL,
    paid_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_record_payment_no (payment_no),
    UNIQUE KEY uk_payment_record_order_id (order_id),
    KEY idx_payment_record_order_no (order_no),
    KEY idx_payment_record_user_id (user_id),
    CONSTRAINT fk_payment_record_order
        FOREIGN KEY (order_id) REFERENCES trade_order (id),
    CONSTRAINT fk_payment_record_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT chk_payment_record_channel
        CHECK (channel = 'MOCK'),
    CONSTRAINT chk_payment_record_status
        CHECK (status = 20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO inventory_stock (sku_id, available_quantity, locked_quantity, sold_quantity)
SELECT id, 100, 0, 0
FROM product_sku
ORDER BY id;
