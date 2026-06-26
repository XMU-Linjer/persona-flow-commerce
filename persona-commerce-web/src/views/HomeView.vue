<script setup lang="ts">
import { ref } from 'vue'
import http from '@/api/http'

interface PingData {
  message: string
  service: string
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const backendStatus = ref('未检测')
const backendMessage = ref('')
const checking = ref(false)

async function checkBackend() {
  checking.value = true

  try {
    const response = await http.get<ApiResponse<PingData>>('/api/system/ping')

    backendStatus.value = response.data.code === 0 ? '连接成功' : '接口异常'
    backendMessage.value =
      `${response.data.data.message} · ${response.data.data.service}`
  } catch (error) {
    backendStatus.value = '连接失败'
    backendMessage.value = '请确认 Spring Boot 已启动'
    console.error(error)
  } finally {
    checking.value = false
  }
}
</script>

<template>
  <main>
    <h1>PersonaFlow Commerce</h1>
    <p>Vue 3 frontend is running.</p>

    <el-button
      type="primary"
      :loading="checking"
      @click="checkBackend"
    >
      检查后端连接
    </el-button>

    <p>
      后端状态：
      <el-tag :type="backendStatus === '连接成功' ? 'success' : 'danger'">
        {{ backendStatus }}
      </el-tag>
    </p>

    <p v-if="backendMessage">
      {{ backendMessage }}
    </p>
  </main>
</template>

<style scoped>
main {
  padding: 40px;
}

p {
  margin-top: 20px;
}
</style>
