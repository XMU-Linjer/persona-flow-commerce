import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/http'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/products',
    },
    {
      path: '/products',
      name: 'products',
      component: () => import('@/views/ProductListView.vue'),
      meta: { title: '商品' },
    },
    {
      path: '/products/:spuId',
      name: 'product-detail',
      component: () => import('@/views/ProductDetailView.vue'),
      meta: { title: '商品详情' },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录', guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { title: '注册', guestOnly: true },
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('@/views/FavoriteView.vue'),
      meta: { title: '收藏', requiresAuth: true },
    },
    {
      path: '/cart',
      name: 'cart',
      component: () => import('@/views/CartView.vue'),
      meta: { title: '购物车', requiresAuth: true },
    },
    {
      path: '/addresses',
      name: 'addresses',
      component: () => import('@/views/AddressView.vue'),
      meta: { title: '地址', requiresAuth: true },
    },
    {
      path: '/checkout',
      name: 'checkout',
      component: () => import('@/views/CheckoutView.vue'),
      meta: { title: '确认订单', requiresAuth: true },
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('@/views/OrderListView.vue'),
      meta: { title: '订单', requiresAuth: true },
    },
    {
      path: '/orders/:orderId',
      name: 'order-detail',
      component: () => import('@/views/OrderDetailView.vue'),
      meta: { title: '订单详情', requiresAuth: true },
    },
    {
      path: '/ai-insights',
      name: 'ai-insights',
      component: () => import('@/views/ai/InsightView.vue'),
      meta: { title: 'AI 购物洞察', requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  const token = getToken()
  if (to.meta.requiresAuth && !token) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  if (to.meta.guestOnly && token) {
    return { name: 'products' }
  }

  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '首页')} - PersonaFlow Commerce`
})

export default router
