# NomNom 餐厅订餐系统

NomNom 是一个面向堂食场景的前后端分离订餐系统，支持顾客选桌、浏览菜单、收藏菜品、购物车下单、钱包支付、订单追踪、评价反馈和退款申请；商家端支持菜品与分类维护、桌台管理、订单流转、退款审批、评价回复、财务对账和销量统计。

## 项目结构

```text
NomNom/
├── backend/                  # Spring Boot 后端服务
├── frontend/                 # Vue 3 + Vite 前端
├── database/
│   ├── nomnom.sql            # 建表脚本
│   └── nomnom-demo-data.sql  # 演示数据
└── docs/
    └── API.md                # 接口文档
```

## 技术栈

- 后端：Java 17、Spring Boot 2.7、MyBatis-Plus、MySQL 8、JWT、BCrypt
- 前端：Vue 3、Vite、Vue Router、Axios、Fetch
- 数据库：`nomnom.sql` 初始化结构，`nomnom-demo-data.sql` 初始化答辩演示数据
- 静态资源：上传图片默认保存到 `backend/uploads/`，通过 `/uploads/**` 访问

## 核心功能

| 端 | 功能 |
| --- | --- |
| 顾客端 | 登录注册、选桌入座、分类点餐、推荐菜品、收藏、购物车、钱包充值与支付、订单查看、评价、退款申请、消息提醒 |
| 商家端 | 菜品/分类管理、库存快速修改、上下架、桌台状态管理、订单状态流转、异常订单清理、退款审批、评价回复、财务对账、销量统计、图片上传 |

## 本地启动

1. 创建 MySQL 数据库：

```sql
CREATE DATABASE nomnom DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 导入建表脚本和演示数据：

```bash
mysql -u root -p nomnom < database/nomnom.sql
mysql -u root -p nomnom < database/nomnom-demo-data.sql
```

3. 启动后端：

```bash
cd backend
mvn spring-boot:run
```

4. 启动前端：

```bash
cd frontend
npm install
npm run dev
```

5. 浏览器访问 `http://localhost:5173`。Vite 已配置 `/api` 与 `/uploads` 代理到 `http://localhost:8080`。

后端启动时会根据 `NOMNOM_DEMO_DATA_ENABLED` 自动补齐演示数据。若只想使用空库，可在启动前设置：

```powershell
$env:NOMNOM_DEMO_DATA_ENABLED = "false"
```

## 常用配置

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `NOMNOM_DB_URL` | `jdbc:mysql://localhost:3306/nomnom?...` | 数据库连接地址 |
| `NOMNOM_DB_USERNAME` | `root` | 数据库用户名 |
| `NOMNOM_DB_PASSWORD` | `ghy0910@` | 数据库密码 |
| `NOMNOM_JWT_SECRET` | 内置开发密钥 | JWT 签名密钥 |
| `NOMNOM_UPLOAD_PATH` | `uploads` | 上传文件保存目录 |
| `NOMNOM_DEMO_DATA_ENABLED` | `true` | 是否自动导入演示数据 |

## 演示账号

| 场景 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 顾客完整下单演示 | `demo_customer` | `123456` | 钱包有余额、无未完成桌台订单，适合演示选桌、点餐、支付 |
| 商家管理演示 | `merchant1` | `123456` | 可演示菜品、桌台、订单、审批、评价和统计 |

## 推荐答辩流程

1. 使用 `demo_customer` 登录顾客端，选择空闲桌台，浏览菜单，收藏菜品并加入购物车。
2. 使用钱包余额或充值后下单支付，展示订单回执、钱包流水和桌台占用变化。
3. 切换到 `merchant1` 商家端，查看新订单，将订单从已支付推进到制作中、已完成。
4. 回到顾客端查看订单状态，进入已完成订单提交评价或退款申请。
5. 商家端回复评价、处理退款审批，并查看消息、财务对账和销量统计。

接口详情见 [docs/API.md](docs/API.md)。
