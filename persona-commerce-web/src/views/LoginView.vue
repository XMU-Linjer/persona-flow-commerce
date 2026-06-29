<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  identifier: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  identifier: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '用户名长度为 4 到 32 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8 到 64 个字符', trigger: 'blur' },
  ],
}

async function submit() {
  if (!formRef.value) return

  await formRef.value.validate()
  loading.value = true
  try {
    await auth.login({
      identityType: 'USERNAME',
      identifier: form.identifier,
      password: form.password,
    })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/products'
    router.push(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-card">
      <div class="auth-intro">
        <h1>欢迎回来</h1>
        <p>登录后可以继续使用收藏、购物车、地址和订单能力。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="identifier">
          <el-input v-model.trim="form.identifier" :prefix-icon="User" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            placeholder="请输入密码"
            show-password
            type="password"
          />
        </el-form-item>

        <el-button class="auth-submit" type="primary" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>

      <p class="auth-switch">
        还没有账号？
        <router-link to="/register">去注册</router-link>
      </p>
    </div>
  </section>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: calc(100vh - 160px);
  place-items: center;
}

.auth-card {
  width: min(420px, 100%);
  padding: 28px;
  border: 1px solid #e3e9f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 45px rgba(31, 42, 55, 0.08);
}

.auth-intro {
  margin-bottom: 24px;
}

.auth-intro h1 {
  margin: 0 0 8px;
  color: #1f2a37;
  font-size: 26px;
  font-weight: 700;
}

.auth-intro p,
.auth-switch {
  margin: 0;
  color: #6b7280;
}

.auth-submit {
  width: 100%;
  margin-top: 8px;
}

.auth-switch {
  margin-top: 18px;
  text-align: center;
}

.auth-switch a {
  color: #1f8f75;
  font-weight: 600;
}
</style>
