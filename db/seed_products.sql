-- Seeds for E2E and manual testing: a variety of products
-- Ensure we use UTF-8 for these inserts
SET NAMES utf8mb4;
-- Uses ON DUPLICATE KEY UPDATE so the script is safe to re-run

INSERT INTO product (name, sku, description, price, stock, image_url, status) VALUES
('Blue T-Shirt', 'TS-BLUE-001', '柔软纯棉短袖 T 恤，蓝色，适合日常穿着。材质：100% 棉。', 19.99, 50, 'https://example.com/images/ts-blue.jpg', 1),
('Wireless Mouse', 'WM-001', '人体工学无线鼠标，支持双模（蓝牙/2.4GHz），带可调 DPI。', 29.95, 150, 'https://example.com/images/wm-001.jpg', 1),
('Coffee Mug', 'CM-RED-01', '陶瓷咖啡杯，红色，容量 350ml，适合微波炉和洗碗机。', 7.50, 200, 'https://example.com/images/cm-red.jpg', 1),
('Noise Cancelling Headphones', 'NCH-1000', '主动降噪耳机，支持蓝牙、40 小时续航和快充。', 199.99, 25, 'https://example.com/images/nch-1000.jpg', 1),
('USB-C Charger 65W', 'UC-65W', 'GaN 快充充电器，65W，支持笔记本与手机快速充电。', 39.99, 80, 'https://example.com/images/uc-65w.jpg', 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  price = VALUES(price),
  stock = VALUES(stock),
  image_url = VALUES(image_url),
  status = VALUES(status);
