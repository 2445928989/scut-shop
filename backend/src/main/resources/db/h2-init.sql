-- H2 compatible schema (MODE=MySQL) for e2e tests
CREATE TABLE IF NOT EXISTS "user" (
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
CREATE TABLE IF NOT EXISTS "category" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  sort_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "address" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  recipient VARCHAR(100) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  province VARCHAR(50) DEFAULT NULL,
  city VARCHAR(50) DEFAULT NULL,
  district VARCHAR(50) DEFAULT NULL,
  detail VARCHAR(500) NOT NULL,
  is_default TINYINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "product" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  sku VARCHAR(100),
  description TEXT,
  price DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
  stock INT NOT NULL DEFAULT 10,
  category_id BIGINT DEFAULT NULL,
  image_url VARCHAR(512),
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "product_audit" (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  actor VARCHAR(128) NOT NULL,
  details CLOB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "cart" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL,
  guest_id VARCHAR(128),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "cart_item" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cart_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT DEFAULT 1,
  price DECIMAL(19, 2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "refresh_token" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(500) NOT NULL,
  user_id BIGINT NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  revoked TINYINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "order" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  shipping_address_id BIGINT,
  total_amount DECIMAL(19, 2) NOT NULL,
  status TINYINT DEFAULT 0,
  payment_status TINYINT DEFAULT 0,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "order_item" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  price DECIMAL(19, 2) NOT NULL,
  quantity INT DEFAULT 1,
  subtotal DECIMAL(19, 2) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "payment" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  method VARCHAR(64) NOT NULL,
  amount DECIMAL(19, 2) NOT NULL,
  status INT DEFAULT 0,
  transaction_no VARCHAR(255),
  paid_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "user_log" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  action VARCHAR(50) NOT NULL,
  details TEXT,
  ip_address VARCHAR(45) DEFAULT NULL,
  duration_seconds INT DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "login_log" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  ip_address VARCHAR(45) DEFAULT NULL,
  user_agent VARCHAR(500) DEFAULT NULL,
  login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS "operation_log" (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  action VARCHAR(100) NOT NULL,
  target_type VARCHAR(50) DEFAULT NULL,
  target_id BIGINT DEFAULT NULL,
  details TEXT,
  ip_address VARCHAR(45) DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert roles
INSERT INTO "role" (name) VALUES ('ROLE_USER');
INSERT INTO "role" (name) VALUES ('ROLE_ADMIN');
INSERT INTO "role" (name) VALUES ('ROLE_SALES');

-- Insert categories
INSERT INTO "category" (id, name, parent_id, sort_order) VALUES (1, '手机数码', NULL, 1);
INSERT INTO "category" (id, name, parent_id, sort_order) VALUES (2, '电脑办公', NULL, 2);
INSERT INTO "category" (id, name, parent_id, sort_order) VALUES (3, '耳机音箱', NULL, 3);
INSERT INTO "category" (id, name, parent_id, sort_order) VALUES (4, '食品零食', NULL, 4);
INSERT INTO "category" (id, name, parent_id, sort_order) VALUES (5, '日用百货', NULL, 5);
INSERT INTO "category" (id, name, parent_id, sort_order) VALUES (6, '厨房家电', NULL, 6);

-- Insert sample products
INSERT INTO "product" (name, sku, description, price, stock, category_id, image_url, status) VALUES
('Apple iPhone 15 Pro Max', 'JD-100067912345', 'A17 Pro芯片，钛金属设计', 8999.00, 100, 1, '/api/uploads/iphone.png', 1),
('小米 14 Ultra', 'JD-100082345678', '徕卡全明星四摄', 6499.00, 80, 1, '/api/uploads/xiaomi.webp', 1),
('华为 Mate 60 Pro', 'JD-100065432109', '麒麟9000S芯片，卫星通话', 6999.00, 50, 1, '/api/uploads/huawei.jpg', 1),
('Apple MacBook Air 13.6 M3', 'JD-100091234567', 'M3芯片，无风扇设计', 10499.00, 30, 2, '/api/uploads/mac.webp', 1),
('联想拯救者 Y9000P', 'JD-100054321098', 'i9-14900HX, RTX4060', 9999.00, 40, 2, '/api/uploads/legion.jpg', 1),
('索尼 WH-1000XM5', 'JD-100032109876', '双芯片降噪', 2299.00, 150, 3, '/api/uploads/sony.jpeg', 1),
('三只松鼠 每日坚果', 'JD-100001234567', '6种坚果3种果干', 139.00, 1000, 4, '/api/uploads/three_squirrels.avif', 1),
('维达抽纸 24包', 'JD-100004567890', '超韧3层', 59.90, 1500, 5, '/api/uploads/vinda.jpg', 1),
('苏泊尔电水壶', 'JD-100006789012', '1.5L双层防烫', 99.00, 500, 6, '/api/uploads/supor.jpg', 1);
