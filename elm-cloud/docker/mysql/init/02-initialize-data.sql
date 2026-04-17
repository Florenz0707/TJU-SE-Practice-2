-- =============================================
-- Elm Cloud 数据库初始化脚本
-- 仅初始化用户数据，其他表让 JPA 自动创建
-- =============================================

USE elm_user;

-- =============================================
-- 初始化用户数据
-- =============================================

-- 密码说明：
-- admin -> adminadmin (BCrypt hash: $2a$08$lDnHPz7eUkSi6ao14Twuau08mzhWrL4kyZGGU5xfiGALO/Vxd5DOi)
-- 其他账号先设置为 111111，登录 admin 后可手动修改(BCrypt hash: $2a$10$L1h4OoCczPO7snWAqU6ssewNJLtzP.QRQH3wc.KOYh2CeKRrtqSxG)

-- 插入用户数据
INSERT IGNORE INTO `user` (id, username, password, activated, deleted, create_time, update_time) 
VALUES 
(1, 'admin', '$2a$08$lDnHPz7eUkSi6ao14Twuau08mzhWrL4kyZGGU5xfiGALO/Vxd5DOi', 1, 0, NOW(), NOW()),
(2, 'business1', '$2a$10$L1h4OoCczPO7snWAqU6ssewNJLtzP.QRQH3wc.KOYh2CeKRrtqSxG', 1, 0, NOW(), NOW()),
(3, 'business2', '$2a$10$L1h4OoCczPO7snWAqU6ssewNJLtzP.QRQH3wc.KOYh2CeKRrtqSxG', 1, 0, NOW(), NOW()),
(4, 'user1', '$2a$10$L1h4OoCczPO7snWAqU6ssewNJLtzP.QRQH3wc.KOYh2CeKRrtqSxG', 1, 0, NOW(), NOW()),
(5, 'user2', '$2a$10$L1h4OoCczPO7snWAqU6ssewNJLtzP.QRQH3wc.KOYh2CeKRrtqSxG', 1, 0, NOW(), NOW());

-- 分配权限
INSERT IGNORE INTO user_authority (user_id, authority_name) 
VALUES 
(1, 'ADMIN'),
(1, 'USER'),
(1, 'BUSINESS'),
(2, 'BUSINESS'),
(2, 'USER'),
(3, 'BUSINESS'),
(3, 'USER'),
(4, 'USER'),
(5, 'USER');

-- =============================================
-- 初始化个人信息数据
-- =============================================

-- 插入个人信息数据
INSERT IGNORE INTO person (id, first_name, last_name, email, phone, gender, create_time, update_time) 
VALUES 
(1, '管', '理员', 'admin@example.com', '13800000001', 'MALE', NOW(), NOW()),
(2, '商', '家1', 'business1@example.com', '13800000002', 'MALE', NOW(), NOW()),
(3, '商', '家2', 'business2@example.com', '13800000003', 'FEMALE', NOW(), NOW()),
(4, '用', '户1', 'user1@example.com', '13800000004', 'MALE', NOW(), NOW()),
(5, '用', '户2', 'user2@example.com', '13800000005', 'FEMALE', NOW(), NOW());

-- =============================================
-- 初始化完成
-- =============================================
