-- SCUT-Shop: 完整数据库初始化脚本
-- 包含 schema、权限用户、默认角色、管理员用户、示例商品数据
-- 该脚本会在 MySQL 容器初始化时执行一次
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
-- ============================================================================
-- 1. 建立数据库结构（schema）
-- ============================================================================
-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `email` VARCHAR(128) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=active,0=disabled',
  `activation_token` VARCHAR(128) DEFAULT NULL,
  `activation_expires` DATETIME DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表';
-- 角色表
CREATE TABLE IF NOT EXISTS `role` (
  `id` SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255),
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表';
-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `user_role` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `role_id` SMALLINT UNSIGNED NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关联表';
-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `sku` VARCHAR(100),
  `description` TEXT,
  `price` DECIMAL(19, 2) NOT NULL,
  `stock` INT UNSIGNED DEFAULT 0,
  `image_url` VARCHAR(512),
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=active,0=inactive/deleted',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_sku` (`sku`),
  KEY `idx_product_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品表';
-- 商品审计表（记录软删除和状态变更）
CREATE TABLE IF NOT EXISTS `product_audit` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `action` VARCHAR(50) NOT NULL COMMENT 'delete, restore, update, etc.',
  `actor` VARCHAR(128) COMMENT '执行人（通常是用户名或系统）',
  `details` JSON COMMENT '变更详情',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_product` (`product_id`),
  CONSTRAINT `fk_audit_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品操作审计表';
-- 购物车表（支持游客，user_id 可为空）
CREATE TABLE IF NOT EXISTS `cart` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED DEFAULT NULL,
  `guest_id` VARCHAR(128),
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cart_user` (`user_id`),
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '购物车表';
-- 购物车项目表
CREATE TABLE IF NOT EXISTS `cart_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `cart_id` BIGINT UNSIGNED NOT NULL,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `quantity` INT UNSIGNED NOT NULL DEFAULT 1,
  `price` DECIMAL(19, 2) COMMENT '添加时的商品价格快照',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cart_item_cart` (`cart_id`),
  KEY `idx_cart_item_product` (`product_id`),
  CONSTRAINT `fk_cart_item_cart` FOREIGN KEY (`cart_id`) REFERENCES `cart` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '购物车项目表';
-- 刷新令牌表（用于 JWT 刷新）
CREATE TABLE IF NOT EXISTS `refresh_token` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(500) NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `revoked` TINYINT NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_refresh_token_user` (`user_id`),
  KEY `idx_refresh_token_token` (`token`(100)),
  CONSTRAINT `fk_refresh_token_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'JWT刷新令牌表';
-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `order_no` VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
  `shipping_address_id` BIGINT UNSIGNED COMMENT '收货地址ID',
  `total_amount` DECIMAL(19, 2) NOT NULL COMMENT '订单总金额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=CREATED,1=PAID,2=SHIPPED,3=DELIVERED,4=CANCELLED,5=REFUNDED',
  `payment_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=UNPAID,1=PAID,2=REFUNDED',
  `remark` VARCHAR(255),
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_user` (`user_id`),
  KEY `idx_order_status` (`status`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单表';
-- 订单项目表
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `product_name` VARCHAR(255) NOT NULL COMMENT '订单创建时的商品名称快照',
  `price` DECIMAL(19, 2) NOT NULL COMMENT '订单创建时的商品价格',
  `quantity` INT UNSIGNED NOT NULL DEFAULT 1,
  `subtotal` DECIMAL(19, 2) NOT NULL COMMENT '小计（价格 × 数量）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_item_order` (`order_id`),
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单项目表';
-- ============================================================================
-- 支付表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `payment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `method` VARCHAR(64) NOT NULL COMMENT '支付方式：mock, alipay, wechat, stripe 等',
  `amount` DECIMAL(19, 2) NOT NULL COMMENT '支付金额',
  `status` INTEGER NOT NULL DEFAULT 0 COMMENT '0=INIT,1=SUCCESS,2=FAILED,3=REFUNDED',
  `transaction_no` VARCHAR(255) COMMENT '三方交易流水号',
  `paid_at` TIMESTAMP NULL COMMENT '支付完成时间',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payment_order` (`order_id`),
  CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '支付记录表';
-- 用户日志表
CREATE TABLE IF NOT EXISTS `user_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `action` VARCHAR(50) NOT NULL COMMENT 'BROWSE_PRODUCT, PURCHASE, etc.',
  `details` TEXT,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_log_user` (`user_id`),
  CONSTRAINT `fk_user_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户日志表';
SET FOREIGN_KEY_CHECKS = 1;
-- ============================================================================
-- 2. 初始化默认角色
-- ============================================================================
INSERT INTO `role` (name, description)
VALUES ('ROLE_USER', 'Default user') ON DUPLICATE KEY
UPDATE name = name;
INSERT INTO `role` (name, description)
VALUES ('ROLE_ADMIN', 'Administrator') ON DUPLICATE KEY
UPDATE name = name;
-- ============================================================================
-- 3. 初始化示例商品数据
-- ============================================================================
INSERT INTO product (
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
    '1500W 快速沸腾，双层隔热防烫设计，优质温控器，自动断电保护，大口径易清洗。',
    99.00,
    500,
    '/api/uploads/supor.jpg',
    1
  ) ON DUPLICATE KEY
UPDATE name =
VALUES(name),
  description =
VALUES(description),
  price =
VALUES(price),
  stock =
VALUES(stock),
  image_url =
VALUES(image_url),
  status =
VALUES(status);
-- ============================================================================
-- 4. 初始化默认管理员用户
-- ============================================================================
-- 【重要】初始管理员用户由 db/init_admin.sh 脚本创建
-- 该脚本在 MySQL 容器初始化时自动执行，从 .env 文件读取以下配置：
--   - ADMIN_USERNAME: 管理员用户名（默认: admin）
--   - ADMIN_EMAIL: 管理员邮箱（默认: admin@shop.local）
--   - ADMIN_PASSWORD: 管理员密码（默认: Admin@2024）
--
-- 初始密码使用 SHA256 哈希存储（仅用于初始化）
-- 用户首次登录时，应用会自动使用 BCrypt 重新加密密码
--
-- 如需修改管理员配置，编辑 .env 文件中的以下变量：
--   ADMIN_USERNAME=admin
--   ADMIN_EMAIL=admin@shop.local
--   ADMIN_PASSWORD=YourSecurePassword123!
-- 然后删除并重新创建容器：docker compose down -v && docker compose up -d
--
-- ============================================================================