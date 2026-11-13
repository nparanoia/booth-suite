-- 演示目录：空库开箱前台有货（仅本地演示）

INSERT INTO brand (id, name, first_letter, logo, sort, show_status)
VALUES (1, 'Booth 自营', 'B', NULL, 0, 1);

INSERT INTO category (id, parent_id, name, level, sort, nav_status, show_status)
VALUES (1, 0, '日用', 1, 0, 1, 1);

INSERT INTO product (id, category_id, brand_id, product_sn, name, sub_title, pic, price, original_price,
                     unit, sale, sort, publish_status, new_status, recommend_status, gift_integration,
                     brand_name, category_name, deleted)
VALUES (1, 1, 1, 'DEMO-MUG-001', '演示马克杯', '开箱演示用，可下单走完售后路径',
        NULL, 29.90, 39.90, '个', 0, 0, 1, 1, 1, 0, 'Booth 自营', '日用', 0);

INSERT INTO sku (id, product_id, sku_code, spec_data, price, stock, lock_stock, low_stock, sale)
VALUES (1, 1, 'DEMO-MUG-001-DFT', JSON_ARRAY(JSON_OBJECT('key', '规格', 'value', '标准')),
        29.90, 999, 0, 5, 0);

INSERT INTO home_banner (id, name, pic, url, sort, status)
VALUES (1, '开箱欢迎', 'https://via.placeholder.com/1200x360.png?text=Booth', '/', 0, 1);

INSERT INTO home_recommend (product_id, type, sort, status)
VALUES (1, 1, 0, 1),
       (1, 2, 0, 1);
