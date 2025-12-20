-- Seeds for E2E tests: basic products
INSERT INTO product (name, sku, description, price, stock, status) VALUES
('E2E 测试商品', 'E2E-SKU-001', '用于 E2E 自动化测试', 9.99, 100, 1)
ON DUPLICATE KEY UPDATE name=name;
