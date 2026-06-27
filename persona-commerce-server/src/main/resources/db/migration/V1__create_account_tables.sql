CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    display_name VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_login_identity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    identity_type VARCHAR(20) NOT NULL,
    identifier VARCHAR(100) NOT NULL,
    normalized_identifier VARCHAR(100) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_login_identity_type_identifier (identity_type, normalized_identifier),
    KEY idx_login_identity_user_id (user_id),
    CONSTRAINT fk_login_identity_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    UNIQUE KEY uk_sys_user_role_user_role (user_id, role_id),
    KEY idx_sys_user_role_role_id (role_id),
    CONSTRAINT fk_sys_user_role_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role
        FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_address (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(50) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    postal_code VARCHAR(20) NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    default_user_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN is_default = TRUE THEN user_id ELSE NULL END
    ) VIRTUAL,
    PRIMARY KEY (id),
    KEY idx_user_address_user_id (user_id),
    UNIQUE KEY uk_user_address_default_user (default_user_id),
    CONSTRAINT fk_user_address_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sys_role (id, code, name)
VALUES
    (1, 'ROLE_USER', 'User'),
    (2, 'ROLE_ADMIN', 'Admin');

-- Demo accounts are for local development and permission checks only.
-- Password values are BCrypt hashes, not production secrets.
INSERT INTO sys_user (id, display_name, password_hash, avatar_url)
VALUES
    (10001, 'Demo User', '$2a$10$ZsUUfzgdjMmX0fUK7eFfFuXtXtG10uQOEVP.q/R/ZLdJEWpGw2h8S', NULL),
    (10002, 'Demo Admin', '$2a$10$4xeq9GuwHYhpFDqrETNRreNhWd8tzcX2JzLquAii7N2818uMVBzXS', NULL);

INSERT INTO user_login_identity (
    id,
    user_id,
    identity_type,
    identifier,
    normalized_identifier,
    verified
)
VALUES
    (10001, 10001, 'USERNAME', 'demo_user', 'demo_user', TRUE),
    (10002, 10002, 'USERNAME', 'demo_admin', 'demo_admin', TRUE);

INSERT INTO sys_user_role (user_id, role_id)
VALUES
    (10001, 1),
    (10002, 1),
    (10002, 2);
