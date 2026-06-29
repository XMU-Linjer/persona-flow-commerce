# PersonaFlow Commerce Web

V1.0 前端演示项目，基于 Vue 3、Vite、Vue Router、Axios 和 Element Plus。

## 当前范围

已实现：

- 基础布局、导航和登录状态展示
- 登录、注册
- 商品列表、关键词搜索和分页
- 商品详情、SKU 展示
- 收藏、购物车、地址、订单占位路由
- 需要登录页面的路由守卫

本阶段暂不实现：

- 收藏真实功能
- 购物车真实功能
- 地址管理
- 创建订单、订单列表、取消订单、模拟支付
- admin、behavior、RabbitMQ、Agent

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

构建：

```sh
npm run build
```
