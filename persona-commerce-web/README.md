# PersonaFlow Commerce Web

V1.0 前端演示项目，基于 Vue 3、Vite、Vue Router、Axios 和 Element Plus。

## 当前范围

已实现：

- 基础布局、导航和登录状态展示
- 登录、注册
- 商品列表、关键词搜索和分页
- 商品详情、SKU 选择、收藏 SKU、加入购物车
- 收藏列表、取消收藏、跳转商品详情
- 购物车列表、修改数量、删除单项、清空购物车、总金额展示
- 地址列表、新增、编辑、删除、设为默认地址
- 需要登录页面的路由守卫

本阶段暂不实现：

- 创建订单
- 订单列表、订单详情
- 取消订单、模拟支付
- admin、behavior、RabbitMQ、Agent、RAG

## 启动方式

后端默认地址为 `http://localhost:8080`。如需修改，可设置环境变量：

```sh
VITE_API_BASE_URL=http://localhost:8080
```

安装依赖：

```sh
npm install
```

启动开发服务：

```sh
npm run dev
```

如果 PowerShell 拦截 `npm.ps1`，可以使用：

```sh
npm.cmd run dev -- --host 127.0.0.1
```

构建：

```sh
npm.cmd run build
```
