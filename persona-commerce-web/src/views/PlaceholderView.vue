<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const title = computed(() => String(route.meta.title || '页面'))
</script>

<template>
  <section class="placeholder content-panel">
    <h1>{{ title }}</h1>
    <p>该页面路由已接入，真实业务功能将在后续前端阶段对接。</p>
    <el-button type="primary" @click="router.push('/products')">先去浏览商品</el-button>
  </section>
</template>

<style scoped>
.placeholder {
  display: grid;
  min-height: 360px;
  place-items: center;
  align-content: center;
  gap: 12px;
  padding: 40px;
  text-align: center;
}

.placeholder h1 {
  margin: 0;
  color: #1f2a37;
  font-size: 28px;
  font-weight: 700;
}

.placeholder p {
  margin: 0 0 8px;
  color: #6b7280;
}
</style>
