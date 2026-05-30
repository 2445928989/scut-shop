#!/bin/bash
# SCUT-Shop 测试数据生成脚本
# 用法: bash seed_data.sh [admin密码]
set -e
API="http://127.0.0.1:3000/api"
ADMIN_PASS="${1:-Admin@2024}"
TOTAL=0

login() {
  curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])"
}

# 快速下单: quick_order <token> <addr_id> <pid1> <qty1> [pid2] [qty2] ...
quick_order() {
  local TOKEN=$1 ADDR=$2; shift 2
  while [ $# -gt 0 ]; do
    curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" -d "{\"productId\":$1,\"quantity\":$2}" > /dev/null
    shift 2
  done
  curl -s -X POST "$API/orders/checkout" -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" -d "{\"paymentMethod\":\"mock\",\"addressId\":$ADDR}" > /dev/null
}

# 贵商品随机: 偏向前9个高价商品(1-6价格¥6k-10k, 7-9价格¥1k-2k)
price_pid() {
  local r=$((RANDOM % 100))
  if   [ $r -lt 30 ]; then echo 1   # iPhone  ¥8999  30%
  elif [ $r -lt 50 ]; then echo 4   # MacBook ¥10499 20%
  elif [ $r -lt 65 ]; then echo 5   # 拯救者  ¥9999  15%
  elif [ $r -lt 75 ]; then echo 2   # 小米    ¥6499  10%
  elif [ $r -lt 85 ]; then echo 3   # 华为    ¥6999  10%
  elif [ $r -lt 92 ]; then echo 6   # 华硕    ¥7499  7%
  elif [ $r -lt 97 ]; then echo 7   # 索尼耳机 ¥2299 5%
  else echo 8                       # AirPods  ¥1899 3%
  fi
}

echo ">>> 1. 创建用户"
USERS=(alice bob carol dave eve)
TOKENS=()
for u in "${USERS[@]}"; do
  curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
    -d "{\"username\":\"$u\",\"email\":\"${u}@test.com\",\"password\":\"Test1234!\"}" > /dev/null 2>/dev/null || true
  TOKENS+=("$(login "$u" 'Test1234!')")
  echo "  $u"
done

echo ">>> 2. 地址"
C=(广州 北京 上海 杭州 南京)
P=(广东 北京 上海 浙江 江苏)
for i in "${!USERS[@]}"; do
  curl -s -X POST "$API/addresses" -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKENS[$i]}" \
    -d "{\"recipient\":\"${USERS[$i]}\",\"phone\":\"1380000000$i\",\"province\":\"${P[$i]}\",\"city\":\"${C[$i]}\",\"district\":\"中心区\",\"detail\":\"街道${i}号\",\"isDefault\":1}" > /dev/null
done && echo "  done"

# ====== 每天每人2-4单，持续30天 ======
echo ">>> 3. 日常订单 (30天, 每日每人1-3单)"
for day in $(seq 0 29); do
  for i in "${!USERS[@]}"; do
    N=$((RANDOM % 3 + 1))
    for n in $(seq 1 $N); do
      PID=$(price_pid)
      QTY=$((RANDOM % 3 + 1))
      quick_order "${TOKENS[$i]}" $((i+1)) $PID $QTY
      TOTAL=$((TOTAL+1))
    done
  done
done
echo "  $TOTAL 笔"

# ====== 高价值多商品关联订单 ======
echo ">>> 4. 关联订单"

# iPhone + 配件 (高频)
for i in $(seq 1 15); do
  quick_order "${TOKENS[$((RANDOM%5))]}" $((RANDOM%5+1)) 1 1 7 1
  TOTAL=$((TOTAL+1))
done
# iPhone + AirPods
for i in $(seq 1 10); do
  quick_order "${TOKENS[$((RANDOM%5))]}" $((RANDOM%5+1)) 1 1 8 1
  TOTAL=$((TOTAL+1))
done
# MacBook + 耳机 + 华硕
for i in $(seq 1 8); do
  quick_order "${TOKENS[$((RANDOM%5))]}" $((RANDOM%5+1)) 4 1 7 1 6 1
  TOTAL=$((TOTAL+1))
done
# 小米 + 拯救者 (游戏套装)
for i in $(seq 1 8); do
  quick_order "${TOKENS[$((RANDOM%5))]}" $((RANDOM%5+1)) 2 1 5 1
  TOTAL=$((TOTAL+1))
done
# 华为 + 华硕 (商务套装)
for i in $(seq 1 6); do
  quick_order "${TOKENS[$((RANDOM%5))]}" $((RANDOM%5+1)) 3 1 6 1
  TOTAL=$((TOTAL+1))
done
# 零食三件套
for i in $(seq 1 10); do
  quick_order "${TOKENS[$((RANDOM%5))]}" $((RANDOM%5+1)) 10 2 11 1 12 1
  TOTAL=$((TOTAL+1))
done
# 日用品 + 厨房
for i in $(seq 1 8); do
  quick_order "${TOKENS[$((RANDOM%5))]}" $((RANDOM%5+1)) 13 2 14 1 15 1
  TOTAL=$((TOTAL+1))
done
echo "  关联: ${TOTAL} 笔"

# ====== 周末加量 (模拟真实购物高峰) ======
echo ">>> 5. 周末高峰"
# days 5,6,12,13,19,20,26,27 是周末
for wknd in 5 6 12 13 19 20 26 27; do
  for i in "${!USERS[@]}"; do
    PID=$(price_pid)
    QTY=$((RANDOM % 4 + 1))
    quick_order "${TOKENS[$i]}" $((i+1)) $PID $QTY
    TOTAL=$((TOTAL+1))
  done
done
echo "  总计: $TOTAL 笔"

# ====== 分散日期 ======
echo ">>> 6. 分散日期"
if [ -f .env ]; then
  DB_PASS=$(grep -oP 'MYSQL_PASSWORD=\K.*' .env | head -1)
fi
DB_PASS="${DB_PASS:-changeme_db_pw}"

docker compose exec -T db mysql -u scut_user -p"$DB_PASS" scut_shop -e "
DROP TEMPORARY TABLE IF EXISTS t;
CREATE TEMPORARY TABLE t AS
  SELECT id, ROW_NUMBER() OVER (ORDER BY id)-1 rn, COUNT(*) OVER () total FROM \`order\`;
UPDATE \`order\` o JOIN t ON o.id=t.id
  SET created_at=DATE_SUB(NOW(), INTERVAL FLOOR(30*t.rn/t.total) DAY),
      updated_at=DATE_SUB(NOW(), INTERVAL FLOOR(30*t.rn/t.total) DAY);
" 2>/dev/null
echo "  done"

# ====== 统计 ======
echo ">>> 7. 统计"
ATOKEN=$(login "admin" "$ADMIN_PASS")
curl -s -H "Authorization: Bearer $ATOKEN" "$API/analytics/sales-stats?range=30" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(f'  总订单: {d[\"totalOrders\"]}  总销售额: ¥{d[\"totalSales\"]}')
ds=d.get('dailySales',[])
if ds:
    amounts=[float(r['amount']) for r in ds]
    print(f'  日均: ¥{sum(amounts)/len(amounts):.0f}  最低: ¥{min(amounts):.0f}  最高: ¥{max(amounts):.0f}')
    print(f'  日期: {ds[0][\"date\"]} ~ {ds[-1][\"date\"]}')
"
echo ">>> 完成"
