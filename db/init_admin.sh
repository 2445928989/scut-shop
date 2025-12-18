#!/bin/bash
set -euo pipefail

# 初始化管理员脚本
# 使用环境变量：ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD（可留空以自动生成）
# 该脚本会在 MySQL 容器初始化时被执行，执行输出会出现在容器日志中（查看：docker compose logs db）

: "${MYSQL_ROOT_PASSWORD:?Need MYSQL_ROOT_PASSWORD env var}"
: "${MYSQL_DATABASE:=scut_shop}"
: "${ADMIN_USERNAME:=admin}"
: "${ADMIN_EMAIL:=admin@example.com}"
: "${ADMIN_PASSWORD:=}"
: "${ADMIN_ROLE_NAME:=ROLE_ADMIN}"

# 如果未提供密码则生成一个随机密码
if [ -z "${ADMIN_PASSWORD}" ]; then
  ADMIN_PASSWORD="$(openssl rand -base64 12)"
  GENERATED_PASSWORD=1
else
  GENERATED_PASSWORD=0
fi

# 使用 SHA256 做初始存储（请在应用层强制首次登录后更改为 bcrypt）
PASSWORD_HASH=$(printf "%s" "${ADMIN_PASSWORD}" | sha256sum | awk '{print $1}')
STORED_HASH="sha256\$${PASSWORD_HASH}"

echo "[init_admin] Creating admin user '${ADMIN_USERNAME}' (${ADMIN_EMAIL}) in database '${MYSQL_DATABASE}'"

cat <<-EOSQL | mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}"
INSERT INTO `role` (name, description) VALUES ('${ADMIN_ROLE_NAME}', 'Initial admin role') ON DUPLICATE KEY UPDATE name=name;
SET @role_id = (SELECT id FROM `role` WHERE name='${ADMIN_ROLE_NAME}' LIMIT 1);
INSERT INTO `user` (username, email, password_hash, status) VALUES ('${ADMIN_USERNAME}', '${ADMIN_EMAIL}', '${STORED_HASH}', 1)
  ON DUPLICATE KEY UPDATE email=VALUES(email), password_hash=VALUES(password_hash), status=1;
SET @user_id = (SELECT id FROM `user` WHERE username='${ADMIN_USERNAME}' LIMIT 1);
INSERT IGNORE INTO `user_role` (user_id, role_id) VALUES (@user_id, @role_id);
EOSQL

if [ "${GENERATED_PASSWORD}" -eq 1 ]; then
  echo "[init_admin] Generated admin password for user '${ADMIN_USERNAME}': ${ADMIN_PASSWORD}"
else
  echo "[init_admin] Admin '${ADMIN_USERNAME}' created/updated with provided password."
fi

echo "[init_admin] Done initializing admin user."
