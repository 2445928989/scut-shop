#!/bin/bash
# SCUT-Shop 测试数据生成脚本
# 用法: bash seed_data.sh [admin密码]
# 默认管理员密码: Admin@2024

set -e
API="http://127.0.0.1:3000/api"
ADMIN_PASS="${1:-Admin@2024}"
TOTAL=0

login() {
  curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])"
}

# 创建测试用户
echo ">>> 1. 创建用户"
USERS=(alice bob carol dave eve)
TOKENS=()
for u in "${USERS[@]}"; do
  curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
    -d "{\"username\":\"$u\",\"email\":\"${u}@test.com\",\"password\":\"Test1234!\"}" > /dev/null 2>/dev/null || true
  TOKENS+=("$(login "$u" 'Test1234!')")
  echo "  $u"
done

# 创建地址
echo ">>> 2. 地址"
CITIES=(广州 北京 上海 杭州 南京)
PROVS=(广东 北京 上海 浙江 江苏)
for i in "${!USERS[@]}"; do
  curl -s -X POST "$API/addresses" -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKENS[$i]}" \
    -d "{\"recipient\":\"${USERS[$i]}\",\"phone\":\"1380000000$i\",\"province\":\"${PROVS[$i]}\",\"city\":\"${CITIES[$i]}\",\"district\":\"中心区\",\"detail\":\"街道${i}号\",\"isDefault\":1}" > /dev/null
done
echo "  done"

# 随机下单（30天均匀分布）
echo ">>> 3. 随机订单"
for round in 1 2 3; do
  for i in "${!USERS[@]}"; do
    PID=$((RANDOM % 14 + 1))
    curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${TOKENS[$i]}" \
      -d "{\"productId\":$PID,\"quantity\":1}" > /dev/null
    curl -s -X POST "$API/orders/checkout" -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${TOKENS[$i]}" \
      -d "{\"paymentMethod\":\"mock\",\"addressId\":$((i+1))}" > /dev/null
    TOTAL=$((TOTAL+1))
  done
done
echo "  $TOTAL 笔"

# 关联订单
echo ">>> 4. 关联订单"
# 手机+耳机
for i in 1 2 3; do
  curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[0]}" -d '{"productId":1,"quantity":1}' > /dev/null
  curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[0]}" -d '{"productId":7,"quantity":1}' > /dev/null
  curl -s -X POST "$API/orders/checkout" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[0]}" -d '{"paymentMethod":"mock","addressId":1}' > /dev/null
  TOTAL=$((TOTAL+1))
done
# 电脑+耳机
for i in 1 2; do
  curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[1]}" -d '{"productId":4,"quantity":1}' > /dev/null
  curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[1]}" -d '{"productId":7,"quantity":1}' > /dev/null
  curl -s -X POST "$API/orders/checkout" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[1]}" -d '{"paymentMethod":"mock","addressId":2}' > /dev/null
  TOTAL=$((TOTAL+1))
done
# 零食组合
for i in 1 2 3; do
  curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[2]}" -d '{"productId":10,"quantity":1}' > /dev/null
  curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[2]}" -d '{"productId":11,"quantity":1}' > /dev/null
  curl -s -X POST "$API/orders/checkout" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[2]}" -d '{"paymentMethod":"mock","addressId":3}' > /dev/null
  TOTAL=$((TOTAL+1))
done
# 日用品
curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[4]}" -d '{"productId":13,"quantity":1}' > /dev/null
curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[4]}" -d '{"productId":14,"quantity":1}' > /dev/null
curl -s -X POST "$API/orders/checkout" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKENS[4]}" -d '{"paymentMethod":"mock","addressId":5}' > /dev/null
TOTAL=$((TOTAL+1))
echo "  done, 总计 $TOTAL 笔"

# 分散日期到过去30天
echo ">>> 5. 分散日期"
# 从 .env 读取密码，兜底用默认值
if [ -f .env ]; then
  DB_PASS=$(grep -oP 'MYSQL_PASSWORD=\K.*' .env | head -1)
fi
DB_PASS="${DB_PASS:-changeme_db_pw}"
echo "  using db password: ${DB_PASS:0:1}***"

# 分批更新，避免一次性处理太多
ORDER_COUNT=$(docker compose exec -T db mysql -u scut_user -p"$DB_PASS" scut_shop -N -e "SELECT COUNT(*) FROM \`order\`" 2>/dev/null)
echo "  orders: $ORDER_COUNT"

if [ -n "$ORDER_COUNT" ] && [ "$ORDER_COUNT" -gt 0 ]; then
  docker compose exec -T db mysql -u scut_user -p"$DB_PASS" scut_shop <<SQL 2>/dev/null
DROP TEMPORARY TABLE IF EXISTS t;
CREATE TEMPORARY TABLE t AS
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn, COUNT(*) OVER () AS total FROM \`order\`;
UPDATE \`order\` o JOIN t ON o.id = t.id
  SET o.created_at = DATE_SUB(NOW(), INTERVAL FLOOR(30 * t.rn / t.total) DAY),
      o.updated_at = DATE_SUB(NOW(), INTERVAL FLOOR(30 * t.rn / t.total) DAY);
SQL
  echo "  done"
else
  echo "  skip (no orders)"
fi

# 统计
echo ">>> 6. 统计"
ATOKEN=$(login "admin" "$ADMIN_PASS")
curl -s -H "Authorization: Bearer $ATOKEN" "$API/analytics/sales-stats?range=30" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(f'  总订单: {d[\"totalOrders\"]}  总销售额: ¥{d[\"totalSales\"]}')
ds=d.get('dailySales',[])
if ds: print(f'  日期: {ds[0][\"date\"]} ~ {ds[-1][\"date\"]}')
"
echo ">>> 完成"
