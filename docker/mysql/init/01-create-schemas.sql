CREATE DATABASE IF NOT EXISTS elm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_points CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_account CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_catalog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_wallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_address CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_merchant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON elm.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_points.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_account.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_catalog.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_order.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_user.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_wallet.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_address.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_merchant.* TO 'elm'@'%';
FLUSH PRIVILEGES;

USE elm_user;

CREATE TABLE IF NOT EXISTS authority (
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    activated BOOLEAN NOT NULL,
    create_time DATETIME,
    update_time DATETIME,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_authority (
    user_id BIGINT NOT NULL,
    authority_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, authority_name),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_authority_name FOREIGN KEY (authority_name) REFERENCES authority (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO authority (name) VALUES ('USER'), ('ADMIN'), ('BUSINESS');
INSERT IGNORE INTO `user` (id, username, password, activated, deleted) VALUES (1, 'admin', '$2a$10$X2ngW1dCn.osyqZUXksi1.88repVt9jc1N2YW9kfB/69nozB1PJs.', 1, 0);
INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (1, 'ADMIN'), (1, 'USER');

