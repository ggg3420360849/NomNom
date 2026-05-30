create table collection
(
    id        int auto_increment comment '收藏夹主键ID，自增'
        primary key,
    user_id   int null comment '用户ID，外键，关联用户表',
    dishes_id int null comment '菜品ID，外键，关联菜品表'
)
    comment '菜品收藏表' charset = utf8mb4;

create index idx_user_dishes_id
    on collection (user_id, dishes_id)
    comment '索引-用户ID、菜品ID';

create table dishes
(
    id          int auto_increment comment '主键ID,自增'
        primary key,
    type_id     int          null comment '菜品种类ID，外键，关联菜品种类',
    name        varchar(50)  null comment '菜品名称',
    detail      text         null comment '菜品介绍',
    cover_url   varchar(255) null comment '菜品封面路径',
    status      tinyint(1)   null comment '菜品状态（0：下架状态；1：上架状态）',
    create_time datetime     null comment '菜品创建时间'
)
    comment '菜品信息表' charset = utf8mb4;

create index idx_type_id
    on dishes (type_id)
    comment '索引-菜品种类ID';

create table dishes_evaluations
(
    id            int auto_increment comment '菜品评价表主键，自增'
        primary key,
    dishes_id     int        null comment '菜品主键ID，外键，关联菜品表',
    user_id       int        null comment '用户ID，外键，关联用户表',
    content       text       null comment '评论内容',
    rating_value  int        null comment '评分值，1-5分',
    reply_content text       null comment '回复内容',
    reply_time    datetime   null comment '回复时间',
    reply_status  tinyint(1) null comment '回复状态（0：未回复；1：已回复）',
    create_time   datetime   null comment '创建时间'
)
    comment '菜品评价表' charset = utf8mb4;

create index idx_dishes_id
    on dishes_evaluations (dishes_id)
    comment '索引-菜品ID';

create index idx_dishes_user_id
    on dishes_evaluations (user_id, dishes_id)
    comment '索引-菜品ID，用户ID';

create index idx_user_id
    on dishes_evaluations (user_id)
    comment '索引-用户ID';

create table dishes_package
(
    id          int auto_increment comment '菜品套餐主键ID，自增'
        primary key,
    dishes_id   int            null comment '菜品ID，外键，关联菜品信息表',
    name        varchar(30)    not null comment '菜品套餐名字',
    specs       varchar(30)    null comment '规格',
    price       decimal(10, 2) null comment '价格',
    create_time datetime       null comment '创建时间'
)
    comment '菜品套餐信息表' charset = utf8mb4;

create index idx_dishes_id
    on dishes_package (dishes_id)
    comment '索引-菜品ID';

create table dishes_table
(
    id            int auto_increment comment '餐桌主键ID，自增'
        primary key,
    number        varchar(20) null comment '餐桌序号',
    person_number int         null comment '就餐人数',
    status        tinyint(1)  null comment '状态',
    create_time   datetime    null comment '创建时间'
)
    comment '餐桌信息表' charset = utf8mb4;

create table dishes_type
(
    id       int auto_increment comment '主键ID，自增'
        primary key,
    icon_uri varchar(255) null comment '图标URL路径',
    name     varchar(30)  not null comment '菜品种类'
)
    comment '菜品种类信息表' charset = utf8mb4;

create table images
(
    id                    int auto_increment comment '菜品评论图片表主键，自增'
        primary key,
    dishes_evaluations_id int          null comment '菜品评论ID，外键，关联菜品评论表',
    picture_url           varchar(255) null comment '图片的URL路径',
    number                int          null comment '排序序号'
)
    comment '菜品评论图片表' charset = utf8mb4;

create index idx_dishes_evaluations_id
    on images (dishes_evaluations_id)
    comment '索引-菜品评价ID';

create table messages
(
    id          int auto_increment comment '消息通知表主键，自增'
        primary key,
    user_id     int          null comment '用户ID，外键，关联用户表',
    content     varchar(255) null comment '消息内容',
    type        tinyint(1)   null comment '消息类型（1：系统通知；2：订单通知）',
    read_status tinyint(1)   null comment '阅读状态（0：未读；1：已读）',
    create_time datetime     null comment '通知时间'
)
    comment '消息通知表' charset = utf8mb4;

create index idx_user_id
    on messages (user_id)
    comment '索引-用户ID';

create table orders
(
    id              int auto_increment comment '订单表主键ID，自增'
        primary key,
    code            varchar(100)   null comment '订单号',
    user_id         int            null comment '用户ID，外键，关联用户表',
    status          tinyint(1)     null comment '订单状态（1：待支付；2：已支付；3：出餐中；4：已完成',
    total_price     decimal(10, 2) null comment '订单总价格',
    serve_food_time datetime       null comment '出餐时间',
    pay_time        datetime       null comment '支付时间',
    create_time     datetime       null comment '创建时间'
)
    comment '订单信息表' charset = utf8mb4;

create index idx_user_id
    on orders (user_id)
    comment '索引-用户ID';

create table orders_item
(
    id                int auto_increment comment '订单项主键ID，自增'
        primary key,
    orders_id         int            null comment '订单ID，外键，关联订单表',
    dishes_package_id int            null comment '菜品套餐ID，外键，关联的是菜品套餐表',
    snap_price        decimal(10, 2) null comment '快照价格',
    snap_name         varchar(50)    null comment '快照名称',
    snap_cover        varchar(255)   null comment '快照封面',
    buy_number        int            null comment '购买数量',
    total_price       decimal(10, 2) null comment '价格(快照价格*购买数量)',
    create_time       datetime       null comment '创建时间'
)
    comment '订单项' charset = utf8mb4;

create index idx_orders_id
    on orders_item (orders_id)
    comment '索引订单ID';

create table orders_refund_reply
(
    id                  int auto_increment comment '订单退款申请表主键，自增'
        primary key,
    orders_id           int          null comment '订单ID，外键，关联订单表',
    status              tinyint(1)   null comment '退款状态(1：无退款；2：退款中；3：已退款；4：拒绝退款)',
    refund_cause        varchar(255) null comment '退款缘由（顾客退款时进行补充）',
    reject_refund_cause varchar(255) null comment '拒绝退款时缘由（商家拒绝退款时补充）',
    create_time         datetime     null comment '创建时间',
    refun_time          datetime     null comment '退款时间'
)
    comment '订单退款申请表' charset = utf8mb4;

create index idx_orders_id
    on orders_refund_reply (orders_id)
    comment '索引-订单ID';

create table shopping_car
(
    id                int auto_increment comment '购物车主键ID，自增'
        primary key,
    user_id           int        null comment '用户ID，外键，关联用户表',
    dishes_package_id int        null comment '餐品套餐ID，外键，关联的是菜品套餐表',
    buy_number        int        null comment '加购的数量',
    is_seleced        tinyint(1) not null comment '是否选中',
    create_time       datetime   null comment '创建时间'
)
    comment '购物车信息表' charset = utf8mb4;

create index idx_user_id
    on shopping_car (user_id)
    comment '索引-用户ID';

create table user
(
    id          int unsigned auto_increment comment '用户编号'
        primary key,
    account     varchar(50)  null comment '用户账号',
    username    varchar(50)  null comment '用户昵称',
    password    varchar(100) null comment '用户密码',
    avatar      text         null comment '用户头像',
    email       varchar(50)  null comment '用户邮箱',
    role        int          null comment '用户角色',
    gender      tinyint(1)   null comment '性别：1-女，2-男',
    birthday    date         null comment '生日',
    phone       varchar(255) null comment '手机号',
    create_time datetime     null comment '用户注册时间'
)
    comment '用户表' charset = utf8mb4
                     row_format = DYNAMIC;

create table wallet
(
    id          int auto_increment comment '钱包主键ID，自增'
        primary key,
    user_id     int            null comment '用户ID，外键，关联用户表',
    create_time datetime       null comment '创建时间',
    status      tinyint(1)     null comment '钱包状态（0：冻结；1：正常可用）',
    type        tinyint(1)     null comment '钱包类型（1：私人钱包；2：商家钱包）',
    surplus     decimal(10, 2) null comment '余额'
)
    comment '钱包信息表' charset = utf8mb4;

create index idx_user_id
    on wallet (user_id)
    comment '索引-用户ID';

create table wallet_info
(
    create_time   datetime       null comment '创建时间',
    id            int auto_increment comment '钱包明细主键ID，自增'
        primary key,
    wallet_id     int            null comment '钱包ID，外键，关联的是钱包信息表',
    detail        varchar(255)   null comment '描述',
    surplus_money decimal(10, 2) null comment '钱包资金走向'
)
    comment '钱包明细表' charset = utf8mb4;

create index idex_wallet_id
    on wallet_info (wallet_id)
    comment '索引-钱包ID';



-- 种子用户数据 (密码已使用 BCrypt 加密)
-- 账号 123456 / 密码 123456 (顾客)
insert into user (id, account, username, password, avatar, email, role, gender, birthday, phone, create_time)
values (1, '123456', 'aaa', '$2b$10$9FOQgf7wwoVm7tRWNdtoyOA/YsHUk9bcKS/fxAvsLAk5D9ZoQZdRS', null, null, 1, null, null, null, '2026-05-08 21:00:11');

-- 账号 merchant1 / 密码 123456 (商家)
insert into user (id, account, username, password, avatar, email, role, gender, birthday, phone, create_time)
values (3, 'merchant1', '商家甲', '$2b$10$9FOQgf7wwoVm7tRWNdtoyOA/YsHUk9bcKS/fxAvsLAk5D9ZoQZdRS', null, null, 2, null, null, null, '2026-05-08 21:00:30');

-- 账号 1234567 / 密码 1234567 (顾客)
insert into user (id, account, username, password, avatar, email, role, gender, birthday, phone, create_time)
values (2, '1234567', 'bbb', '$2b$10$wxW1d5pgB.QmGpuA6N4RH.o7rZLmYHmTzMCzO5UB0qOPNAHX4bLcG', null, null, 1, null, null, null, '2026-05-08 21:00:24');

-- 3. 升级购物车表：增加 dishes_id 冗余菜品ID避免多层JOIN
ALTER TABLE shopping_car ADD COLUMN dishes_id INT COMMENT '菜品ID，冗余字段便于列表展示';

-- 4. 升级菜品评价表：增加 orders_id 关联订单
ALTER TABLE dishes_evaluations ADD COLUMN orders_id INT COMMENT '关联订单ID，防止刷评';

-- 1. 升级桌台表：保留区域兼容字段、增加当前消费金额、最低消费
ALTER TABLE dishes_table ADD COLUMN area VARCHAR(50) COMMENT '桌台区域兼容字段';
ALTER TABLE dishes_table ADD COLUMN amount DECIMAL(10, 2) DEFAULT 0.00 COMMENT '当前桌台消费总额';
ALTER TABLE dishes_table ADD COLUMN min_spend DECIMAL(10, 2) DEFAULT 0.00 COMMENT '最低消费';
ALTER TABLE dishes_table ADD COLUMN occupied_by INT COMMENT '当前占用用户ID';

-- 2. 升级菜品表：增加库存、多规格JSON配置
ALTER TABLE dishes ADD COLUMN stock INT DEFAULT 999 COMMENT '库存数量';
ALTER TABLE dishes ADD COLUMN specs TEXT COMMENT '菜品规格口味，存储为JSON字符串';

-- 5. 升级菜品表：增加推荐字段
ALTER TABLE dishes ADD COLUMN is_recommend TINYINT(1) DEFAULT 0 COMMENT '是否店长推荐（0：否；1：是）';
ALTER TABLE dishes ADD COLUMN recommend_ids TEXT COMMENT '推荐搭配菜品ID列表，JSON数组格式';
