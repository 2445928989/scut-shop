#!/bin/bash
# ============================================================================
# 初始化管理员用户脚本
# 该脚本在 MySQL 容器初始化时执行，用于创建初始管理员账户
# ============================================================================

set -euo pipefail

# 获取环境变量
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:?需要设置 MYSQL_ROOT_PASSWORD}"
MYSQL_DATABASE="${MYSQL_DATABASE:=scut_shop}"
ADMIN_USERNAME="${ADMIN_USERNAME:=admin}"
ADMIN_EMAIL="${ADMIN_EMAIL:=admin@shop.local}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:=}"

# 如果未提供密码，则生成随机密码
if [ -z "${ADMIN_PASSWORD}" ]; then
  ADMIN_PASSWORD="$(openssl rand -base64 12)"
  GENERATED_PASSWORD=1
else
  GENERATED_PASSWORD=0
fi

# 注意：这里使用 SHA256 作为临时哈希（仅用于初始化）
# 当用户首次登录时，应用会自动使用 BCrypt 重新加密密码
PASSWORD_HASH=$(printf "%s" "${ADMIN_PASSWORD}" | sha256sum | awk '{print $1}')
STORED_HASH="sha256\$${PASSWORD_HASH}"

echo "[init_admin] 开始创建管理员账户 '${ADMIN_USERNAME}' (${ADMIN_EMAIL})"

# 执行 SQL 脚本创建管理员账户和分配角色
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" << EOSQL
-- 确保角色存在
INSERT INTO \`role\` (name, description) 
VALUES ('ROLE_ADMIN', 'Administrator') 
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO \`role\` (name, description) 
VALUES ('ROLE_USER', 'Default user') 
ON DUPLICATE KEY UPDATE name=name;

-- 创建或更新管理员用户
INSERT INTO \`user\` (username, email, password_hash, status, created_at, updated_at) 
VALUES ('${ADMIN_USERNAME}', '${ADMIN_EMAIL}', '${STORED_HASH}', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  email='${ADMIN_EMAIL}', 
  password_hash='${STORED_HASH}', 
  status=1, 
  updated_at=NOW();

-- 获取管理员用户 ID 和 ROLE_ADMIN ID
SET @admin_user_id = (SELECT id FROM \`user\` WHERE username='${ADMIN_USERNAME}' LIMIT 1);
SET @admin_role_id = (SELECT id FROM \`role\` WHERE name='ROLE_ADMIN' LIMIT 1);

-- 分配 ROLE_ADMIN 角色（如果还没有分配）
INSERT IGNORE INTO \`user_role\` (user_id, role_id) 
VALUES (@admin_user_id, @admin_role_id);

EOSQL

# 输出结果
if [ "${GENERATED_PASSWORD}" -eq 1 ]; then
  echo "[init_admin] ✅ 管理员 '${ADMIN_USERNAME}' 创建成功！"
  echo "[init_admin] ⚠️  生成的密码: ${ADMIN_PASSWORD}"
  echo "[init_admin] 📝 请妥善保管此密码，不会再显示。"
else
  echo "[init_admin] ✅ 管理员 '${ADMIN_USERNAME}' 创建成功！"
  echo "[init_admin] 📝 使用提供的密码: ${ADMIN_PASSWORD}"
fi

echo "[init_admin] 访问 http://localhost:3000/login 登录"
echo "[init_admin] 初始化完成。"
