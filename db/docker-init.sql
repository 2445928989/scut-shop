-- 初始化数据库用户与权限脚本
-- 该脚本会与 schema.sql 一起在 MySQL 容器初始化时执行

-- 创建应用用户（仅示例，生产请使用更强的密码并通过 Secret 管理）
CREATE USER IF NOT EXISTS 'scut_user'@'%' IDENTIFIED BY 'change_me_db_pw';
GRANT SELECT, INSERT, UPDATE, DELETE ON scut_shop.* TO 'scut_user'@'%';

-- 创建迁移用户（用于 Flyway/Liquibase，授予 DDL 权限）
CREATE USER IF NOT EXISTS 'scut_migrate'@'%' IDENTIFIED BY 'change_me_migrate_pw';
GRANT ALL PRIVILEGES ON scut_shop.* TO 'scut_migrate'@'%';

-- Ensure default roles exist
INSERT INTO role (name, description) VALUES ('ROLE_USER', 'Default user') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO role (name, description) VALUES ('ROLE_ADMIN', 'Administrator') ON DUPLICATE KEY UPDATE name=name;

FLUSH PRIVILEGES;
