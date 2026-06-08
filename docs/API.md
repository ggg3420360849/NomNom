# NomNom 餐厅订餐系统 — API 接口文档

> **版本**: v1.2  
> **Base URL**: `http://localhost:8080`  
> **前端开发地址**: `http://localhost:5173`  
> **最后更新**: 2026-06-08

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
| `/api/menu/recommend/{packageId}` | `GET` | 公开 |
| `/api/evaluations/dish/{dishesId}` | `GET` | 公开 |
| `/api/tables/available` | `GET` | 需登录 |
| `/api/tables/{id}/seat` | `PUT` | 需登录 |
| `/api/tables/{id}/release` | `PUT` | 需登录 |
| `/api/orders/current` | `GET` | 需登录 |
| `/api/orders/preview` | `POST` | 需登录 |
| `/api/orders` | `POST` | 需登录 |
| `/api/customer/tables` | `GET` | 公开 |
| `/api/customer/**` | `*` | 需登录（`/api/customer/tables` 除外） |
| `/api/cart/**` | `*` | 需登录 |
| `/api/collection/**` | `*` | 需登录 |
| `/api/evaluations/**` | `POST/PUT` | 需登录 |
| `/api/messages/**` | `*` | 需登录 |
| `/api/refunds/**` | `*` | 需登录（商家操作需商家角色） |
| `/api/wallet/**` | `*` | 需登录 |
| `/api/admin/**` | `*` | 需商家角色 |
| `/api/common/upload` | `POST` | 需登录 |
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
      "dishesId": 1,
      "name": "招牌毛肚 - 大份 / 麻辣",
      "category": "1",
      "categoryLabel": "火锅",
      "price": 68.00,
      "description": "新鲜毛肚，每日空运到店",
      "coverUrl": "/uploads/abc123_maodu.jpg",
      "status": "AVAILABLE",
      "monthlySales": 0,
      "recommend": true
    },
    {
      "id": 2,
      "dishesId": 1,
      "name": "招牌毛肚 - 小份 / 原味",
      "category": "1",
      "categoryLabel": "火锅",
      "price": 38.00,
      "description": "新鲜毛肚，每日空运到店",
      "coverUrl": "/uploads/abc123_maodu.jpg",
      "status": "AVAILABLE",
      "monthlySales": 0,
      "recommend": false
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `number` | 菜品套餐 ID（`dishes_package.id`） |
| `dishesId` | `number` | 菜品 ID（`dishes.id`） |
| `name` | `string` | 展示名称，由 菜品名 + 套餐名 + 规格 拼接 |
| `category` | `string` | 所属分类编码 |
| `categoryLabel` | `string` | 分类中文名 |
| `price` | `number` | 套餐价格（保留两位小数） |
| `description` | `string` | 菜品详情描述 |
| `coverUrl` | `string` | 封面图路径（相对路径，需拼接 base URL） |
| `status` | `string` | `"AVAILABLE"` = 可售，`"SOLD_OUT"` = 已下架 |
| `monthlySales` | `number` | 月销量（当前固定为 0，预留字段） |
| `recommend` | `boolean` | 是否为推荐菜品 |

---

### 5.3 查询推荐菜品

```
GET /api/menu/recommend/{packageId}
```

根据当前套餐 ID 查询关联推荐菜品，用于顾客端购物车/菜品详情中的“搭配推荐”场景。

**成功响应** (200): 同 [5.2 查询菜品列表](#52-查询菜品列表)，返回 `MenuItem[]`。

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

### 6.4 查询空闲桌台

```
GET /api/tables/available
Authorization: Bearer <token>
```

返回状态为 `0`（空闲）的桌台，按桌台号升序排列。

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "id": 1,
      "number": "A01",
      "personNumber": 4,
      "status": 0,
      "amount": 0.00,
      "occupiedBy": null
    }
  ]
}
```

---

### 6.5 选择桌台

```
PUT /api/tables/{id}/seat
Authorization: Bearer <token>
```

顾客入座指定桌台。若桌台已被当前用户占用，会直接返回“桌台已选择”；若被其他用户占用或维修中，返回 `success=false`。

**成功响应**: `{ "success": true, "message": "桌台已选择", "data": { "id": 1, "status": 1, "occupiedBy": 1 } }`

---

### 6.6 释放桌台

```
PUT /api/tables/{id}/release
Authorization: Bearer <token>
```

仅允许释放当前用户选择且没有消费金额的桌台。已有消费记录时不能直接换桌。

**成功响应**: `{ "success": true, "message": "桌台已释放", "data": { "id": 1, "status": 0, "occupiedBy": null } }`

---

### 6.7 查询全部桌台（顾客兼容）

```
GET /api/customer/tables
```

公开返回所有桌台，按区域与桌台号排序。新版顾客选桌优先使用 [6.4 查询空闲桌台](#64-查询空闲桌台)。

**成功响应**: 同 [6.4 查询空闲桌台](#64-查询空闲桌台)，但包含全部状态的桌台。

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
      "typeName": "火锅",
      "name": "招牌毛肚",
      "detail": "新鲜毛肚，每日空运到店",
      "coverUrl": "/uploads/maodu.jpg",
      "status": 1,
      "stock": 200,
      "price": 68.00,
      "specs": "{\"sizes\":[\"大份\",\"小份\"],\"spiciness\":[\"微辣\",\"中辣\",\"特辣\"]}",
      "isRecommend": 1,
      "recommendIds": "2,3",
      "createTime": "2026-05-14T04:15:00.000+00:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `number` | 菜品 ID |
| `typeId` | `number` | 分类 ID |
| `typeName` | `string` | 分类名称 |
| `name` | `string` | 菜品名称 |
| `detail` | `string` | 菜品详情 |
| `coverUrl` | `string` | 封面图路径 |
| `status` | `number` | `1` = 上架，`0` = 下架 |
| `stock` | `number` | 库存数量 |
| `price` | `number` | 当前最低套餐价 |
| `specs` | `string \| null` | 规格 JSON |
| `isRecommend` | `number` | `1` = 推荐菜品，`0` = 普通菜品 |
| `recommendIds` | `string \| null` | 关联推荐菜品 ID，逗号分隔 |
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
    "specs": "{\"sizes\":[\"大份\",\"小份\"]}",
    "isRecommend": 1,
    "recommendIds": "2,3",
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
  "isRecommend": 1,
  "recommendIds": "2,3",
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
| `isRecommend` | `number` | 否 | `1` = 推荐菜品，`0` = 普通菜品 |
| `recommendIds` | `string` | 否 | 关联推荐菜品 ID，逗号分隔 |
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

**请求体**: 与 [7.3 新增菜品](#73-新增菜品) 完全相同。若传入 `price` 且大于 0，会同步更新当前菜品价格最低的一条套餐。

**成功响应** (200):

```json
{ "success": true, "message": "更新成功", "data": null }
```

**失败响应**:

```json
{ "success": false, "message": "菜品不存在", "data": null }
```

---

### 7.4.1 快速修改库存

```
PUT /api/admin/dishes/{id}/stock?stock=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `stock` | `number` | 是 | 新库存数量，不能小于 `0` |

**成功响应** (200):

```json
{ "success": true, "message": "库存已更新", "data": null }
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
| `status` | `number` | 否 | `1`=待支付，`2`=已支付，`3`=制作中，`4`=已完成，`5`=已取消 |

**成功响应**: `{ "success": true, "data": [{ "id": 1, "code": "NN...", "userId": 1, "status": 1, "totalPrice": 148.00, "createTime": "..." }] }`

---

### 7.13 更新订单状态

```
PUT /api/admin/orders/{id}/status?status=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | `number` | 是 | 目标状态值：`1`=待支付，`2`=已支付，`3`=制作中，`4`=已完成，`5`=已取消 |

> 更新订单状态后会同步相关桌台占用状态，并给顾客写入订单消息提醒。

**成功响应**: `{ "success": true, "message": "状态更新成功", "data": null }`

---

### 7.14 清理异常订单

```
DELETE /api/admin/orders/invalid
Authorization: Bearer <token>
```

扫描并删除缺失订单号、用户、有效状态、金额或订单项的异常订单，同时清理关联订单项、评价、评价图片和退款记录。

**成功响应** (200):

```json
{
  "success": true,
  "message": "异常订单已清理",
  "data": {
    "invalidOrderIds": [12, 15],
    "deletedOrders": 2,
    "deletedItems": 4,
    "deletedEvaluations": 1,
    "deletedRefunds": 1,
    "deletedImages": 2
  }
}
```

---

### 7.15 新增桌台

```
POST /api/admin/tables
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**:

```json
{
  "number": "C01",
  "personNumber": 6,
  "status": 0
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `number` | `string` | 是 | 桌台号，不能重复 |
| `personNumber` | `number` | 否 | 可容纳人数 |
| `status` | `number` | 否 | `0`=空闲，`1`=已入座，`6`=维修中；默认 `0` |

> 后端会忽略前端传入的 `area` 和 `minSpend`，统一保存为空区域、最低消费 `0`。

---

### 7.16 编辑桌台

```
PUT /api/admin/tables/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**: 同 [7.15 新增桌台](#715-新增桌台)。

**成功响应**: `{ "success": true, "message": "更新成功", "data": null }`

---

### 7.17 删除桌台

```
DELETE /api/admin/tables/{id}
Authorization: Bearer <token>
```

**成功响应**: `{ "success": true, "message": "删除成功", "data": null }`

---

### 7.18 查询桌台列表

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
      "area": null,
      "amount": 0.00,
      "minSpend": 0.00,
      "occupiedBy": null,
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
| `status` | `number` | `0`=空闲，`1`=已入座，`6`=维修中 |
| `area` | `string \| null` | 当前项目保留字段，商家端创建/编辑时统一置空 |
| `amount` | `number` | 当前消费总额 |
| `minSpend` | `number` | 最低消费 |
| `occupiedBy` | `number \| null` | 当前占用该桌台的用户 ID |

---

### 7.19 查询顾客可选桌台（商家路径）

```
GET /api/admin/customer/tables
Authorization: Bearer <token>
```

返回当前可供顾客选择的空闲桌台。该接口主要服务商家端或旧版前端兼容场景；顾客端常用 `/api/tables/available`。

**成功响应**: 同 [7.18 查询桌台列表](#718-查询桌台列表)，但仅包含 `status = 0` 的桌台。

---

### 7.20 更新桌台状态

```
PUT /api/admin/tables/status?tableId=&status=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `tableId` | `number` | 是 | 桌台 ID |
| `status` | `number` | 是 | 目标状态：`0`=空闲，`1`=已入座，`6`=维修中 |

> 设为 `0`（空闲）或 `6`（维修中）时自动清零消费金额并释放占用用户。

**成功响应** (200):

```json
{ "success": true, "message": "桌台已设为空闲", "data": null }
```

**失败响应**:

```json
{ "success": false, "message": "桌台不存在", "data": null }
```

---

### 7.21 审核列表

```
GET /api/admin/audits
Authorization: Bearer <token>
```

> 返回退款申请及充值流水相关的审核/对账线索，按创建时间倒序排列。

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "id": 1,
      "type": "REFUND",
      "desc": "订单 NN20260518100000123 退款原因：菜品份量太少",
      "status": "PENDING",
      "createTime": "2026-05-18T10:00:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `number \| string` | 审核项 ID；退款项也可使用 `REFUND-{id}` |
| `type` | `string` | 审核类型，如 `REFUND`、`RECHARGE` |
| `desc` | `string` | 申请详情描述 |
| `status` | `string` | `PENDING` / `APPROVED` / `REJECTED` / `NONE` |

---

### 7.22 审核操作

```
PUT /api/admin/audit/{id}?approved=&cause=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `approved` | `boolean` | 是 | `true` = 通过，`false` = 驳回 |
| `cause` | `string` | 否 | 驳回原因；驳回退款时建议填写 |

> `{id}` 支持纯退款 ID 或 `REFUND-{id}`。充值流水无需审批，传入充值记录会返回 `success=false`。

**成功响应**:

```json
{ "success": true, "message": "审批已通过", "data": null }
{ "success": true, "message": "申请已驳回", "data": null }
```

---

### 7.23 财务对账

```
GET /api/admin/finance/reconciliation
Authorization: Bearer <token>
```

> 需要商家角色。返回所有订单的消费与流水对账数据。

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "id": 1,
      "orderAmt": 148.00,
      "flowAmt": 148.00
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `number` | 审计凭证号（订单 ID） |
| `orderAmt` | `number` | 应用端消费金额 |
| `flowAmt` | `number` | 底层流水金额 |

---

### 7.24 评价列表（商家端）

```
GET /api/admin/reviews
Authorization: Bearer <token>
```

> 需要商家角色。返回所有菜品评价，含用户和菜品信息。

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "id": 1,
      "user": "张三",
      "dishName": "招牌毛肚",
      "rating": 5,
      "content": "非常好吃！",
      "reply": "感谢您的评价！"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `user` | `string` | 消费者昵称 |
| `dishName` | `string` | 受评菜品名称 |
| `rating` | `number` | 评分 1-5 |
| `content` | `string` | 评语内容 |
| `reply` | `string \| null` | 商家回复内容 |

---

### 7.25 回复评价（商家端）

```
POST /api/admin/reviews/{id}/reply
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**:

```json
{ "reply": "感谢您的支持！" }
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reply` | `string` | 是 | 回复内容 |

> 需要商家角色。

**成功响应**: `{ "success": true, "message": "回复成功", "data": null }`

---

### 7.26 删除评价（商家端）

```
DELETE /api/admin/reviews/{id}
Authorization: Bearer <token>
```

> 需要商家角色。删除评价及其关联图片。

**成功响应**: `{ "success": true, "message": "评价已删除", "data": null }`

---

### 7.27 统计分析

```
GET /api/admin/stats
Authorization: Bearer <token>
```

> 需要商家角色。返回菜品销量排名及营收统计。

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "rank": 1,
      "name": "招牌毛肚",
      "sales": 25,
      "revenue": 1700.00
    },
    {
      "rank": 2,
      "name": "手工虾滑",
      "sales": 18,
      "revenue": 1224.00
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `rank` | `number` | 销量排名 |
| `name` | `string` | 菜品名称 |
| `sales` | `number` | 累积销量 |
| `revenue` | `number` | 账面营收（单价 × 销量） |

---

## 8. 购物车模块

> 以下接口需要登录（顾客端），请求头需携带 JWT token。

### 8.1 查看购物车

```
GET /api/cart
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
      "userId": 1,
      "dishesId": 1,
      "packageId": 1,
      "buyNumber": 2,
      "isSelected": 1,
      "dishName": "招牌毛肚",
      "dishCover": "/uploads/maodu.jpg",
      "packageName": "标准份",
      "price": 68.00,
      "specs": ""
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `number` | 购物车项 ID |
| `dishesId` | `number` | 菜品 ID |
| `packageId` | `number` | 套餐 ID |
| `buyNumber` | `number` | 购买数量 |
| `isSelected` | `number` | `1` = 选中，`0` = 未选中 |
| `dishName` | `string` | 菜品名称 |
| `dishCover` | `string` | 菜品封面 |
| `packageName` | `string` | 套餐名称 |
| `price` | `number` | 单价 |
| `specs` | `string` | 规格描述 |

---

### 8.2 加入购物车

```
POST /api/cart
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**:

```json
{
  "dishes_id": 1,
  "package_id": 1,
  "buy_number": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `dishes_id` | `number` | 是 | 菜品 ID |
| `package_id` | `number` | 是 | 套餐 ID |
| `buy_number` | `number` | 否 | 数量，默认 `1` |

> 若该用户的购物车中已存在相同 `package_id`，则累加数量，不重复插入。

**成功响应**: `{ "success": true, "message": "已加入购物车", "data": null }`

---

### 8.3 修改数量

```
PUT /api/cart/{id}/quantity?buyNumber=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `buyNumber` | `number` | 是 | 新数量 |

**成功响应**: `{ "success": true, "message": "更新成功", "data": null }`

---

### 8.4 切换选中状态

```
PUT /api/cart/{id}/selected
Authorization: Bearer <token>
```

**成功响应**: `{ "success": true, "message": "ok", "data": null }`

---

### 8.5 移除单项

```
DELETE /api/cart/{id}
Authorization: Bearer <token>
```

**成功响应**: `{ "success": true, "message": "已移除", "data": null }`

---

### 8.6 清空购物车

```
DELETE /api/cart
Authorization: Bearer <token>
```

> 清空当前用户购物车的所有商品。

**成功响应**: `{ "success": true, "message": "已清空", "data": null }`

---

## 9. 收藏模块

> 以下接口需要登录（顾客端），请求头需携带 JWT token。

### 9.1 收藏列表

```
GET /api/collection
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
      "dishesId": 1,
      "packageId": 3,
      "dishName": "招牌毛肚",
      "dishCover": "/uploads/maodu.jpg",
      "price": 68.00,
      "dishStatus": 1
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `number` | 收藏记录 ID |
| `dishesId` | `number` | 菜品 ID |
| `packageId` | `number` | 该菜品最低价套餐 ID，可用于顾客端加购 |
| `dishName` | `string` | 菜品名称 |
| `dishCover` | `string` | 菜品封面 |
| `price` | `number` | 该菜品最低套餐价 |
| `dishStatus` | `number` | `1` = 上架，`0` = 下架 |

---

### 9.2 检查是否已收藏

```
GET /api/collection/check?dishesId=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `dishesId` | `number` | 是 | 菜品 ID |

**成功响应**: `{ "success": true, "message": "success", "data": true }`

---

### 9.3 切换收藏

```
POST /api/collection/toggle?dishesId=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `dishesId` | `number` | 是 | 菜品 ID |

> 未收藏则添加收藏，已收藏则取消。

**成功响应**:
```json
{ "success": true, "message": "已收藏", "data": true }
{ "success": true, "message": "已取消收藏", "data": false }
```

---

## 10. 评价模块

> 查询评价公开访问，提交/回复评价需登录。

### 10.1 查看菜品评价

```
GET /api/evaluations/dish/{dishesId}
```

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "id": 1,
      "dishesId": 1,
      "userId": 1,
      "userName": "张三",
      "userAvatar": null,
      "content": "非常好吃！",
      "ratingValue": 5,
      "replyContent": "感谢您的评价！",
      "replyTime": "2026-05-18T12:00:00",
      "replyStatus": 1,
      "createTime": "2026-05-17T10:30:00",
      "imageUrls": ["/uploads/eval_img1.jpg", "/uploads/eval_img2.jpg"]
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `userName` | `string` | 评价用户昵称 |
| `ratingValue` | `number` | 评分 1-5 |
| `replyStatus` | `number` | `0` = 未回复，`1` = 已回复 |
| `imageUrls` | `array` | 评价图片链接列表 |

---

### 10.2 查看我的评价

```
GET /api/evaluations/my
Authorization: Bearer <token>
```

**成功响应**: 同 [10.1](#101-查看菜品评价)，仅返回当前用户的评价。

---

### 10.3 提交评价

```
POST /api/evaluations
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**:

```json
{
  "dishesId": 1,
  "ordersId": 1,
  "ratingValue": 5,
  "content": "非常好吃！",
  "imageUrls": ["/uploads/eval_img1.jpg"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `dishesId` | `number` | 是 | 菜品 ID |
| `ordersId` | `number` | 是 | 订单 ID（验证消费身份） |
| `ratingValue` | `number` | 是 | 评分 1-5 |
| `content` | `string` | 否 | 评价内容 |
| `imageUrls` | `array` | 否 | 图片链接数组 |

> 仅订单状态为 `COMPLETED`（已完成）时允许提交评价；同一订单内同一菜品只能评价一次。

**成功响应**: `{ "success": true, "message": "评价成功", "data": null }`

---

### 10.4 商家回复评价

```
PUT /api/evaluations/{id}/reply?content=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | `string` | 是 | 回复内容 |

> 此接口需商家角色。

**成功响应**: `{ "success": true, "message": "回复成功", "data": null }`

---

### 10.5 查询订单套餐（评价弹窗）

```
GET /api/customer/orders/{orderId}/packages
Authorization: Bearer <token>
```

返回某个订单下可评价的套餐快照。顾客只能查看自己的订单，商家角色可查看所有订单。

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": [
    {
      "packageId": 1,
      "dishesId": 1,
      "dishName": "招牌毛肚",
      "packageName": "大份",
      "specs": "麻辣",
      "coverUrl": "/uploads/maodu.jpg",
      "price": 68.00,
      "dishStatus": 1,
      "stock": 200
    }
  ]
}
```

---

### 10.6 批量提交评价

```
POST /api/customer/comments/batch
Authorization: Bearer <token>
Content-Type: application/json
```

用于顾客对同一订单中的多个菜品一次性提交评价。

**请求体**:

```json
[
  {
    "dishesId": 1,
    "ordersId": 1,
    "ratingValue": 5,
    "content": "非常好吃！",
    "imageUrls": ["/uploads/eval_img1.jpg"]
  }
]
```

**成功响应**: `{ "success": true, "message": "评价成功", "data": null }`

---

### 10.7 顾客端兼容评价接口

```
POST /api/customer/comment
Authorization: Bearer <token>
Content-Type: application/json
```

请求体同 [10.3 提交评价](#103-提交评价)，用于旧版前端或商家端调用兼容。

---

## 11. 消息模块

> 以下接口需要登录，请求头需携带 JWT token。

### 11.1 消息列表

```
GET /api/messages
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
      "userId": 1,
      "content": "您的订单已完成",
      "type": 2,
      "readStatus": 0,
      "createTime": "2026-05-18T10:00:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | `number` | `1` = 系统通知，`2` = 订单通知 |
| `readStatus` | `number` | `0` = 未读，`1` = 已读 |

---

### 11.2 未读消息数

```
GET /api/messages/unread-count
Authorization: Bearer <token>
```

**成功响应**: `{ "success": true, "message": "success", "data": 3 }`

---

### 11.3 标记已读

```
PUT /api/messages/{id}/read
Authorization: Bearer <token>
```

**成功响应**: `{ "success": true, "message": "ok", "data": null }`

---

### 11.4 全部已读

```
PUT /api/messages/read-all
Authorization: Bearer <token>
```

**成功响应**: `{ "success": true, "message": "ok", "data": null }`

---

### 11.5 删除消息

```
DELETE /api/messages/{id}
Authorization: Bearer <token>
```

**成功响应**: `{ "success": true, "message": "已删除", "data": null }`

---

## 12. 取消订单与退款模块

> 顾客提交取消订单/退款申请需登录，商家审核需商家角色；通过后自动退回顾客钱包，驳回时必须填写理由。

### 12.1 提交取消订单或退款申请

```
POST /api/refunds
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**:

```json
{
  "ordersId": 1,
  "requestType": "REFUND",
  "refundCause": "菜品份量太少"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `ordersId` | `number` | 是 | 订单 ID |
| `requestType` | `string` | 否 | `CANCEL` = 取消订单，`REFUND` = 申请退款；默认 `REFUND` |
| `refundCause` | `string` | 是 | 取消/退款原因 |

> 同一订单只能提交一次售后申请。`CANCEL` 仅支持已支付/制作中的订单；`REFUND` 仅支持已完成订单。

**成功响应**: `{ "success": true, "message": "申请已提交，等待商家审核", "data": null }`

---

### 12.2 查询退款状态

```
GET /api/refunds/order/{ordersId}
```

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": {
    "id": 1,
    "ordersId": 1,
    "status": 2,
    "refundCause": "菜品份量太少",
    "rejectRefundCause": null,
    "createTime": "2026-05-18T10:00:00",
    "refunTime": null
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | `number` | `1` = 无退款，`2` = 审核中，`3` = 已退款，`4` = 已驳回 |

---

### 12.3 退款列表（商家端）

```
GET /api/refunds
Authorization: Bearer <token>
```

> 需要商家角色。返回所有退款申请，按时间倒序。

---

### 12.4 通过退款

```
PUT /api/refunds/{id}/approve
Authorization: Bearer <token>
```

> 需要商家角色。

**成功响应**: `{ "success": true, "message": "退款已通过", "data": null }`

---

### 12.5 拒绝退款

```
PUT /api/refunds/{id}/reject?cause=
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `cause` | `string` | 是 | 拒绝原因 |

> 需要商家角色。

**成功响应**: `{ "success": true, "message": "已拒绝退款", "data": null }`

---

## 13. 钱包模块

> 以下接口需要登录，请求头需携带 JWT token。

### 13.1 查询钱包

```
GET /api/wallet
Authorization: Bearer <token>
```

> 若当前用户尚未开通钱包，自动创建一个余额为 0 的私人钱包。

**成功响应** (200):

```json
{
  "success": true,
  "message": "success",
  "data": {
    "id": 1,
    "userId": 1,
    "createTime": "2026-05-18T10:00:00",
    "status": 1,
    "type": 1,
    "surplus": 0.00
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | `number` | `0` = 冻结，`1` = 正常 |
| `type` | `number` | `1` = 私人钱包，`2` = 商家钱包 |
| `surplus` | `number` | 余额 |

---

### 13.2 交易明细

```
GET /api/wallet/info
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
      "detail": "订单支付 - NN20260518100000123",
      "surplusMoney": -68.00,
      "createTime": "2026-05-18T10:30:00"
    }
  ]
}
```

---

### 13.3 钱包充值

```
POST /api/wallet/recharge
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**:

```json
{
  "amount": 100.00
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `amount` | `number` | 是 | 充值金额 |

**成功响应** (200):

```json
{
  "success": true,
  "message": "充值成功",
  "data": {
    "id": 1,
    "userId": 1,
    "status": 1,
    "type": 1,
    "surplus": 100.00
  }
}
```

---

## 14. 文件上传模块

> 上传接口需要登录，但不限定顾客或商家角色；上传后的图片通过 `/uploads/**` 公开访问。

### 14.1 上传图片

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

> **注意**: 文件默认存储在 `backend/uploads/` 目录（可通过 `nomnom.upload-path` 配置或 `NOMNOM_UPLOAD_PATH` 环境变量自定义），通过 `/uploads/**` 映射对外访问。

---

## 15. 附录

### 15.1 角色说明

| role 值 | roleCode | roleLabel | 说明 |
|---------|----------|-----------|------|
| `1` | `CUSTOMER` | 顾客端 | 浏览菜单、购物车、下单、收藏、评价、消息、退款、钱包 |
| `2` | `MERCHANT` | 商家端 | 菜品管理、桌台管理、评价回复、退款审核、上传图片 |

### 15.2 HTTP 方法约定

| 方法 | 用途 |
|------|------|
| `GET` | 查询资源 |
| `POST` | 创建资源、提交操作 |
| `PUT` | 更新资源 |
| `DELETE` | 删除资源或清理数据 |

### 15.3 接口总览

| 模块 | 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|------|
| 系统 | `GET` | `/api/health` | 健康检查 | 公开 |
| 认证 | `POST` | `/api/auth/login` | 用户登录 | 公开 |
| 认证 | `POST` | `/api/auth/register` | 用户注册 | 公开 |
| 菜单 | `GET` | `/api/menu/categories` | 获取菜品分类 | 公开 |
| 菜单 | `GET` | `/api/menu/items` | 查询菜品列表 | 公开 |
| 菜单 | `GET` | `/api/menu/recommend/{packageId}` | 查询推荐菜品 | 公开 |
| 桌台 | `GET` | `/api/tables/available` | 查询空闲桌台 | 需登录 |
| 桌台 | `PUT` | `/api/tables/{id}/seat` | 选择桌台 | 需登录 |
| 桌台 | `PUT` | `/api/tables/{id}/release` | 释放桌台 | 需登录 |
| 桌台 | `GET` | `/api/customer/tables` | 查询全部桌台 | 公开 |
| 订单 | `GET` | `/api/orders/current` | 查询当前用户订单 | 需登录 |
| 订单 | `POST` | `/api/orders/preview` | 订单预览 | 需登录 |
| 订单 | `POST` | `/api/orders` | 创建订单 | 需登录 |
| 购物车 | `GET` | `/api/cart` | 查看购物车 | 需登录 |
| 购物车 | `POST` | `/api/cart` | 加入购物车 | 需登录 |
| 购物车 | `PUT` | `/api/cart/{id}/quantity` | 修改购物车数量 | 需登录 |
| 购物车 | `PUT` | `/api/cart/{id}/selected` | 切换选中状态 | 需登录 |
| 购物车 | `DELETE` | `/api/cart/{id}` | 移除购物车项 | 需登录 |
| 购物车 | `DELETE` | `/api/cart` | 清空购物车 | 需登录 |
| 收藏 | `GET` | `/api/collection` | 收藏列表 | 需登录 |
| 收藏 | `GET` | `/api/collection/check` | 检查是否已收藏 | 需登录 |
| 收藏 | `POST` | `/api/collection/toggle` | 切换收藏 | 需登录 |
| 评价 | `GET` | `/api/evaluations/dish/{dishesId}` | 查看菜品评价 | 公开 |
| 评价 | `GET` | `/api/evaluations/my` | 查看我的评价 | 需登录 |
| 评价 | `POST` | `/api/evaluations` | 提交评价 | 需登录 |
| 评价 | `PUT` | `/api/evaluations/{id}/reply` | 商家回复评价 | 需商家 |
| 评价 | `GET` | `/api/customer/orders/{orderId}/packages` | 查询订单套餐 | 需登录 |
| 评价 | `POST` | `/api/customer/comments/batch` | 批量提交评价 | 需登录 |
| 评价 | `POST` | `/api/customer/comment` | 单个评价兼容接口 | 需登录 |
| 消息 | `GET` | `/api/messages` | 消息列表 | 需登录 |
| 消息 | `GET` | `/api/messages/unread-count` | 未读消息数 | 需登录 |
| 消息 | `PUT` | `/api/messages/{id}/read` | 标记已读 | 需登录 |
| 消息 | `PUT` | `/api/messages/read-all` | 全部已读 | 需登录 |
| 消息 | `DELETE` | `/api/messages/{id}` | 删除消息 | 需登录 |
| 退款 | `POST` | `/api/refunds` | 提交取消/退款申请 | 需登录 |
| 退款 | `GET` | `/api/refunds/order/{ordersId}` | 查询退款状态 | 需登录 |
| 退款 | `GET` | `/api/refunds` | 退款列表 | 需商家 |
| 退款 | `PUT` | `/api/refunds/{id}/approve` | 通过退款 | 需商家 |
| 退款 | `PUT` | `/api/refunds/{id}/reject` | 拒绝退款 | 需商家 |
| 钱包 | `GET` | `/api/wallet` | 查询钱包 | 需登录 |
| 钱包 | `GET` | `/api/wallet/info` | 交易明细 | 需登录 |
| 钱包 | `POST` | `/api/wallet/recharge` | 钱包充值 | 需登录 |
| 文件 | `POST` | `/api/common/upload` | 上传图片 | 需登录 |
| 文件 | `GET` | `/uploads/**` | 访问上传资源 | 公开 |
| 商家-菜品 | `GET` | `/api/admin/dishes` | 菜品列表 | 需商家 |
| 商家-菜品 | `GET` | `/api/admin/dishes/{id}` | 菜品详情 | 需商家 |
| 商家-菜品 | `POST` | `/api/admin/dishes/add` | 新增菜品 | 需商家 |
| 商家-菜品 | `PUT` | `/api/admin/dishes/{id}` | 编辑菜品 | 需商家 |
| 商家-菜品 | `PUT` | `/api/admin/dishes/{id}/stock` | 快速修改库存 | 需商家 |
| 商家-菜品 | `PUT` | `/api/admin/dishes/{id}/status` | 上架/下架 | 需商家 |
| 商家-菜品 | `DELETE` | `/api/admin/dishes/{id}` | 删除菜品 | 需商家 |
| 商家-分类 | `GET` | `/api/admin/categories` | 分类列表 | 需商家 |
| 商家-分类 | `POST` | `/api/admin/categories` | 新增分类 | 需商家 |
| 商家-分类 | `PUT` | `/api/admin/categories/{id}` | 编辑分类 | 需商家 |
| 商家-分类 | `DELETE` | `/api/admin/categories/{id}` | 删除分类 | 需商家 |
| 商家-订单 | `GET` | `/api/admin/dashboard` | 仪表盘 | 需商家 |
| 商家-订单 | `GET` | `/api/admin/orders` | 订单列表 | 需商家 |
| 商家-订单 | `PUT` | `/api/admin/orders/{id}/status` | 更新订单状态 | 需商家 |
| 商家-订单 | `DELETE` | `/api/admin/orders/invalid` | 清理异常订单 | 需商家 |
| 商家-桌台 | `GET` | `/api/admin/tables` | 查询桌台 | 需商家 |
| 商家-桌台 | `GET` | `/api/admin/customer/tables` | 查询顾客可选桌台 | 需商家 |
| 商家-桌台 | `POST` | `/api/admin/tables` | 新增桌台 | 需商家 |
| 商家-桌台 | `PUT` | `/api/admin/tables/{id}` | 编辑桌台 | 需商家 |
| 商家-桌台 | `DELETE` | `/api/admin/tables/{id}` | 删除桌台 | 需商家 |
| 商家-桌台 | `PUT` | `/api/admin/tables/status` | 更新桌台状态 | 需商家 |
| 商家-审核 | `GET` | `/api/admin/audits` | 审核列表 | 需商家 |
| 商家-审核 | `PUT` | `/api/admin/audit/{id}` | 审核操作 | 需商家 |
| 商家-财务 | `GET` | `/api/admin/finance/reconciliation` | 财务对账 | 需商家 |
| 商家-评价 | `GET` | `/api/admin/reviews` | 评价列表 | 需商家 |
| 商家-评价 | `POST` | `/api/admin/reviews/{id}/reply` | 回复评价 | 需商家 |
| 商家-评价 | `DELETE` | `/api/admin/reviews/{id}` | 删除评价 | 需商家 |
| 商家-统计 | `GET` | `/api/admin/stats` | 统计分析 | 需商家 |
