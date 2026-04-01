CREATE DATABASE IF NOT EXISTS elm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_points CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_account CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_catalog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_wallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_address CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS elm_merchant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE elm_user;
INSERT INTO authority (name) VALUES ('USER'), ('ADMIN'), ('BUSINESS');
INSERT INTO user (id, username, password, activated) VALUES (1, 'admin', '$2a$10$X2ngW1dCn.osyqZUXksi1.88repVt9jc1N2YW9kfB/69nozB1PJs.', 1);
INSERT INTO user_authority (user_id, authority_name) VALUES (1, 'ADMIN'), (1, 'USER');

