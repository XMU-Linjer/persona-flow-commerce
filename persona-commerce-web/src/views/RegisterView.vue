<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  displayName: '',
  password: '',
})

function validatePassword(_: unknown, value: string, callback: (error?: Error) => void) {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,64}$/.test(value)) {
    callback(new Error('密码需为 8 到 64 个字符，且至少包含字母和数字'))
    return
  }
  callback()
}

const rules: FormRules<typeof form> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^\w{4,32}$/, message: '用户名只允许 4 到 32 位字母、数字或下划线', trigger: 'blur' },
  ],
  displayName: [
    { required: true, message: '请输入展示名', trigger: 'blur' },
    { min: 1, max: 50, message: '展示名长度为 1 到 50 个字符', trigger: 'blur' },
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
}

async function submit() {
  if (!formRef.value) return

  await formRef.value.validate()
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-card">
      <div class="auth-intro">
        <h1>创建账号</h1>
        <p>注册后即可体验 V1.0 的商品、收藏、购物车和订单主链路。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="4 到 32 位字母、数字或下划线" />
        </el-form-item>

        <el-form-item label="展示名" prop="displayName">
          <el-input v-model.trim="form.displayName" placeholder="请输入展示名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" placeholder="至少包含字母和数字" show-password type="password" />
        </el-form-item>

        <el-button class="auth-submit" type="primary" :loading="loading" @click="submit">
          注册
        </el-button>
      </el-form>

      <p class="auth-switch">
        已有账号？
        <router-link to="/login">去登录</router-link>
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
  width: min(460px, 100%);
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
