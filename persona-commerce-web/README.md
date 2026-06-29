# PersonaFlow Commerce Web

PersonaFlow Commerce V1.0 前端演示项目，使用 Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios 和 Element Plus 构建。

## 当前页面

- 登录
- 注册
- 商品列表
- 商品详情
- 收藏
- 购物车
- 地址管理
- 结算页
- 订单列表
- 订单详情

## 当前能力

- 登录 / 注册
- 商品关键词搜索和分页
- 商品详情和 SKU 选择
- 收藏 SKU / 取消收藏
- 加入购物车
- 修改购物车数量
- 删除购物车项
- 清空购物车
- 新增 / 编辑 / 删除 / 设置默认收货地址
- 立即购买
- 从购物车进入结算
- 创建订单
- 查看订单列表和订单详情
- 取消待支付订单
- 模拟支付待支付订单
- 需要登录的路由守卫
- 商品图片本地不可访问时显示统一占位图

## V1.0 不包含

- 真实第三方支付
- 退款
- 物流
- 优惠券
- 商家系统
- behavior
- RabbitMQ 事件 UI
- Agent
- admin
- RAG

## API 地址

默认后端地址：

```text
http://127.0.0.1:8080
```

如需覆盖，可设置 `VITE_API_BASE_URL`：

```powershell
$env:VITE_API_BASE_URL="http://127.0.0.1:8080"
```

## 本地联调

先在仓库根目录启动后端依赖：

```powershell
docker compose up -d
```

启动后端：

```powershell
cd persona-commerce-server
.\mvnw.cmd spring-boot:run
```

Spring Boot 会自动读取项目根目录 `.env`。不需要手动设置 MySQL、Redis、RabbitMQ 相关 PowerShell 环境变量。`.env` 不提交，`.env.example` 是模板。

安装前端依赖：

```powershell
cd persona-commerce-web
npm.cmd install
```

启动前端：

```powershell
npm.cmd run dev -- --host 127.0.0.1
```

访问：

```text
http://127.0.0.1:5173/
```

PowerShell 下如果 `npm.ps1` 被拦截，请使用 `npm.cmd`。

## 构建

```powershell
npm.cmd run build
```

## 演示流程

1. 注册新用户。
2. 登录。
3. 浏览商品列表。
4. 搜索商品。
5. 进入商品详情。
6. 选择 SKU。
7. 收藏商品。
8. 加入购物车。
9. 新增或编辑收货地址。
10. 立即购买或从购物车结算。
11. 提交订单。
12. 查看订单详情。
13. 模拟支付待支付订单。
14. 再创建一笔订单并取消。
15. 在订单列表按全部、待支付、已支付、已取消筛选。
