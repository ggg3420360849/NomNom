SET NAMES utf8mb4;
USE `nomnom`;

SET @pwd_123456 = '$2b$10$9FOQgf7wwoVm7tRWNdtoyOA/YsHUk9bcKS/fxAvsLAk5D9ZoQZdRS';

SET @order_1001 = CONCAT('NN', DATE_FORMAT(NOW(), '%Y%m%d'), '114520101');
SET @order_1002 = CONCAT('NN', DATE_FORMAT(NOW(), '%Y%m%d'), '121836205');
SET @order_1003 = CONCAT('NN', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 DAY), '%Y%m%d'), '193006333');
SET @order_1004 = CONCAT('NN', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 2 DAY), '%Y%m%d'), '184012407');
SET @order_1005 = CONCAT('NN', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 3 DAY), '%Y%m%d'), '131522516');
SET @order_1010 = CONCAT('NN', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 DAY), '%Y%m%d'), '202405610');
SET @order_1011 = CONCAT('NN', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 2 DAY), '%Y%m%d'), '135500711');

-- 用户与钱包。
INSERT INTO `user` (id, account, username, password, avatar, email, role, gender, birthday, phone, create_time)
VALUES
    (3, 'merchant1', '店长小周', @pwd_123456, NULL, 'merchant@example.com', 2, 1, NULL, '13900139001', DATE_SUB(NOW(), INTERVAL 24 DAY)),
    (10, 'demo_customer', '顾客', @pwd_123456, NULL, 'demo.customer@nomnom.local', 1, 0, '2003-06-01', '13800138110', DATE_SUB(NOW(), INTERVAL 5 DAY))
AS new
ON DUPLICATE KEY UPDATE
    account = new.account,
    username = new.username,
    password = new.password,
    avatar = new.avatar,
    email = new.email,
    role = new.role,
    gender = new.gender,
    birthday = new.birthday,
    phone = new.phone,
    create_time = new.create_time;

INSERT INTO wallet (id, user_id, create_time, status, type, surplus)
VALUES
    (3, 3, DATE_SUB(NOW(), INTERVAL 5 DAY), 1, 2, 0.00),
    (10, 10, DATE_SUB(NOW(), INTERVAL 4 DAY), 1, 1, 497.00)
AS new
ON DUPLICATE KEY UPDATE
    user_id = new.user_id,
    create_time = new.create_time,
    status = new.status,
    type = new.type,
    surplus = new.surplus;

-- 菜品分类、菜品和套餐。
INSERT INTO dishes_type (id, icon_uri, name)
VALUES
    (101, 'rice', '主食'),
    (102, 'meat', '招牌荤菜'),
    (103, 'vegetable', '家常素菜'),
    (104, 'snack', '风味小吃'),
    (105, 'drink', '汤品饮品')
AS new
ON DUPLICATE KEY UPDATE
    icon_uri = new.icon_uri,
    name = new.name;

INSERT INTO dishes (id, type_id, name, detail, cover_url, status, create_time, stock, specs, is_recommend, recommend_ids)
VALUES
    (201, 101, '黑椒鸡排饭', '外焦里嫩的鸡排配黑椒汁，适合快速完成点餐演示。', NULL, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), 80, '{"sizes":["标准份","加蛋套餐"],"spiciness":["黑椒","微辣"]}', 1, '[209,210]'),
    (202, 101, '番茄牛腩饭', '牛腩炖煮软烂，番茄汤汁酸甜开胃。', NULL, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), 65, '{"sizes":["标准份"],"spiciness":["原味"]}', 1, '[207,212]'),
    (203, 101, '三鲜菌菇面', '菌菇、虾仁与青菜搭配，汤底清爽。', NULL, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), 70, '{"sizes":["小份","大份"],"spiciness":["原味"]}', 0, '[211]'),
    (204, 102, '蒜香排骨', '排骨外酥里嫩，蒜香浓郁，是商家端推荐搭配示例。', NULL, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), 42, '{"sizes":["标准份"],"spiciness":["蒜香"]}', 1, '[210,211]'),
    (205, 102, '香辣小炒肉', '五花肉与青椒猛火快炒，口味鲜香下饭。', NULL, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), 55, '{"sizes":["标准份"],"spiciness":["中辣","特辣"]}', 0, '[207,210]'),
    (206, 102, '宫保鸡丁', '经典酸甜微辣口味，适合展示搜索与分类筛选。', NULL, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), 60, '{"sizes":["标准份"],"spiciness":["微辣"]}', 0, '[212]'),
    (207, 103, '清炒时蔬', '当季绿叶菜现炒，补足套餐蔬菜搭配。', NULL, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), 90, '{"sizes":["标准份"],"spiciness":["清淡"]}', 0, '[201,202]'),
    (208, 103, '干锅花菜', '花菜爽脆入味，带轻微锅气。', NULL, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), 72, '{"sizes":["标准份"],"spiciness":["微辣"]}', 0, '[205]'),
    (209, 104, '手工小酥肉', '现炸小酥肉，适合作为加购小吃演示。', NULL, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), 100, '{"sizes":["标准份"],"spiciness":["椒盐"]}', 1, '[201,210]'),
    (210, 105, '招牌酸梅汤', '冰镇酸梅汤，解腻清爽。', NULL, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), 120, '{"sizes":["单杯"],"temperature":["冰镇","常温"]}', 1, '[204,209]'),
    (211, 105, '冰镇柠檬茶', '柠檬香气清新，适合展示饮品分类。', NULL, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), 110, '{"sizes":["单杯"],"temperature":["冰镇"]}', 0, '[203]'),
    (212, 105, '紫菜蛋花汤', '热汤出餐快，可与主食搭配。', NULL, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), 80, '{"sizes":["例汤"],"temperature":["热"]}', 0, '[202,206]'),
    (213, 101, '今日售罄叉烧饭', '用于商家端演示下架/售罄状态，顾客端不会展示。', NULL, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, '{"sizes":["标准份"],"spiciness":["原味"]}', 0, '[]')
AS new
ON DUPLICATE KEY UPDATE
    type_id = new.type_id,
    name = new.name,
    detail = new.detail,
    cover_url = COALESCE(NULLIF(dishes.cover_url, ''), new.cover_url),
    status = new.status,
    create_time = new.create_time,
    stock = new.stock,
    specs = new.specs,
    is_recommend = new.is_recommend,
    recommend_ids = new.recommend_ids;

INSERT INTO dishes_package (id, dishes_id, name, specs, price, create_time)
VALUES
    (1001, 201, '标准份', '黑椒', 36.00, DATE_SUB(NOW(), INTERVAL 8 DAY)),
    (1002, 201, '加蛋套餐', '黑椒+煎蛋', 42.00, DATE_SUB(NOW(), INTERVAL 8 DAY)),
    (1003, 202, '标准份', '原味', 42.00, DATE_SUB(NOW(), INTERVAL 8 DAY)),
    (1004, 203, '小份', '清汤', 28.00, DATE_SUB(NOW(), INTERVAL 7 DAY)),
    (1005, 203, '大份', '清汤', 35.00, DATE_SUB(NOW(), INTERVAL 7 DAY)),
    (1006, 204, '标准份', '蒜香', 58.00, DATE_SUB(NOW(), INTERVAL 7 DAY)),
    (1007, 205, '标准份', '中辣', 46.00, DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (1008, 206, '标准份', '微辣', 39.00, DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (1009, 207, '标准份', '清淡', 22.00, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (1010, 208, '标准份', '微辣', 26.00, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (1011, 209, '标准份', '椒盐', 24.00, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (1012, 210, '单杯', '冰镇', 9.00, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (1013, 211, '单杯', '冰镇', 12.00, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (1014, 212, '例汤', '热', 16.00, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (1015, 213, '标准份', '售罄', 48.00, DATE_SUB(NOW(), INTERVAL 2 DAY))
AS new
ON DUPLICATE KEY UPDATE
    dishes_id = new.dishes_id,
    name = new.name,
    specs = new.specs,
    price = new.price,
    create_time = new.create_time;

-- 桌台。
INSERT INTO dishes_table (id, number, person_number, status, create_time, area, amount, min_spend, occupied_by)
VALUES
    (101, 'A01', 2, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, 54.00, 0.00, 10),
    (102, 'A02', 2, 0, DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, 0.00, 0.00, NULL),
    (103, 'A03', 4, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, 128.00, 0.00, 10),
    (104, 'A04', 4, 0, DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, 0.00, 0.00, NULL),
    (105, 'B01', 6, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, 0.00, 0.00, NULL),
    (106, 'B02', 6, 6, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, 0.00, 0.00, NULL),
    (107, 'C01', 8, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, 0.00, 0.00, NULL),
    (108, 'C02', 8, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, 0.00, 0.00, NULL)
AS new
ON DUPLICATE KEY UPDATE
    number = new.number,
    person_number = new.person_number,
    status = new.status,
    create_time = new.create_time,
    area = new.area,
    amount = new.amount,
    min_spend = new.min_spend,
    occupied_by = new.occupied_by;

-- 订单、订单项与退款。
INSERT INTO orders (id, code, user_id, table_id, table_number, status, total_price, serve_food_time, pay_time, create_time)
VALUES
    (1001, @order_1001, 10, 101, 'A01', 2, 54.00, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (1002, @order_1002, 10, 103, 'A03', 3, 128.00, DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (1003, @order_1003, 10, 104, 'A04', 4, 99.00, DATE_SUB(NOW(), INTERVAL 23 HOUR), DATE_SUB(NOW(), INTERVAL 25 HOUR), DATE_SUB(NOW(), INTERVAL 25 HOUR)),
    (1004, @order_1004, 10, 105, 'B01', 4, 76.00, DATE_SUB(NOW(), INTERVAL 46 HOUR), DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 48 HOUR)),
    (1005, @order_1005, 10, 102, 'A02', 5, 42.00, NULL, DATE_SUB(NOW(), INTERVAL 72 HOUR), DATE_SUB(NOW(), INTERVAL 72 HOUR)),
    (1010, @order_1010, 10, 102, 'A02', 4, 66.00, DATE_SUB(NOW(), INTERVAL 22 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR)),
    (1011, @order_1011, 10, 107, 'C01', 4, 91.00, DATE_SUB(NOW(), INTERVAL 44 HOUR), DATE_SUB(NOW(), INTERVAL 46 HOUR), DATE_SUB(NOW(), INTERVAL 46 HOUR))
AS new
ON DUPLICATE KEY UPDATE
    code = new.code,
    user_id = new.user_id,
    table_id = new.table_id,
    table_number = new.table_number,
    status = new.status,
    total_price = new.total_price,
    serve_food_time = new.serve_food_time,
    pay_time = new.pay_time,
    create_time = new.create_time;

INSERT INTO orders_item (id, orders_id, dishes_package_id, snap_price, snap_name, snap_cover, buy_number, total_price, create_time)
VALUES
    (2001, 1001, 1001, 36.00, '黑椒鸡排饭 - 标准份 / 黑椒', NULL, 1, 36.00, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (2002, 1001, 1012, 9.00, '招牌酸梅汤 - 单杯 / 冰镇', NULL, 2, 18.00, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (2003, 1002, 1006, 58.00, '蒜香排骨 - 标准份 / 蒜香', NULL, 1, 58.00, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (2004, 1002, 1007, 46.00, '香辣小炒肉 - 标准份 / 中辣', NULL, 1, 46.00, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (2005, 1002, 1011, 24.00, '手工小酥肉 - 标准份 / 椒盐', NULL, 1, 24.00, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (2006, 1003, 1003, 42.00, '番茄牛腩饭 - 标准份 / 原味', NULL, 1, 42.00, DATE_SUB(NOW(), INTERVAL 25 HOUR)),
    (2007, 1003, 1009, 22.00, '清炒时蔬 - 标准份 / 清淡', NULL, 1, 22.00, DATE_SUB(NOW(), INTERVAL 25 HOUR)),
    (2008, 1003, 1010, 26.00, '干锅花菜 - 标准份 / 微辣', NULL, 1, 26.00, DATE_SUB(NOW(), INTERVAL 25 HOUR)),
    (2009, 1003, 1012, 9.00, '招牌酸梅汤 - 单杯 / 冰镇', NULL, 1, 9.00, DATE_SUB(NOW(), INTERVAL 25 HOUR)),
    (2010, 1004, 1004, 28.00, '三鲜菌菇面 - 小份 / 清汤', NULL, 1, 28.00, DATE_SUB(NOW(), INTERVAL 48 HOUR)),
    (2011, 1004, 1008, 39.00, '宫保鸡丁 - 标准份 / 微辣', NULL, 1, 39.00, DATE_SUB(NOW(), INTERVAL 48 HOUR)),
    (2012, 1004, 1012, 9.00, '招牌酸梅汤 - 单杯 / 冰镇', NULL, 1, 9.00, DATE_SUB(NOW(), INTERVAL 48 HOUR)),
    (2013, 1005, 1002, 42.00, '黑椒鸡排饭 - 加蛋套餐 / 黑椒+煎蛋', NULL, 1, 42.00, DATE_SUB(NOW(), INTERVAL 72 HOUR)),
    (2020, 1010, 1004, 28.00, '三鲜菌菇面 - 小份 / 清汤', NULL, 1, 28.00, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
    (2021, 1010, 1009, 22.00, '清炒时蔬 - 标准份 / 清淡', NULL, 1, 22.00, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
    (2022, 1010, 1014, 16.00, '紫菜蛋花汤 - 例汤 / 热', NULL, 1, 16.00, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
    (2023, 1011, 1006, 58.00, '蒜香排骨 - 标准份 / 蒜香', NULL, 1, 58.00, DATE_SUB(NOW(), INTERVAL 46 HOUR)),
    (2024, 1011, 1011, 24.00, '手工小酥肉 - 标准份 / 椒盐', NULL, 1, 24.00, DATE_SUB(NOW(), INTERVAL 46 HOUR)),
    (2025, 1011, 1012, 9.00, '招牌酸梅汤 - 单杯 / 冰镇', NULL, 1, 9.00, DATE_SUB(NOW(), INTERVAL 46 HOUR))
AS new
ON DUPLICATE KEY UPDATE
    orders_id = new.orders_id,
    dishes_package_id = new.dishes_package_id,
    snap_price = new.snap_price,
    snap_name = new.snap_name,
    snap_cover = COALESCE(NULLIF(orders_item.snap_cover, ''), new.snap_cover),
    buy_number = new.buy_number,
    total_price = new.total_price,
    create_time = new.create_time;

INSERT INTO orders_refund_reply (id, orders_id, status, refund_cause, reject_refund_cause, create_time, refun_time)
VALUES
    (1001, 1004, 2, '退款：菜品味道偏咸，希望退款', NULL, DATE_SUB(NOW(), INTERVAL 20 HOUR), NULL),
    (1002, 1005, 3, '取消订单：临时更换用餐计划', NULL, DATE_SUB(NOW(), INTERVAL 70 HOUR), DATE_SUB(NOW(), INTERVAL 69 HOUR))
AS new
ON DUPLICATE KEY UPDATE
    orders_id = new.orders_id,
    status = new.status,
    refund_cause = new.refund_cause,
    reject_refund_cause = new.reject_refund_cause,
    create_time = new.create_time,
    refun_time = new.refun_time;

-- 评价、收藏和购物车。
INSERT INTO dishes_evaluations (id, dishes_id, user_id, orders_id, content, rating_value, reply_content, reply_time, reply_status, create_time)
VALUES
    (1001, 202, 10, 1003, '番茄牛腩很入味，米饭也热，整体体验不错。', 5, '感谢认可，欢迎下次继续点番茄牛腩饭。', DATE_SUB(NOW(), INTERVAL 18 HOUR), 1, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (1002, 204, 10, 1011, '蒜香排骨很好吃，但希望下次蒜蓉可以再多一点。', 4, NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 30 HOUR)),
    (1003, 207, 10, 1010, '清炒时蔬很清爽，搭配主食刚好。', 5, '谢谢反馈，我们会继续保持出品稳定。', DATE_SUB(NOW(), INTERVAL 16 HOUR), 1, DATE_SUB(NOW(), INTERVAL 18 HOUR))
AS new
ON DUPLICATE KEY UPDATE
    dishes_id = new.dishes_id,
    user_id = new.user_id,
    orders_id = new.orders_id,
    content = new.content,
    rating_value = new.rating_value,
    reply_content = new.reply_content,
    reply_time = new.reply_time,
    reply_status = new.reply_status,
    create_time = new.create_time;

INSERT INTO collection (id, user_id, dishes_id)
VALUES
    (1001, 10, 201),
    (1002, 10, 209),
    (1003, 10, 210),
    (1004, 10, 202),
    (1005, 10, 204)
AS new
ON DUPLICATE KEY UPDATE
    user_id = new.user_id,
    dishes_id = new.dishes_id;

INSERT INTO shopping_car (id, user_id, dishes_package_id, dishes_id, buy_number, is_seleced, create_time)
VALUES
    (1001, 10, 1001, 201, 1, 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
    (1002, 10, 1013, 211, 2, 1, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
    (1003, 10, 1011, 209, 1, 1, DATE_SUB(NOW(), INTERVAL 20 MINUTE))
AS new
ON DUPLICATE KEY UPDATE
    user_id = new.user_id,
    dishes_package_id = new.dishes_package_id,
    dishes_id = new.dishes_id,
    buy_number = new.buy_number,
    is_seleced = new.is_seleced,
    create_time = new.create_time;

-- 钱包流水和系统消息。
INSERT INTO wallet_info (id, wallet_id, detail, surplus_money, create_time)
VALUES
    (1001, 10, '钱包充值', 500.00, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (1002, 10, CONCAT('订单支付，', @order_1001), -54.00, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (1003, 10, CONCAT('订单支付，', @order_1002), -128.00, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (1004, 10, CONCAT('订单支付，', @order_1003), -99.00, DATE_SUB(NOW(), INTERVAL 25 HOUR)),
    (1005, 10, CONCAT('订单支付，', @order_1004), -76.00, DATE_SUB(NOW(), INTERVAL 48 HOUR)),
    (1006, 10, CONCAT('订单支付，', @order_1005), -42.00, DATE_SUB(NOW(), INTERVAL 72 HOUR)),
    (1007, 10, CONCAT('订单退款，', @order_1005), 42.00, DATE_SUB(NOW(), INTERVAL 69 HOUR)),
    (1010, 10, '钱包充值', 300.00, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (1011, 10, CONCAT('订单支付，', @order_1010), -66.00, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
    (1012, 10, '钱包充值', 211.00, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (1013, 10, CONCAT('订单支付，', @order_1011), -91.00, DATE_SUB(NOW(), INTERVAL 46 HOUR))
AS new
ON DUPLICATE KEY UPDATE
    wallet_id = new.wallet_id,
    detail = new.detail,
    surplus_money = new.surplus_money,
    create_time = new.create_time;

INSERT INTO messages (id, user_id, content, type, read_status, create_time)
VALUES
    (1002, 10, CONCAT('订单 ', @order_1010, ' 已完成，可在我的订单中继续评价未评价菜品。'), 2, 0, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
    (1003, 10, CONCAT('订单 ', @order_1002, ' 正在制作中，请稍候。'), 2, 0, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
    (1004, 10, '你的退款申请已提交，等待商家审核。', 2, 0, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (1005, 3, CONCAT('收到新订单 ', @order_1002, '，桌台 A03，金额 ￥128.00。'), 2, 0, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (1006, 3, CONCAT('订单 ', @order_1004, ' 有新的退款申请，请在订单与审批中处理。'), 2, 0, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (1007, 3, '顾客评价了「蒜香排骨」，等待商家回复。', 4, 0, DATE_SUB(NOW(), INTERVAL 30 HOUR))
AS new
ON DUPLICATE KEY UPDATE
    user_id = new.user_id,
    content = new.content,
    type = new.type,
    read_status = new.read_status,
    create_time = new.create_time;
