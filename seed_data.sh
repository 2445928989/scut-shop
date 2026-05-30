#!/bin/bash
# SCUT-Shop 大数据量测试脚本
# 用法: bash seed_data.sh [admin密码]
# 生成: 15用户 × 30天 × 每人3-8单 ≈ 2500+订单
set -e
API="http://127.0.0.1:3000/api"
ADMIN_PASS="${1:-Admin@2024}"
TOTAL=0

login() {
  curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])"
}

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

price_pid() {
  local r=$((RANDOM % 100))
  if   [ $r -lt 25 ]; then echo 1   # iPhone 25%
  elif [ $r -lt 48 ]; then echo 4   # MacBook 23%
  elif [ $r -lt 63 ]; then echo 5   # 拯救者 15%
  elif [ $r -lt 73 ]; then echo 2   # 小米 10%
  elif [ $r -lt 83 ]; then echo 3   # 华为 10%
  elif [ $r -lt 90 ]; then echo 6   # 华硕 7%
  elif [ $r -lt 96 ]; then echo 7   # 索尼 6%
  else echo 8                       # AirPods 4%
  fi
}

# ====== 15用户 ======
echo ">>> 1. 创建15个用户"
USERS=(alice bob carol dave eve frank grace henry ivy jack kate leo mia nick olivia)
CITIES=(广州 北京 上海 杭州 南京 深圳 成都 武汉 西安 重庆 苏州 天津 长沙 郑州 青岛)
PROVS=(广东 北京 上海 浙江 江苏 广东 四川 湖北 陕西 重庆 江苏 天津 湖南 河南 山东)
NU=${#USERS[@]}
TOKENS=()

for i in "${!USERS[@]}"; do
  curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
    -d "{\"username\":\"${USERS[$i]}\",\"email\":\"${USERS[$i]}@test.com\",\"password\":\"Test1234!\"}" > /dev/null 2>/dev/null || true
  TOKENS[$i]="$(login "${USERS[$i]}" 'Test1234!')"
  echo "  ${USERS[$i]}"
done

echo ">>> 2. 创建地址"
for i in "${!USERS[@]}"; do
  curl -s -X POST "$API/addresses" -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKENS[$i]}" \
    -d "{\"recipient\":\"${USERS[$i]}\",\"phone\":\"1380000$(printf '%04d' $i)\",\"province\":\"${PROVS[$i]}\",\"city\":\"${CITIES[$i]}\",\"district\":\"中心区\",\"detail\":\"街道${i}号\",\"isDefault\":1}" > /dev/null
done
echo "  done"

# ====== 30天每人3-8单 ======
echo ">>> 3. 日常订单 (30天, 每用户3-8单/天)"
for i in "${!USERS[@]}"; do
  for day in $(seq 0 29); do
    N=$((RANDOM % 6 + 3))
    for n in $(seq 1 $N); do
      PID=$(price_pid)
      QTY=$((RANDOM % 4 + 1))
      quick_order "${TOKENS[$i]}" $((i+1)) $PID $QTY
      TOTAL=$((TOTAL+1))
    done
  done
done
echo "  累计: $TOTAL"

# ====== 关联订单 ======
echo ">>> 4. 关联订单"
for i in $(seq 1 80); do
  quick_order "${TOKENS[$((RANDOM%NU))]}" $((RANDOM%NU+1)) 1 1 7 1; TOTAL=$((TOTAL+1))
done
for i in $(seq 1 50); do
  quick_order "${TOKENS[$((RANDOM%NU))]}" $((RANDOM%NU+1)) 1 1 8 1; TOTAL=$((TOTAL+1))
done
for i in $(seq 1 40); do
  quick_order "${TOKENS[$((RANDOM%NU))]}" $((RANDOM%NU+1)) 4 1 7 1 6 1; TOTAL=$((TOTAL+1))
done
for i in $(seq 1 40); do
  quick_order "${TOKENS[$((RANDOM%NU))]}" $((RANDOM%NU+1)) 2 1 5 1; TOTAL=$((TOTAL+1))
done
for i in $(seq 1 30); do
  quick_order "${TOKENS[$((RANDOM%NU))]}" $((RANDOM%NU+1)) 10 2 11 1 12 1; TOTAL=$((TOTAL+1))
done
echo "  累计: $TOTAL"

# ====== 周末高峰 ======
echo ">>> 5. 周末暴增 (每个周末每人+2单)"
for wknd in 5 6 12 13 19 20 26 27; do
  for i in "${!USERS[@]}"; do
    for r in 1 2; do
      PID=$(price_pid)
      QTY=$((RANDOM % 5 + 1))
      quick_order "${TOKENS[$i]}" $((i+1)) $PID $QTY
      TOTAL=$((TOTAL+1))
    done
  done
done
echo "  累计: $TOTAL"

# ====== 大额批发订单 ======
echo ">>> 6. 企业批发大单 (大数量)"
for i in $(seq 1 30); do
  quick_order "${TOKENS[$((RANDOM%NU))]}" $((RANDOM%NU+1)) 1 10 4 5 3 3
  TOTAL=$((TOTAL+1))
done
echo "  累计: $TOTAL"

# ====== 分散日期 ======
echo ">>> 7. 分散日期到30天"
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
echo ">>> 8. 统计"
ATOKEN=$(login "admin" "$ADMIN_PASS")
curl -s -H "Authorization: Bearer $ATOKEN" "$API/analytics/sales-stats?range=30" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(f'  总订单: {d[\"totalOrders\"]}  总销售额: ¥{d[\"totalSales\"]:,.0f}')
ds=d.get('dailySales',[])
if ds:
    amts=[float(r.get('amount',0)) for r in ds]
    print(f'  日均: ¥{sum(amts)/len(amts):,.0f}  最低: ¥{min(amts):,.0f}  最高: ¥{max(amts):,.0f}')
"
echo ">>> 完成"
