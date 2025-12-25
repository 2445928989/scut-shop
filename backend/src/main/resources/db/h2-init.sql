-- Minimal schema for e2e tests (H2 compatible, MODE=MySQL)
-- Create both quoted (lowercase) and unquoted (uppercase) table names to be compatible with potential SQL quoting differences
CREATE TABLE IF NOT EXISTS "user" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  email VARCHAR(128) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS USER (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  email VARCHAR(128) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "role" (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL
);
CREATE TABLE IF NOT EXISTS "user_role" (
  user_id BIGINT NOT NULL,
  role_id INT NOT NULL
);
CREATE TABLE IF NOT EXISTS "product" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  sku VARCHAR(100),
  description TEXT,
  price DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
  stock INT NOT NULL DEFAULT 10,
  image_url VARCHAR(512),
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- insert products
DELETE FROM "product";
INSERT INTO "product" (
    name,
    sku,
    description,
    price,
    stock,
    image_url,
    status
  )
VALUES (
    'Apple iPhone 15 Pro Max (A3108) 256GB 原色钛金属',
    'JD-100067912345',
    '搭载 A17 Pro 芯片，首款采用航空级钛金属设计的 iPhone，配备 5 倍光学变焦潜望式长焦镜头，支持 USB-C 接口（USB 3 速度）。',
    8999.00,
    100,
    '/api/uploads/iphone.png',
    1
  ),
  (
    '小米 14 Ultra 徕卡光学 Summilux 镜头 16GB+512GB 黑色',
    'JD-100082345678',
    '徕卡全明星四摄，一英寸无级可变光圈主摄，骁龙 8 Gen 3 处理器，2K 旗舰高刷屏，支持双向卫星通信。',
    6499.00,
    80,
    '/api/uploads/xiaomi.webp',
    1
  ),
  (
    '华为 Mate 60 Pro 12GB+512GB 雅川青',
    'JD-100065432109',
    '自研麒麟 9000S 芯片，支持卫星通话，超可靠玄武架构，第二代昆仑玻璃，全焦段超清影像。',
    6999.00,
    50,
    '/api/uploads/huawei.jpg',
    1
  ),
  (
    'Apple MacBook Air 13.6英寸 M3 芯片 (8核中央处理器 10核图形处理器) 16G 512G',
    'JD-100091234567',
    '全新 M3 芯片，性能强劲且功耗极低，无风扇静音设计，Liquid 视网膜显示屏，支持两台外接显示器。',
    10499.00,
    30,
    '/api/uploads/mac.webp',
    1
  ),
  (
    '联想 拯救者 Y9000P 2024 游戏笔记本电脑 (i9-14900HX 16G 1T RTX4060 2.5K 240Hz)',
    'JD-100054321098',
    '专业电竞本，霜刃 Pro 散热系统 6.0，140W 满功耗显卡释放，支持显卡直连，高色域电竞屏。',
    9999.00,
    40,
    '/api/uploads/legion.jpg',
    1
  ),
  (
    '华硕 灵耀14 2024 14英寸超薄本 (Ultra 7 155H 32G 1T 2.8K 120Hz OLED)',
    'JD-100076543210',
    '轻约 1.19kg，薄约 13.9mm，搭载全新酷睿 Ultra 处理器，AI 性能大幅提升，75Wh 大容量电池。',
    7499.00,
    60,
    '/api/uploads/asus.jpg',
    1
  ),
  (
    '索尼 (SONY) WH-1000XM5 头戴式无线降噪耳机 黑色',
    'JD-100032109876',
    '双芯片驱动，8 麦克风降噪系统，支持 LDAC 高音质传输，30 小时长续航，佩戴感全面升级。',
    2299.00,
    150,
    '/api/uploads/sony.jpeg',
    1
  ),
  (
    'Apple AirPods Pro (第二代) 配 MagSafe 充电盒 (USB-C)',
    'JD-100043210987',
    'H2 芯片带来更智能的降噪性能和三维空间音效，主动降噪效果提升最高可达 2 倍，支持通透模式。',
    1899.00,
    200,
    '/api/uploads/airpods.png',
    1
  ),
  (
    '漫步者 (EDIFIER) W820NB 双金标版 头戴式主动降噪耳机',
    'JD-100056789012',
    'Hi-Res 双金标认证，-43dB 深度主动降噪，50 小时超长续航，支持游戏低延迟模式。',
    329.00,
    300,
    '/api/uploads/edifier.avif',
    1
  ),
  (
    '三只松鼠 每日坚果 750g/30袋 礼盒装',
    'JD-100001234567',
    '精选全球 6 种坚果 3 种果干，干湿分离包装，科学配比，营养均衡，老少皆宜。',
    139.00,
    1000,
    '/api/uploads/three_squirrels.avif',
    1
  ),
  (
    '乐事 (Lay\'s) 薯片 经典原味 135g*3袋 组合装',
    'JD-100002345678',
    '精选优质土豆，切片均匀，口感薄脆，经典原味，追剧必备零食。',
    35.90,
    2000,
    '/api/uploads/lays.png',
    1
  ),
  (
    '良品铺子 猪肉脯 靖江风味 500g 经典原味',
    'JD-100003456789',
    '精选猪后腿肉，传统工艺烘烤，肉质紧实，咸甜适口，独立小包装，方便携带。',
    49.90,
    800,
    '/api/uploads/bestore.webp',
    1
  ),
  (
    '维达 (Vinda) 抽纸 超韧3层100抽*24包 (整箱销售)',
    'JD-100004567890',
    '精选进口优质原生木浆，长短纤维紧密结合，湿水不易破，不含可迁移性荧光增白剂。',
    59.90,
    1500,
    '/api/uploads/vinda.jpg',
    1
  ),
  (
    '蓝月亮 洗衣液 亮白增艳 3kg*2瓶 组合装',
    'JD-100005678901',
    '深层洁净，亮白增艳，低泡易漂，温和不伤手，持久留香，适合各种衣物。',
    89.00,
    1200,
    '/api/uploads/bluemoon.png',
    1
  ),
  (
    '苏泊尔 (SUPOR) 电水壶 1.5L 双层防烫 304不锈钢',
    'JD-100006789012',
    '1500W 快速沸腾，双层防烫设计，优质温控器，自动断电保护，大口径易清洗。',
    99.00,
    500,
    '/api/uploads/supor.jpg',
    1
  );
,
(
  '良品铺子 猪肉脯 靖江风味 500g 经典原味',
  'JD-100003456789',
  '精选猪后腿肉，传统工艺烘烤，肉质紧实，咸甜适口，独立小包装，方便携带。',
  49.90,
  800,
  'https://picsum.photos/seed/pork/400/400',
  1
),
(
  '维达 (Vinda) 抽纸 超韧3层100抽*24包 (整箱销售)',
  'JD-100004567890',
  '精选进口优质原生木浆，长短纤维紧密结合，湿水不易破，不含可迁移性荧光增白剂。',
  59.90,
  1500,
  'https://picsum.photos/seed/tissue/400/400',
  1
),
(
  '蓝月亮 洗衣液 亮白增艳 3kg*2瓶 组合装',
  'JD-100005678901',
  '深层洁净，亮白增艳，低泡易漂，温和不伤手，持久留香，适合各种衣物。',
  89.00,
  1200,
  'https://picsum.photos/seed/detergent/400/400',
  1
),
(
  '苏泊尔 (SUPOR) 电水壶 1.5L 双层防烫 304不锈钢',
  'JD-100006789012',
  '1500W 快速沸腾，双层防烫设计，优质温控器，自动断电保护，大口径易清洗。',
  99.00,
  500,
  'https://picsum.photos/seed/kettle/400/400',
  1
);
-- product_audit for h2/init
CREATE TABLE IF NOT EXISTS "product_audit" (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  actor VARCHAR(128) NOT NULL,
  details CLOB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);