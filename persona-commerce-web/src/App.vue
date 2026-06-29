<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Goods,
  Location,
  ShoppingCart,
  Star,
  Tickets,
  User,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/products')) return '/products'
  return route.path
})

const menuItems = [
  { path: '/products', label: '首页 / 商品', icon: Goods },
  { path: '/favorites', label: '收藏', icon: Star },
  { path: '/cart', label: '购物车', icon: ShoppingCart },
  { path: '/addresses', label: '地址', icon: Location },
  { path: '/orders', label: '订单', icon: Tickets },
]

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <router-link class="brand" to="/products">
        <span class="brand-mark">PF</span>
        <span>
          <strong>PersonaFlow Commerce</strong>
          <small>V1.0 Demo</small>
        </span>
      </router-link>

      <div class="header-actions">
        <template v-if="auth.isLoggedIn">
          <el-icon><User /></el-icon>
          <span class="user-name">{{ auth.displayName }}</span>
          <el-button plain size="small" @click="logout">退出登录</el-button>
        </template>
        <template v-else>
          <el-button text @click="router.push('/login')">登录</el-button>
          <el-button type="primary" @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>

    <el-container class="body-shell">
      <el-aside class="sidebar" width="216px">
        <el-menu :default-active="activeMenu" router>
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
