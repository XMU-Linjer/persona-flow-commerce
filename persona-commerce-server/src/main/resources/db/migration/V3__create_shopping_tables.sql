CREATE TABLE IF NOT EXISTS user_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_favorite_user_sku (user_id, sku_id),
    KEY idx_user_favorite_user_id (user_id),
    KEY idx_user_favorite_sku_id (sku_id),
    CONSTRAINT fk_user_favorite_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_favorite_sku
        FOREIGN KEY (sku_id) REFERENCES product_sku (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS shopping_cart_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_item_user_sku (user_id, sku_id),
    KEY idx_cart_item_user_id (user_id),
    KEY idx_cart_item_sku_id (sku_id),
    CONSTRAINT fk_cart_item_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_cart_item_sku
        FOREIGN KEY (sku_id) REFERENCES product_sku (id),
    CONSTRAINT chk_cart_item_quantity
        CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
