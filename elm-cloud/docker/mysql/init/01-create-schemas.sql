-- =============================================
-- Elm Cloud 数据库架构初始化脚本
-- 负责创建所有需要的数据库和基础表结构
-- =============================================

CREATE DATABASE IF NOT EXISTS elm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_points CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_catalog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_wallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_address CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_merchant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建数据库用户并授权
CREATE USER IF NOT EXISTS 'elm'@'%' IDENTIFIED BY 'elm';
GRANT ALL PRIVILEGES ON elm.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_points.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_catalog.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_order.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_user.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_wallet.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_address.* TO 'elm'@'%';
GRANT ALL PRIVILEGES ON elm_merchant.* TO 'elm'@'%';
FLUSH PRIVILEGES;

USE elm_user;

-- 权限表
CREATE TABLE IF NOT EXISTS authority (
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户表
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

-- 用户权限关联表
CREATE TABLE IF NOT EXISTS user_authority (
    user_id BIGINT NOT NULL,
    authority_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, authority_name),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_authority_name FOREIGN KEY (authority_name) REFERENCES authority (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 初始化权限数据
INSERT IGNORE INTO authority (name) VALUES ('USER'), ('ADMIN'), ('BUSINESS');

