<script setup lang="ts">
import { ref } from 'vue'
import http from '@/api/http'

interface HealthComponent {
  status: string
}

interface HealthResponse {
  status: string
  components?: Record<string, HealthComponent>
}

const backendStatus = ref('未检测')
const details = ref('')
const checking = ref(false)

async function checkBackend() {
  checking.value = true

  try {
    const response = await http.get<HealthResponse>('/actuator/health')
    const data = response.data

    backendStatus.value = data.status
    details.value = Object.entries(data.components ?? {})
      .map(([name, component]) => `${name}: ${component.status}`)
      .join('，')
  } catch (error) {
    backendStatus.value = '连接失败'
    details.value = '请确认 Spring Boot 已启动'
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

    <el-button type="primary" :loading="checking" @click="checkBackend">
      检查后端连接
    </el-button>

    <p>
      后端状态：
      <el-tag :type="backendStatus === 'UP' ? 'success' : 'danger'">
        {{ backendStatus }}
      </el-tag>
    </p>

    <p v-if="details">{{ details }}</p>
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