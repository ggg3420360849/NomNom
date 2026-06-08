SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `nomnom` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `nomnom`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `images`;
DROP TABLE IF EXISTS `messages`;
DROP TABLE IF EXISTS `wallet_info`;
DROP TABLE IF EXISTS `shopping_car`;
DROP TABLE IF EXISTS `collection`;
DROP TABLE IF EXISTS `dishes_evaluations`;
DROP TABLE IF EXISTS `orders_refund_reply`;
DROP TABLE IF EXISTS `orders_item`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `wallet`;
DROP TABLE IF EXISTS `dishes_package`;
DROP TABLE IF EXISTS `dishes`;
DROP TABLE IF EXISTS `dishes_table`;
DROP TABLE IF EXISTS `dishes_type`;
DROP TABLE IF EXISTS `user`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `user`
(
    `id`          INT UNSIGNED AUTO_INCREMENT COMMENT '用户编号'
        PRIMARY KEY,
    `account`     VARCHAR(50)  NULL COMMENT '用户账号',
    `username`    VARCHAR(50)  NULL COMMENT '用户昵称',
    `password`    VARCHAR(100) NULL COMMENT '用户密码',
    `avatar`      TEXT         NULL COMMENT '用户头像',
    `email`       VARCHAR(50)  NULL COMMENT '用户邮箱',
    `role`        INT          NULL COMMENT '用户角色（1：顾客；2：商家）',
    `gender`      TINYINT(1)   NULL COMMENT '性别（0：未知；1：女；2：男）',
    `birthday`    DATE         NULL COMMENT '生日',
    `phone`       VARCHAR(255) NULL COMMENT '手机号',
    `create_time` DATETIME     NULL COMMENT '用户注册时间',
    INDEX `idx_account` (`account`),
    INDEX `idx_role` (`role`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    ROW_FORMAT = DYNAMIC
    COMMENT = '用户表';

CREATE TABLE `dishes_type`
(
    `id`       INT AUTO_INCREMENT COMMENT '主键ID，自增'
        PRIMARY KEY,
    `icon_uri` VARCHAR(255) NULL COMMENT '图标URL路径',
    `name`     VARCHAR(30)  NOT NULL COMMENT '菜品种类',
    INDEX `idx_name` (`name`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '菜品种类信息表';

CREATE TABLE `dishes_table`
(
    `id`            INT AUTO_INCREMENT COMMENT '餐桌主键ID，自增'
        PRIMARY KEY,
    `number`        VARCHAR(20)    NULL COMMENT '餐桌序号',
    `person_number` INT            NULL COMMENT '就餐人数',
    `status`        TINYINT(1)     NULL COMMENT '状态（0：空闲；1：已入座；6：维修中）',
    `create_time`   DATETIME       NULL COMMENT '创建时间',
    `area`          VARCHAR(50)    NULL COMMENT '桌台区域兼容字段',
    `amount`        DECIMAL(10, 2) NULL DEFAULT 0.00 COMMENT '当前桌台消费总额',
    `min_spend`     DECIMAL(10, 2) NULL DEFAULT 0.00 COMMENT '最低消费',
    `occupied_by`   INT            NULL COMMENT '当前占用用户ID',
    INDEX `idx_number` (`number`),
    INDEX `idx_status` (`status`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '餐桌信息表';

CREATE TABLE `dishes`
(
    `id`            INT AUTO_INCREMENT COMMENT '主键ID，自增'
        PRIMARY KEY,
    `type_id`       INT          NULL COMMENT '菜品种类ID，关联菜品种类',
    `name`          VARCHAR(50)  NULL COMMENT '菜品名称',
    `detail`        TEXT         NULL COMMENT '菜品介绍',
    `cover_url`     VARCHAR(255) NULL COMMENT '菜品封面路径',
    `status`        TINYINT(1)   NULL COMMENT '菜品状态（0：下架状态；1：上架状态）',
    `create_time`   DATETIME     NULL COMMENT '菜品创建时间',
    `stock`         INT          NULL DEFAULT 999 COMMENT '库存数量',
    `specs`         TEXT         NULL COMMENT '菜品规格口味，存储为JSON字符串',
    `is_recommend`  TINYINT(1)   NULL DEFAULT 0 COMMENT '是否店长推荐（0：否；1：是）',
    `recommend_ids` TEXT         NULL COMMENT '推荐搭配菜品ID列表，JSON数组格式',
    INDEX `idx_type_id` (`type_id`),
    INDEX `idx_status_recommend` (`status`, `is_recommend`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '菜品信息表';

CREATE TABLE `dishes_package`
(
    `id`          INT AUTO_INCREMENT COMMENT '菜品套餐主键ID，自增'
        PRIMARY KEY,
    `dishes_id`   INT            NULL COMMENT '菜品ID，关联菜品信息表',
    `name`        VARCHAR(30)    NOT NULL COMMENT '菜品套餐名字',
    `specs`       VARCHAR(30)    NULL COMMENT '规格',
    `price`       DECIMAL(10, 2) NULL COMMENT '价格',
    `create_time` DATETIME       NULL COMMENT '创建时间',
    INDEX `idx_dishes_id` (`dishes_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '菜品套餐信息表';

CREATE TABLE `wallet`
(
    `id`          INT AUTO_INCREMENT COMMENT '钱包主键ID，自增'
        PRIMARY KEY,
    `user_id`     INT            NULL COMMENT '用户ID，关联用户表',
    `create_time` DATETIME       NULL COMMENT '创建时间',
    `status`      TINYINT(1)     NULL COMMENT '钱包状态（0：冻结；1：正常可用）',
    `type`        TINYINT(1)     NULL COMMENT '钱包类型（1：私人钱包；2：商家钱包）',
    `surplus`     DECIMAL(10, 2) NULL COMMENT '余额',
    INDEX `idx_user_id` (`user_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '钱包信息表';

CREATE TABLE `orders`
(
    `id`              INT AUTO_INCREMENT COMMENT '订单表主键ID，自增'
        PRIMARY KEY,
    `code`            VARCHAR(100)   NULL COMMENT '订单号',
    `user_id`         INT            NULL COMMENT '用户ID，关联用户表',
    `table_id`        INT            NULL COMMENT '桌台ID',
    `table_number`    VARCHAR(20)    NULL COMMENT '桌台号快照',
    `status`          TINYINT(1)     NULL COMMENT '订单状态（1：待支付；2：已支付；3：出餐中；4：已完成；5：已退款）',
    `total_price`     DECIMAL(10, 2) NULL COMMENT '订单总价格',
    `serve_food_time` DATETIME       NULL COMMENT '出餐时间',
    `pay_time`        DATETIME       NULL COMMENT '支付时间',
    `create_time`     DATETIME       NULL COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_table_id` (`table_id`),
    INDEX `idx_status` (`status`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '订单信息表';

CREATE TABLE `orders_item`
(
    `id`                INT AUTO_INCREMENT COMMENT '订单项主键ID，自增'
        PRIMARY KEY,
    `orders_id`         INT            NULL COMMENT '订单ID，关联订单表',
    `dishes_package_id` INT            NULL COMMENT '菜品套餐ID，关联菜品套餐表',
    `snap_price`        DECIMAL(10, 2) NULL COMMENT '快照价格',
    `snap_name`         VARCHAR(50)    NULL COMMENT '快照名称',
    `snap_cover`        VARCHAR(255)   NULL COMMENT '快照封面',
    `buy_number`        INT            NULL COMMENT '购买数量',
    `total_price`       DECIMAL(10, 2) NULL COMMENT '价格（快照价格*购买数量）',
    `create_time`       DATETIME       NULL COMMENT '创建时间',
    INDEX `idx_orders_id` (`orders_id`),
    INDEX `idx_package_id` (`dishes_package_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '订单项';

CREATE TABLE `orders_refund_reply`
(
    `id`                  INT AUTO_INCREMENT COMMENT '订单退款申请表主键，自增'
        PRIMARY KEY,
    `orders_id`           INT          NULL COMMENT '订单ID，关联订单表',
    `status`              TINYINT(1)   NULL COMMENT '退款状态（1：无退款；2：退款中；3：已退款；4：拒绝退款）',
    `refund_cause`        VARCHAR(255) NULL COMMENT '退款缘由（顾客退款时进行补充）',
    `reject_refund_cause` VARCHAR(255) NULL COMMENT '拒绝退款时缘由（商家拒绝退款时补充）',
    `create_time`         DATETIME     NULL COMMENT '创建时间',
    `refun_time`          DATETIME     NULL COMMENT '退款时间',
    INDEX `idx_orders_id` (`orders_id`),
    INDEX `idx_status` (`status`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '订单退款申请表';

CREATE TABLE `dishes_evaluations`
(
    `id`            INT AUTO_INCREMENT COMMENT '菜品评价表主键，自增'
        PRIMARY KEY,
    `dishes_id`     INT        NULL COMMENT '菜品主键ID，关联菜品表',
    `user_id`       INT        NULL COMMENT '用户ID，关联用户表',
    `orders_id`     INT        NULL COMMENT '关联订单ID，防止刷评',
    `content`       TEXT       NULL COMMENT '评论内容',
    `rating_value`  INT        NULL COMMENT '评分值，1-5分',
    `reply_content` TEXT       NULL COMMENT '回复内容',
    `reply_time`    DATETIME   NULL COMMENT '回复时间',
    `reply_status`  TINYINT(1) NULL COMMENT '回复状态（0：未回复；1：已回复）',
    `create_time`   DATETIME   NULL COMMENT '创建时间',
    INDEX `idx_dishes_id` (`dishes_id`),
    INDEX `idx_dishes_user_id` (`user_id`, `dishes_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_orders_id` (`orders_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '菜品评价表';

CREATE TABLE `collection`
(
    `id`        INT AUTO_INCREMENT COMMENT '收藏夹主键ID，自增'
        PRIMARY KEY,
    `user_id`   INT NULL COMMENT '用户ID，关联用户表',
    `dishes_id` INT NULL COMMENT '菜品ID，关联菜品表',
    INDEX `idx_user_dishes_id` (`user_id`, `dishes_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '菜品收藏表';

CREATE TABLE `shopping_car`
(
    `id`                INT AUTO_INCREMENT COMMENT '购物车主键ID，自增'
        PRIMARY KEY,
    `user_id`           INT        NULL COMMENT '用户ID，关联用户表',
    `dishes_id`         INT        NULL COMMENT '菜品ID，冗余字段便于列表展示',
    `dishes_package_id` INT        NULL COMMENT '餐品套餐ID，关联菜品套餐表',
    `buy_number`        INT        NULL COMMENT '加购的数量',
    `is_seleced`        TINYINT(1) NOT NULL COMMENT '是否选中',
    `create_time`       DATETIME   NULL COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_dishes_id` (`dishes_id`),
    INDEX `idx_package_id` (`dishes_package_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '购物车信息表';

CREATE TABLE `wallet_info`
(
    `id`            INT AUTO_INCREMENT COMMENT '钱包明细主键ID，自增'
        PRIMARY KEY,
    `wallet_id`     INT            NULL COMMENT '钱包ID，关联钱包信息表',
    `detail`        VARCHAR(255)   NULL COMMENT '描述',
    `surplus_money` DECIMAL(10, 2) NULL COMMENT '钱包资金走向',
    `create_time`   DATETIME       NULL COMMENT '创建时间',
    INDEX `idx_wallet_id` (`wallet_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '钱包明细表';

CREATE TABLE `messages`
(
    `id`          INT AUTO_INCREMENT COMMENT '消息通知表主键，自增'
        PRIMARY KEY,
    `user_id`     INT          NULL COMMENT '用户ID，关联用户表',
    `content`     VARCHAR(255) NULL COMMENT '消息内容',
    `type`        TINYINT(1)   NULL COMMENT '消息类型（1：系统通知；2：订单通知；3：钱包通知；4：评价通知）',
    `read_status` TINYINT(1)   NULL COMMENT '阅读状态（0：未读；1：已读）',
    `create_time` DATETIME     NULL COMMENT '通知时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_read_status` (`read_status`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '消息通知表';

CREATE TABLE `images`
(
    `id`                    INT AUTO_INCREMENT COMMENT '菜品评论图片表主键，自增'
        PRIMARY KEY,
    `dishes_evaluations_id` INT          NULL COMMENT '菜品评论ID，关联菜品评论表',
    `picture_url`           VARCHAR(255) NULL COMMENT '图片的URL路径',
    `number`                INT          NULL COMMENT '排序序号',
    INDEX `idx_dishes_evaluations_id` (`dishes_evaluations_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '菜品评论图片表';

CREATE TABLE `comment`
(
    `id`          BIGINT AUTO_INCREMENT COMMENT '订单评价主键ID，自增'
        PRIMARY KEY,
    `order_id`    BIGINT       NULL COMMENT '订单ID',
    `star`        INT          NULL COMMENT '星级评分',
    `content`     TEXT         NULL COMMENT '评价内容',
    `create_time` DATETIME     NULL COMMENT '创建时间',
    INDEX `idx_order_id` (`order_id`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = '订单评价表';
