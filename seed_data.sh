#!/bin/bash
# ============================================================
# SCUT-Shop 测试数据生成脚本
# 用法: bash seed_data.sh
# 前提: docker compose up -d 已启动
# ============================================================
set -e

API="${API:-http://127.0.0.1:3000/api}"
C="curl -s --noproxy '*'"
TOTAL_ORDERS=0

# 颜色输出
green() { echo -e "\033[32m$1\033[0m"; }
blue()  { echo -e "\033[34m$1\033[0m"; }
yellow(){ echo -e "\033[33m$1\033[0m"; }

# 登录函数
login() {
  echo '{"username":"'$1'","password":"'$2'"}' | \
    $C -X POST -H "Content-Type: application/json" -d @- "$API/auth/login" | \
    python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])" 2>/dev/null
}

# 下单函数: make_order <token> <address_id> <product_ids...>
make_order() {
  local TOKEN=$1; local ADDR=$2; shift 2
  for pid in "$@"; do
    $C -X POST "$API/cart/items" -H "Content-Type: application/json" \
       -H "Authorization: Bearer $TOKEN" -d "{\"productId\":$pid,\"quantity\":1}" > /dev/null
  done
  $C -X POST "$API/orders/checkout" -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" -d "{\"paymentMethod\":\"mock\",\"addressId\":$ADDR}" > /dev/null
  TOTAL_ORDERS=$((TOTAL_ORDERS + 1))
}

# ============================================================
blue "====== SCUT-Shop 测试数据生成 ======"
echo ""

# ---- 1. 创建用户 ----
green ">>> 1. 创建测试用户"
USERS=("alice" "bob" "carol" "dave" "eve")
EMAILS=("alice@test.com" "bob@test.com" "carol@test.com" "dave@test.com" "eve@test.com")
PASS="Test1234!"
PROVINCES=("广东" "北京" "上海" "浙江" "江苏")
CITIES=("广州" "北京" "上海" "杭州" "南京")
TOKENS=()

for i in "${!USERS[@]}"; do
  $C -X POST "$API/auth/register" -H "Content-Type: application/json" \
     -d "{\"username\":\"${USERS[$i]}\",\"email\":\"${EMAILS[$i]}\",\"password\":\"$PASS\"}" > /dev/null 2>&1 || true
  TOKENS[$i]=$(login "${USERS[$i]}" "$PASS")
  echo "  ✓ ${USERS[$i]}"
done

# ---- 2. 创建地址 ----
green ">>> 2. 创建收货地址"
for i in "${!USERS[@]}"; do
  $C -X POST "$API/addresses" -H "Content-Type: application/json" \
     -H "Authorization: Bearer ${TOKENS[$i]}" \
     -d "{\"recipient\":\"${USERS[$i]}\",\"phone\":\"1380000000$i\",\"province\":\"${PROVINCES[$i]}\",\"city\":\"${CITIES[$i]}\",\"district\":\"中心区\",\"detail\":\"街道${i}号\",\"isDefault\":1}" > /dev/null
  echo "  ✓ ${USERS[$i]} → ${PROVINCES[$i]} ${CITIES[$i]}"
done

# ---- 3. 单品随机订单（每日均匀分布） ----
green ">>> 3. 生成每日随机订单（30天）"
for i in $(seq 1 3); do  # 每天 1~3 单
  for u in "${!USERS[@]}"; do
    PID=$((RANDOM % 14 + 1))
    make_order "${TOKENS[$u]}" $((u+1)) $PID
  done
done
echo "  ✓ $TOTAL_ORDERS 笔"

# ---- 4. 多样化关联订单 ----
green ">>> 4. 生成关联购买订单"

# 手机配件组合: iPhone + 耳机（高频共现）
blue "  手机配件..."
for i in 1 2 3; do make_order "${TOKENS[0]}" 1 1 7; done  # alice: iPhone+索尼耳机 x3
make_order "${TOKENS[1]}" 2 1 7                            # bob
make_order "${TOKENS[2]}" 3 1 7                            # carol
make_order "${TOKENS[0]}" 1 1 8                            # alice: iPhone+AirPods
make_order "${TOKENS[1]}" 2 1 8                            # bob
make_order "${TOKENS[4]}" 5 1 7                            # eve: iPhone+索尼

# 电脑配件组合
blue "  电脑配件..."
for i in 1 2; do make_order "${TOKENS[1]}" 2 4 7; done     # bob: MacBook+索尼耳机 x2
make_order "${TOKENS[2]}" 3 4 8                            # carol: MacBook+AirPods
make_order "${TOKENS[3]}" 4 4 5                            # dave: MacBook+拯救者

# 零食组合
blue "  零食组合..."
for i in 1 2 3; do make_order "${TOKENS[2]}" 3 10 11; done # carol: 坚果+薯片 x3
for i in 1 2;   do make_order "${TOKENS[3]}" 4 10 12; done # dave: 坚果+猪肉脯 x2
make_order "${TOKENS[4]}" 5 11 12                            # eve: 薯片+猪肉脯

# 日用品组合
blue "  日用品..."
for i in 1 2; do make_order "${TOKENS[4]}" 5 13 14; done   # eve: 抽纸+洗衣液 x2
make_order "${TOKENS[0]}" 1 13 15                            # alice: 抽纸+电水壶

echo "  ✓ 关联订单创建完成"

# ---- 5. 订单日期分散到过去30天 ----
green ">>> 5. 分散订单日期"
docker.exe compose -f "$(dirname "$0")/docker-compose.yml" exec -T db \
  mysql -u scut_user -pchangeme_db_pw scut_shop -e "
DROP TEMPORARY TABLE IF EXISTS tmp_seed_orders;
CREATE TEMPORARY TABLE tmp_seed_orders AS
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 as rn, COUNT(*) OVER () as total
  FROM \`order\`;
UPDATE \`order\` o JOIN tmp_seed_orders t ON o.id = t.id
SET o.created_at = DATE_SUB(NOW(), INTERVAL FLOOR(30 * t.rn / t.total) DAY),
    o.updated_at = DATE_SUB(NOW(), INTERVAL FLOOR(30 * t.rn / t.total) DAY);
" 2>/dev/null
echo "  ✓ 日期已分散"

# ---- 6. 最终统计 ----
ADMIN_TOKEN=$(login "admin" "Admin@2024")
green ">>> 6. 数据统计"
echo ""
$C -H "Authorization: Bearer $ADMIN_TOKEN" "$API/analytics/sales-stats?range=30" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(f'  总订单: {d[\"totalOrders\"]}')
print(f'  总销售额: ¥{d[\"totalSales\"]}')
ds = d.get('dailySales',[])
if ds:
    amt_first = ds[0].get('amount',0)
    amt_last  = ds[-1].get('amount',0)
    print(f'  日期范围: {ds[0][\"date\"]} ~ {ds[-1][\"date\"]}')
" 2>/dev/null

echo ""
yellow "====== 完成！访问 http://localhost:3000 ======"
