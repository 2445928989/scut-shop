#!/bin/bash
# SCUT-Shop 智能造数据脚本
# 用法: bash seed_data.sh [admin密码]
# 特点: 用户画像购买、自然共现、30用户×30天
set -e
API="http://127.0.0.1:3000/api"
ADMIN_PASS="${1:-Admin@2024}"
TOTAL=0

login() {
  curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])"
}

# 下单: make_order <token> <addr_id> pid1 qty1 pid2 qty2 ...
make_order() {
  local TOKEN=$1 ADDR=$2; shift 2
  while [ $# -gt 0 ]; do
    curl -s -X POST "$API/cart/items" -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" -d "{\"productId\":$1,\"quantity\":$2}" > /dev/null
    shift 2
  done
  curl -s -X POST "$API/orders/checkout" -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" -d "{\"paymentMethod\":\"mock\",\"addressId\":$ADDR}" > /dev/null
}

# ---- 补库存 ----
echo ">>> 0. 补充商品库存"
if [ -f .env ]; then
  DB_PASS=$(grep -oP 'MYSQL_PASSWORD=\K.*' .env | head -1)
fi
DB_PASS="${DB_PASS:-changeme_db_pw}"
docker compose exec -T db mysql -u scut_user -p"$DB_PASS" scut_shop -e "
  UPDATE product SET stock=99999;
  DELETE FROM cart_item; DELETE FROM cart;
  DELETE FROM order_item; DELETE FROM payment; DELETE FROM \`order\`;
  ALTER TABLE \`order\` AUTO_INCREMENT=1;
" 2>/dev/null
echo "  库存已补满, 旧订单已清理"

# ---- 用户和画像 ----
# 数码控(phone): 偏好1,2,3,7,8 (手机+耳机)    共8人
# 电脑党(pc):    偏好4,5,6,7,8 (电脑+耳机)    共8人
# 吃货(food):    偏好10,11,12 (零食)           共7人
# 居家(home):    偏好9,13,14,15 (日用+厨房)    共7人
declare -A PERSONA
USERS=()
CITIES=()
PROVS=()

add_user() { USERS+=("$1"); CITIES+=("$2"); PROVS+=("$3"); PERSONA["$1"]="$4"; }

# 数码控
add_user alice 广州 广东 phone; add_user bob 北京 北京 phone
add_user carol 深圳 广东 phone; add_user dave 成都 四川 phone
add_user emma 武汉 湖北 phone;  add_user fred 西安 陕西 phone
add_user gina 重庆 重庆 phone;  add_user hank 苏州 江苏 phone

# 电脑党
add_user ivy  上海 上海 pc;    add_user jack 杭州 浙江 pc
add_user kate 南京 江苏 pc;    add_user liam 天津 天津 pc
add_user maya 长沙 湖南 pc;    add_user noah 郑州 河南 pc
add_user opal 青岛 山东 pc;    add_user paul 福州 福建 pc

# 吃货
add_user quin 东莞 广东 food;  add_user rose 合肥 安徽 food
add_user sam  昆明 云南 food;  add_user tina 南宁 广西 food
add_user umar 大连 辽宁 food;  add_user vera 厦门 福建 food
add_user will 无锡 江苏 food

# 居家
add_user xena  佛山 广东 home; add_user yuri 温州 浙江 home
add_user zack  贵阳 贵州 home; add_user anna 南昌 江西 home
add_user bill  兰州 甘肃 home; add_user cici 太原 山西 home
add_user duke  惠州 广东 home

NU=${#USERS[@]}
echo ">>> 1. 创建${NU}个用户"
TOKENS=()
PWD='Test1234!'
for i in "${!USERS[@]}"; do
  curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
    -d "{\"username\":\"${USERS[$i]}\",\"email\":\"${USERS[$i]}@test.com\",\"password\":\"$PWD\"}" > /dev/null 2>/dev/null || true
  TOKENS[$i]="$(login "${USERS[$i]}" "$PWD")"
  [ $((i % 10)) -eq 9 ] && echo "  ${USERS[$i]}"
done
echo "  done"

echo ">>> 2. 创建地址"
for i in "${!USERS[@]}"; do
  curl -s -X POST "$API/addresses" -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKENS[$i]}" \
    -d "{\"recipient\":\"${USERS[$i]}\",\"phone\":\"138$(printf '%08d' $i)\",\"province\":\"${PROVS[$i]}\",\"city\":\"${CITIES[$i]}\",\"district\":\"中心区\",\"detail\":\"${CITIES[$i]}路${i}号\",\"isDefault\":1}" > /dev/null
done
echo "  done"

# ---- 画像商品池 ----
phone_pool=(1 2 3 7 8)    # iPhone 小米 华为 索尼 AirPods
pc_pool=(4 5 6 7 8)       # MacBook 拯救者 华硕 索尼 AirPods
food_pool=(10 11 12)       # 坚果 薯片 猪肉脯
home_pool=(9 13 14 15)     # 漫步者 维达 蓝月亮 苏泊尔

rand_from() { local arr=("$@"); echo "${arr[$((RANDOM % ${#arr[@]}))]}"; }

# ---- 日常购买 (30天) ----
echo ">>> 3. 日常购买 (基于画像, 30天)"
for day in $(seq 0 29); do
  for i in "${!USERS[@]}"; do
    p="${PERSONA[${USERS[$i]}]}"
    N=$((RANDOM % 4 + 1))  # 每天1-4单
    for n in $(seq 1 $N); do
      case $p in
        phone) PID=$(rand_from "${phone_pool[@]}") ;;
        pc)    PID=$(rand_from "${pc_pool[@]}") ;;
        food)  PID=$(rand_from "${food_pool[@]}") ;;
        home)  PID=$(rand_from "${home_pool[@]}") ;;
      esac
      QTY=$((RANDOM % 3 + 1))
      make_order "${TOKENS[$i]}" $((i+1)) $PID $QTY
      TOTAL=$((TOTAL+1))
    done
  done
done
echo "  累计: $TOTAL"

# ---- 关联多商品订单 ----
echo ">>> 4. 画像关联订单 (同类别多商品组合)"

# 数码控: 手机+耳机搭配 (85%共现率)
echo "  数码控: iPhone+索尼耳机 (高共现)"
for i in "${!USERS[@]}"; do
  [ "${PERSONA[${USERS[$i]}]}" != "phone" ] && continue
  for r in 1 2 3; do
    make_order "${TOKENS[$i]}" $((i+1)) 1 1 7 1; TOTAL=$((TOTAL+1))
  done
  for r in 1 2; do
    make_order "${TOKENS[$i]}" $((i+1)) 1 1 8 1; TOTAL=$((TOTAL+1))
  done
  make_order "${TOKENS[$i]}" $((i+1)) 2 1 3 1 7 1; TOTAL=$((TOTAL+1))
done

# 电脑党: MacBook+耳机 (75%共现率)
echo "  电脑党: MacBook+索尼耳机 (高共现)"
for i in "${!USERS[@]}"; do
  [ "${PERSONA[${USERS[$i]}]}" != "pc" ] && continue
  for r in 1 2 3; do
    make_order "${TOKENS[$i]}" $((i+1)) 4 1 7 1; TOTAL=$((TOTAL+1))
  done
  make_order "${TOKENS[$i]}" $((i+1)) 4 1 5 1 6 1; TOTAL=$((TOTAL+1))
  make_order "${TOKENS[$i]}" $((i+1)) 5 1 7 1; TOTAL=$((TOTAL+1))
done

# 吃货: 坚果+薯片+猪肉脯 (70%共现率)
echo "  吃货: 零食组合"
for i in "${!USERS[@]}"; do
  [ "${PERSONA[${USERS[$i]}]}" != "food" ] && continue
  for r in 1 2 3; do
    make_order "${TOKENS[$i]}" $((i+1)) 10 2 11 1; TOTAL=$((TOTAL+1))
  done
  for r in 1 2; do
    make_order "${TOKENS[$i]}" $((i+1)) 10 1 12 1; TOTAL=$((TOTAL+1))
  done
done

# 居家: 日用品搭配
echo "  居家: 日用品组合"
for i in "${!USERS[@]}"; do
  [ "${PERSONA[${USERS[$i]}]}" != "home" ] && continue
  for r in 1 2 3; do
    make_order "${TOKENS[$i]}" $((i+1)) 13 2 14 1; TOTAL=$((TOTAL+1))
  done
  for r in 1 2; do
    make_order "${TOKENS[$i]}" $((i+1)) 14 1 15 1; TOTAL=$((TOTAL+1))
  done
done

# ---- 跨类购买 (低共现, 制造差异) ----
echo ">>> 5. 跨类购买 (制造差异)"
# 每人偶尔买点别的
for i in "${!USERS[@]}"; do
  for r in 1 2; do
    PID=$((RANDOM % 15 + 1))
    make_order "${TOKENS[$i]}" $((i+1)) $PID 1; TOTAL=$((TOTAL+1))
  done
done
echo "  累计: $TOTAL"

# ---- 周末高峰 ----
echo ">>> 6. 周末购物高峰"
for wknd in 5 6 12 13 19 20 26 27; do
  for i in "${!USERS[@]}"; do
    p="${PERSONA[${USERS[$i]}]}"
    case $p in
      phone) PID=$(rand_from "${phone_pool[@]}") ;;
      pc)    PID=$(rand_from "${pc_pool[@]}") ;;
      food)  PID=$(rand_from "${food_pool[@]}") ;;
      home)  PID=$(rand_from "${home_pool[@]}") ;;
    esac
    QTY=$((RANDOM % 5 + 1))
    make_order "${TOKENS[$i]}" $((i+1)) $PID $QTY; TOTAL=$((TOTAL+1))
  done
done
echo "  累计: $TOTAL"

# ---- 分散日期 ----
echo ">>> 7. 分散日期到30天"
docker compose exec -T db mysql -u scut_user -p"$DB_PASS" scut_shop -e "
DROP TEMPORARY TABLE IF EXISTS t;
CREATE TEMPORARY TABLE t AS
  SELECT id, ROW_NUMBER() OVER (ORDER BY id)-1 rn, COUNT(*) OVER () total FROM \`order\`;
UPDATE \`order\` o JOIN t ON o.id=t.id
  SET created_at=DATE_SUB(NOW(), INTERVAL FLOOR(30*t.rn/t.total) DAY),
      updated_at=DATE_SUB(NOW(), INTERVAL FLOOR(30*t.rn/t.total) DAY);
" 2>/dev/null
echo "  done"

# ---- 统计 ----
echo ">>> 8. 数据统计"
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
echo ""
echo ">>> 推荐测试 (iPhone=1)"
curl -s "$API/recommend/together/1?limit=4" | python3 -c "
import sys,json
for r in json.load(sys.stdin):
    print(f'  {r[\"name\"][:25]}... {r[\"coPercent\"]}%')
" 2>/dev/null || echo "  (暂无)"

echo ""
echo ">>> 完成"
