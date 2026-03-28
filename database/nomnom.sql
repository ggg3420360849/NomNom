-- 1. 如果表已经存在就先删除，方便重新开始
DROP TABLE IF EXISTS menu;

-- 2. 创建菜单表
CREATE TABLE menu (
    id INT AUTO_INCREMENT PRIMARY KEY,    -- 菜品ID，自动增长
    name VARCHAR(100) NOT NULL,           -- 菜名，不能为空
    price DECIMAL(10, 2) NOT NULL,        -- 价格，保留两位小数
    category VARCHAR(50),                 -- 分类（如：热菜、凉菜、饮料）
    description TEXT,                     -- 菜品描述
    status TINYINT DEFAULT 1              -- 状态：1表示在售，0表示售罄
);

-- 3. 插入一些测试数据
INSERT INTO menu (name, price, category, description) VALUES
('宫保鸡丁', 38.00, '热菜', '经典川菜，酸甜适口'),
('清蒸鲈鱼', 68.00, '热菜', '鲜嫩多汁，老少皆宜'),
('拍黄瓜', 15.00, '凉菜', '清脆爽口'),
('扬州炒饭', 22.00, '主食', '粒粒金黄'),
('冰可乐', 5.00, '饮料', '330ml 罐装');

