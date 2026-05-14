# NomNom 餐厅订餐系统 — API 接口文档

> **版本**: v1.0  
> **Base URL**: `http://localhost:8080`  
> **最后更新**: 2026-05-14

---

## 1. 通用约定

### 1.1 统一响应格式

所有接口返回 `application/json`，结构如下：

```json
{
  "success": true,
  "message": "操作描述或错误提示",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | `boolean` | `true` 表示业务成功，`false` 表示失败 |
| `message` | `string` | 成功时为操作描述；失败时为错误原因 |
| `data` | `any` | 成功时携带业务数据；失败时为 `null` |

### 1.2 分页响应格式

当接口涉及分页查询时（后续扩展），`data` 结构为：

```json
{
  "success": true,
  "message": "查询成功",
  "data": {
    "records": [ ],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

### 1.3 错误码约定

HTTP 状态码与业务错误配合使用：

| HTTP 状态码 | 场景 | 示例 message |
|-------------|------|-------------|
| `200` | 业务成功 | `"login success"` |
| `400` | 请求参数错误、业务校验失败 | `"密码错误。"` `"账号不存在。"` |
| `401` | 未登录或 token 失效 | `"未登录或登录已过期"` |
| `403` | 角色权限不足 | `"当前账号无权访问商家端"` |
| `500` | 服务端异常 | `"Server error: ..."` |

### 1.4 认证方式

登录成功后，客户端收到 JWT token。后续请求在 Header 中携带：

```
Authorization: Bearer <token>
```

Token 有效期默认 **24 小时**。过期后需重新登录。

### 1.5 JWT Token 载荷设计

```json
{
  "userId": 1,
  "account": "zhangsan",
  "role": 2,
  "roleCode": "MERCHANT",
  "iat": 1715692800,
  "exp": 1715779200
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `number` | 用户主键 ID |
| `account` | `string` | 用户账号 |
| `role` | `number` | 角色值：`1` = 顾客，`2` = 商家 |
| `roleCode` | `string` | 角色编码：`CUSTOMER` / `MERCHANT` |
| `iat` | `number` | 签发时间戳（秒） |
| `exp` | `number` | 过期时间戳（秒） |

---

## 2. 接口权限矩阵

| 接口 | 方法 | 权限 |
|------|------|------|
| `/api/health` | `GET` | 公开 |
| `/api/auth/login` | `POST` | 公开 |
| `/api/auth/register` | `POST` | 公开 |
| `/api/menu/categories` | `GET` | 公开 |
| `/api/menu/items` | `GET` | 公开 |
| `/api/orders/current` | `GET` | 需登录 |
| `/api/orders/preview` | `POST` | 需登录 |
| `/api/orders` | `POST` | 需登录 |
| `/api/admin/dishes/add` | `POST` | 需商家角色 |
| `/api/admin/tables` | `GET` | 需商家角色 |
| `/api/admin/tables/status` | `PUT` | 需商家角色 |
| `/api/common/upload` | `POST` | 需商家角色 |
| `/uploads/**` | `GET` | 公开（静态资源） |

---

## 3. 认证模块

### 3.1 用户登录

```
POST /api/auth/login
Content-Type: application/json
```

**请求体**:

```json
{
  "account": "zhangsan",
  "password": "123456",
  "roleCode": "CUSTOMER"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `account` | `string` | 是 | 用户账号 |
| `password` | `string` | 是 | 密码，至少 6 位 |
| `roleCode` | `string` | 是 | `"CUSTOMER"`（顾客端）或 `"MERCHANT"`（商家端） |

**成功响应** (200):

```json
{
  "success": true,
  "message": "login success",
  "data": {
    "id": 1,
    "account": "zhangsan",
    "username": "张三",
    "role": 1,
    "roleCode": "CUSTOMER",
    "roleLabel": "顾客端",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsImFjY291bnQiOiJ6aGFuZ3NhbiIsInJvbGUiOjEsInJvbGVDb2RlIjoiQ1VTVE9NRVIiLCJpYXQiOjE3MTU2OTI4MDAsImV4cCI6MTcxNTc3OTIwMH0.xxx"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.id` | `number` | 用户 ID |
| `data.account` | `string` | 账号 |
| `data.username` | `string` | 昵称 |
| `data.role` | `number` | 角色值（1=顾客，2=商家） |
| `data.roleCode` | `string` | 角色编码 |
| `data.roleLabel` | `string` | 角色中文标签 |
| `data.email` | `string \| null` | 邮箱 |
| `data.phone` | `string \| null` | 手机号 |
| `data.token` | `string` | JWT 令牌，后续请求需携带 |

**失败响应** (400):

```json
// 账号不存在
{ "success": false, "message": "账号不存在。", "data": null }

// 密码错误
{ "success": false, "message": "密码错误。", "data": null }

// 角色与入口不匹配（如用顾客账号登录商家端）
{ "success": false, "message": "账号角色与当前入口不匹配。", "data": null }

// 缺少必填字段
{ "success": false, "message": "请输入账号。", "data": null }
{ "success": false, "message": "请输入密码。", "data": null }
{ "success": false, "message": "角色参数非法。", "data": null }
```

---

### 3.2 用户注册

```
POST /api/auth/register
Content-Type: application/json
```

**请求体**:

```json
{
  "account": "lisi",
  "username": "李四",
  "password": "123456",
  "email": "lisi@example.com",
  "phone": "13900139000",
  "roleCode": "CUSTOMER"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `account` | `string` | 是 | 登录账号，不可重复 |
| `username` | `string` | 否 | 昵称，不填则默认使用 account |
| `password` | `string` | 是 | 密码，至少 6 位 |
| `email` | `string` | 否 | 邮箱 |
| `phone` | `string` | 否 | 手机号 |
| `roleCode` | `string` | 是 | `"CUSTOMER"` 或 `"MERCHANT"` |

**成功响应** (200):

```json
{
  "success": true,
  "message": "register success",
  "data": {
    "id": 3,
    "account": "lisi",
    "username": "李四",
    "role": 1,
    "roleCode": "CUSTOMER",
    "roleLabel": "顾客端",
    "email": "lisi@example.com",
    "phone": "13900139000",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**失败响应** (400):

```json
// 账号已存在
{ "success": false, "message": "该账号已存在，请直接登录。", "data": null }

// 密码过短
{ "success": false, "message": "密码至少 6 位。", "data": null }

// 缺少必填字段
{ "success": false, "message": "请输入账号。", "data": null }
{ "success": false, "message": "角色参数非法。", "data": null }
```

---

## 4. 系统模块

### 4.1 健康检查

```
GET /api/health
```

**成功响应** (200):

```json
{
  "success": true,
  "message": "backend ready",
  "data": {
    "status": "UP"
  }
}
```

---

## 5. 菜单模块

### 5.1 获取菜品分类

```
GET /api/menu/categories
```

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "code": "1",
      "name": "火锅",
      "description": ""
    },
    {
      "code": "2",
      "name": "烧烤",
      "description": ""
    },
    {
      "code": "3",
      "name": "饮品",
      "description": ""
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `string` | 分类编码（对应 dishes_type 主键的字符串形式） |
| `name` | `string` | 分类名称 |
| `description` | `string` | 分类描述（取自 icon_uri 字段） |

---

### 5.2 查询菜品列表

```
GET /api/menu/items?category=&keyword=
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `category` | `string` | 否 | 分类编码，筛选指定分类下的菜品（如 `"1"`） |
| `keyword` | `string` | 否 | 搜索关键词，匹配菜品名称、套餐名、规格、描述 |

**请求示例**:

```
GET /api/menu/items?category=1&keyword=毛肚
GET /api/menu/items
GET /api/menu/items?keyword=麻辣
```

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "招牌毛肚 - 大份 / 麻辣",
      "category": "1",
      "categoryLabel": "火锅",
      "price": 68.00,
      "description": "新鲜毛肚，每日空运到店",
      "coverUrl": "/uploads/abc123_maodu.jpg",
      "status": "AVAILABLE",
      "monthlySales": 0
    },
    {
      "id": 2,
      "name": "招牌毛肚 - 小份 / 原味",
      "category": "1",
      "categoryLabel": "火锅",
      "price": 38.00,
      "description": "新鲜毛肚，每日空运到店",
      "coverUrl": "/uploads/abc123_maodu.jpg",
      "status": "AVAILABLE",
      "monthlySales": 0
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `number` | 菜品套餐 ID（`dishes_package.id`） |
| `name` | `string` | 展示名称，由 菜品名 + 套餐名 + 规格 拼接 |
| `category` | `string` | 所属分类编码 |
| `categoryLabel` | `string` | 分类中文名 |
| `price` | `number` | 套餐价格（保留两位小数） |
| `description` | `string` | 菜品详情描述 |
| `coverUrl` | `string` | 封面图路径（相对路径，需拼接 base URL） |
| `status` | `string` | `"AVAILABLE"` = 可售，`"SOLD_OUT"` = 已下架 |
| `monthlySales` | `number` | 月销量（当前固定为 0，预留字段） |

---

## 6. 订单模块

### 6.1 查询当前用户订单

```
GET /api/orders/current?userId=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | `number` | 否 | 用户 ID。不传则返回所有用户最近 20 条订单 |

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "orderNo": "NN20260514153042123",
      "customerName": "张三",
      "status": "PENDING_PAYMENT",
      "totalAmount": 106.00,
      "itemCount": 2
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `orderNo` | `string` | 订单编号，格式 `NN` + 时间戳 + 随机数 |
| `customerName` | `string` | 顾客名称（取 username，无则取 account，无则 "Guest"） |
| `status` | `string` | 订单状态枚举（见下表） |
| `totalAmount` | `number` | 订单总金额 |
| `itemCount` | `number` | 商品总数量 |

**订单状态枚举**:

| 值 | 含义 |
|----|------|
| `PENDING_PAYMENT` | 待支付 |
| `PAID` | 已支付 |
| `PREPARING` | 出餐中 |
| `COMPLETED` | 已完成 |
| `UNKNOWN` | 未知状态 |

---

### 6.2 订单预览（计算金额）

```
POST /api/orders/preview
Authorization: Bearer <token>
Content-Type: application/json
```

下单前调用此接口，验证商品可用性并计算预估金额。

**请求体**:

```json
{
  "userId": 1,
  "customerName": "张三",
  "remark": "少辣，多加蒜",
  "items": [
    {
      "itemId": 1,
      "itemName": "招牌毛肚 - 大份",
      "quantity": 2,
      "unitPrice": 68.00
    },
    {
      "itemId": 3,
      "itemName": "冰粉",
      "quantity": 1,
      "unitPrice": 12.00
    }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | `number` | 否 | 用户 ID |
| `customerName` | `string` | 否 | 顾客姓名，不填则自动生成 |
| `remark` | `string` | 否 | 订单备注 |
| `items` | `array` | 是 | 订单项列表，至少 1 项 |
| `items[].itemId` | `number` | 是 | 套餐 ID（对应 `dishes_package.id`） |
| `items[].itemName` | `string` | 否 | 商品名称（仅展示用） |
| `items[].quantity` | `number` | 是 | 购买数量，必须 > 0 |
| `items[].unitPrice` | `number` | 否 | 单价（仅展示用，实际以数据库为准） |

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": {
    "customerName": "张三",
    "remark": "少辣，多加蒜",
    "itemCount": 3,
    "totalAmount": 148.00,
    "suggestion": "Connected to database. Preview includes item count, total amount, and order remark."
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `customerName` | `string` | 解析后的顾客名称 |
| `remark` | `string` | 订单备注 |
| `itemCount` | `number` | 商品总数量（各 item 数量之和） |
| `totalAmount` | `number` | 预估总金额 |
| `suggestion` | `string` | 系统提示信息 |

**失败响应** (400):

```json
// 订单项为空
{ "success": false, "message": "Order items cannot be empty.", "data": null }

// 套餐 ID 不存在
{ "success": false, "message": "Package ids not found: [999]", "data": null }

// 数量无效
{ "success": false, "message": "Quantity must be greater than 0.", "data": null }

// 商品已下架
{ "success": false, "message": "Package 1 is currently unavailable.", "data": null }
```

---

### 6.3 创建订单

```
POST /api/orders
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**: 与 [6.2 订单预览](#62-订单预览计算金额) 完全相同。

**成功响应** (200):

```json
{
  "success": true,
  "message": "order created",
  "data": {
    "orderNo": "NN20260514153042123",
    "customerName": "张三",
    "status": "PENDING_PAYMENT",
    "totalAmount": 148.00,
    "itemCount": 3
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `orderNo` | `string` | 生成的订单编号 |
| `customerName` | `string` | 顾客名称 |
| `status` | `string` | 新订单状态固定为 `"PENDING_PAYMENT"` |
| `totalAmount` | `number` | 订单总金额 |
| `itemCount` | `number` | 商品总数量 |

**失败响应** (400): 同 [6.2 订单预览](#62-订单预览计算金额) 的校验错误。

---

## 7. 商家管理模块

> 以下接口需要商家角色（`roleCode = "MERCHANT"`），请求头需携带商家 token。

### 7.1 菜品列表（商家端）

```
GET /api/admin/dishes
Authorization: Bearer <token>
```

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "id": 1,
      "typeId": 1,
      "name": "招牌毛肚",
      "detail": "新鲜毛肚，每日空运到店",
      "coverUrl": "/uploads/maodu.jpg",
      "status": 1,
      "stock": 200,
      "memberPrice": 58.00,
      "specs": "{\"sizes\":[\"大份\",\"小份\"],\"spiciness\":[\"微辣\",\"中辣\",\"特辣\"]}",
      "createTime": "2026-05-14T04:15:00.000+00:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `number` | 菜品 ID |
| `typeId` | `number` | 分类 ID |
| `name` | `string` | 菜品名称 |
| `detail` | `string` | 菜品详情 |
| `coverUrl` | `string` | 封面图路径 |
| `status` | `number` | `1` = 上架，`0` = 下架 |
| `stock` | `number` | 库存数量 |
| `memberPrice` | `number \| null` | 会员价 |
| `specs` | `string \| null` | 规格 JSON |
| `createTime` | `string` | 创建时间 |

---

### 7.2 菜品详情

```
GET /api/admin/dishes/{id}
Authorization: Bearer <token>
```

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": {
    "id": 1,
    "typeId": 1,
    "name": "招牌毛肚",
    "detail": "新鲜毛肚，每日空运到店",
    "coverUrl": "/uploads/maodu.jpg",
    "status": 1,
    "stock": 200,
    "memberPrice": 58.00,
    "specs": "{\"sizes\":[\"大份\",\"小份\"]}",
    "createTime": "2026-05-14T04:15:00.000+00:00",
    "packages": [
      {
        "id": 1,
        "dishesId": 1,
        "name": "大份",
        "specs": "麻辣",
        "price": 78.00,
        "createTime": "2026-05-14T04:15:00"
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `packages` | `array` | 该菜品下的套餐列表 |
| `packages[].id` | `number` | 套餐 ID |
| `packages[].name` | `string` | 套餐名称 |
| `packages[].price` | `number` | 套餐价格 |

**失败响应** (200 `success=false`):

```json
{ "success": false, "message": "菜品不存在", "data": null }
```

---

### 7.3 新增菜品

```
POST /api/admin/dishes/add
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**:

```json
{
  "type_id": 1,
  "name": "金汤肥牛",
  "detail": "酸辣鲜香，配金针菇打底",
  "cover_url": "/uploads/xxx_jintangfeiniu.jpg",
  "status": 1,
  "stock": 200,
  "price": 68.00,
  "memberPrice": 58.00,
  "specs": ["大份", "小份"],
  "tastes": ["微辣", "中辣"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type_id` | `number` | 否 | 分类 ID |
| `name` | `string` | 是 | 菜品名称 |
| `detail` | `string` | 否 | 菜品详情描述 |
| `cover_url` | `string` | 否 | 封面图路径 |
| `status` | `number` | 否 | `1` = 上架，`0` = 下架，默认 `1` |
| `stock` | `number` | 否 | 库存数量，默认 `999` |
| `price` | `number` | 否 | 基础售价（同步创建默认套餐"标准份"） |
| `memberPrice` | `number` | 否 | 会员价 |
| `specs` | `array` | 否 | 规格列表，如 `["大份", "小份"]` |
| `tastes` | `array` | 否 | 口味列表，如 `["微辣", "中辣"]` |

> 保存时自动创建一条默认套餐（名称"标准份"，价格取 `price` 字段），并自动将 `specs` + `tastes` 合并为 JSON 存入 `dishes.specs`。

**成功响应** (200):

```json
{ "success": true, "message": "新增成功", "data": null }
```

---

### 7.4 编辑菜品

```
PUT /api/admin/dishes/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**: 与 [7.3 新增菜品](#73-新增菜品) 完全相同。注意此接口只更新 `dishes` 表，不修改已有套餐。

**成功响应** (200):

```json
{ "success": true, "message": "更新成功", "data": null }
```

**失败响应**:

```json
{ "success": false, "message": "菜品不存在", "data": null }
```

---

### 7.5 上架/下架

```
PUT /api/admin/dishes/{id}/status?status=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | `number` | 是 | `1` = 上架，`0` = 下架 |

**成功响应** (200):

```json
{ "success": true, "message": "已上架", "data": null }
```

---

### 7.6 删除菜品

```
DELETE /api/admin/dishes/{id}
Authorization: Bearer <token>
```

> 级联删除该菜品下的所有套餐（`dishes_package`），再删除菜品本身。

**成功响应** (200):

```json
{ "success": true, "message": "删除成功", "data": null }
```

**失败响应**:

```json
{ "success": false, "message": "菜品不存在", "data": null }
```

---

### 7.7 分类列表

```
GET /api/admin/categories
Authorization: Bearer <token>
```

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    { "id": 1, "iconUri": "/icons/hotpot.png", "name": "火锅" },
    { "id": 2, "iconUri": "/icons/bbq.png", "name": "烧烤" }
  ]
}
```

---

### 7.8 新增分类

```
POST /api/admin/categories
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**:

```json
{ "name": "新分类", "iconUri": "/icons/new.png" }
```

**成功响应**: `{ "success": true, "message": "新增成功", "data": null }`

---

### 7.9 编辑分类

```
PUT /api/admin/categories/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**: 同 [7.8 新增分类](#78-新增分类)

**成功响应**: `{ "success": true, "message": "更新成功", "data": null }`

---

### 7.10 删除分类

```
DELETE /api/admin/categories/{id}
Authorization: Bearer <token>
```

**成功响应**: `{ "success": true, "message": "删除成功", "data": null }`

---

### 7.11 仪表盘

```
GET /api/admin/dashboard
Authorization: Bearer <token>
```

**成功响应**:

```json
{
  "success": true,
  "data": {
    "todayOrderCount": 3,
    "todayRevenue": 456.00,
    "totalOrders": 8,
    "pendingCount": 2,
    "paidCount": 1,
    "preparingCount": 3,
    "completedCount": 2,
    "topDishes": [
      { "name": "招牌毛肚", "sales": 12 },
      { "name": "手工虾滑", "sales": 8 }
    ]
  }
}
```

| 字段 | 说明 |
|------|------|
| `todayOrderCount` | 今日订单数 |
| `todayRevenue` | 今日营业额 |
| `totalOrders` | 历史总订单 |
| `pending/paid/preparing/completedCount` | 各状态订单数 |
| `topDishes` | 销量 Top 5（按 buy_number 汇总） |

---

### 7.12 订单列表（商家端）

```
GET /api/admin/orders?status=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | `number` | 否 | 1=待支付，2=已支付，3=出餐中，4=已完成 |

**成功响应**: `{ "success": true, "data": [{ "id": 1, "code": "NN...", "userId": 1, "status": 1, "totalPrice": 148.00, "createTime": "..." }] }`

---

### 7.12 更新订单状态

```
PUT /api/admin/orders/{id}/status?status=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | `number` | 是 | 目标状态值 |

---

### 7.13 新增桌台

```
POST /api/admin/tables
Authorization: Bearer <token>
```

**请求体**: `{ "number": "C01", "personNumber": 6, "area": "三楼", "minSpend": 200.00 }`

---

### 7.14 编辑桌台

```
PUT /api/admin/tables/{id}
Authorization: Bearer <token>
```

**请求体**: 同 [7.13 新增桌台](#713-新增桌台)

---

### 7.15 删除桌台

```
DELETE /api/admin/tables/{id}
Authorization: Bearer <token>
```

---

### 7.16 查询桌台列表

```
GET /api/admin/tables
Authorization: Bearer <token>
```

**成功响应** (200):

```json
{
  "success": true,
  "message": "获取桌台成功",
  "data": [
    {
      "id": 1,
      "number": "A01",
      "personNumber": 2,
      "status": 0,
      "area": "大厅",
      "amount": 0.00,
      "minSpend": 0.00,
      "createTime": "2026-05-14T12:00:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `number` | 桌台 ID |
| `number` | `string` | 桌台编号 |
| `personNumber` | `number` | 可容纳人数 |
| `status` | `number` | 0=空闲，1=已入座，2=就餐中 |
| `area` | `string` | 所属区域 |
| `amount` | `number` | 当前消费总额 |
| `minSpend` | `number` | 最低消费 |

---

### 7.8 更新桌台状态

```
PUT /api/admin/tables/status?tableId=&status=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `tableId` | `number` | 是 | 桌台 ID |
| `status` | `number` | 是 | 目标状态（0=空闲，1=已入座，2=就餐中） |

> 设为 0（空闲）时自动清零消费金额。

**成功响应** (200):

```json
{ "success": true, "message": "状态更新成功", "data": null }
```

**失败响应**:

```json
{ "success": false, "message": "桌台不存在", "data": null }
```

---

## 8. 文件上传模块

### 8.1 上传图片

```
POST /api/common/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | `file` | 是 | 图片文件 |

**成功响应** (200):

```json
{
  "success": true,
  "message": "上传成功",
  "data": "/uploads/a1b2c3d4_restaurant.png"
}
```

`data` 返回上传后的相对路径，完整访问 URL 为 `http://localhost:8080/uploads/a1b2c3d4_restaurant.png`。

**失败响应** (200 `success=false`):

```json
// 文件为空
{ "success": false, "message": "文件不能为空", "data": null }

// 上传失败
{ "success": false, "message": "上传失败: 磁盘空间不足", "data": null }
```

> **注意**: 文件存储在服务端 `D:/nomnom_uploads/` 目录，通过 `/uploads/**` 映射对外访问。

---

## 9. 附录

### 9.1 角色说明

| role 值 | roleCode | roleLabel | 说明 |
|---------|----------|-----------|------|
| `1` | `CUSTOMER` | 顾客端 | 浏览菜单、下单、查看自己的订单 |
| `2` | `MERCHANT` | 商家端 | 菜品管理、桌台管理、上传图片 |

### 9.2 HTTP 方法约定

| 方法 | 用途 |
|------|------|
| `GET` | 查询资源 |
| `POST` | 创建资源、提交操作 |
| `PUT` | 更新资源 |

### 9.3 接口总览

| 序号 | 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|------|
| 1 | `GET` | `/api/health` | 健康检查 | 公开 |
| 2 | `POST` | `/api/auth/login` | 用户登录 | 公开 |
| 3 | `POST` | `/api/auth/register` | 用户注册 | 公开 |
| 4 | `GET` | `/api/menu/categories` | 获取菜品分类 | 公开 |
| 5 | `GET` | `/api/menu/items` | 查询菜品列表 | 公开 |
| 6 | `GET` | `/api/orders/current` | 查询订单 | 需登录 |
| 7 | `POST` | `/api/orders/preview` | 订单预览 | 需登录 |
| 8 | `POST` | `/api/orders` | 创建订单 | 需登录 |
| 9 | `GET` | `/api/admin/dishes` | 菜品列表（商家端） | 需商家 |
| 10 | `GET` | `/api/admin/dishes/{id}` | 菜品详情（含套餐） | 需商家 |
| 11 | `POST` | `/api/admin/dishes/add` | 新增菜品 | 需商家 |
| 12 | `PUT` | `/api/admin/dishes/{id}` | 编辑菜品 | 需商家 |
| 13 | `PUT` | `/api/admin/dishes/{id}/status` | 上架/下架 | 需商家 |
| 14 | `DELETE` | `/api/admin/dishes/{id}` | 删除菜品 | 需商家 |
| 15 | `GET` | `/api/admin/categories` | 分类列表 | 需商家 |
| 16 | `POST` | `/api/admin/categories` | 新增分类 | 需商家 |
| 17 | `PUT` | `/api/admin/categories/{id}` | 编辑分类 | 需商家 |
| 18 | `DELETE` | `/api/admin/categories/{id}` | 删除分类 | 需商家 |
| 19 | `GET` | `/api/admin/dashboard` | 仪表盘 | 需商家 |
| 20 | `GET` | `/api/admin/orders` | 订单列表 | 需商家 |
| 21 | `PUT` | `/api/admin/orders/{id}/status` | 更新订单状态 | 需商家 |
| 21 | `GET` | `/api/admin/tables` | 查询桌台 | 需商家 |
| 22 | `POST` | `/api/admin/tables` | 新增桌台 | 需商家 |
| 23 | `PUT` | `/api/admin/tables/{id}` | 编辑桌台 | 需商家 |
| 24 | `DELETE` | `/api/admin/tables/{id}` | 删除桌台 | 需商家 |
| 25 | `PUT` | `/api/admin/tables/status` | 更新桌台状态 | 需商家 |
| 26 | `POST` | `/api/common/upload` | 上传图片 | 需商家 |
