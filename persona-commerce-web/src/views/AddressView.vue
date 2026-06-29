<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Star } from '@element-plus/icons-vue'
import {
  createAddress,
  deleteAddress,
  listAddresses,
  setDefaultAddress,
  updateAddress,
  type AddressItem,
  type AddressPayload,
} from '@/api/address'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingAddressId = ref<number>()
const formRef = ref<FormInstance>()
const addresses = ref<AddressItem[]>([])

const emptyForm: AddressPayload = {
  recipientName: '',
  recipientPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  postalCode: '',
  isDefault: false,
}

const form = reactive<AddressPayload>({ ...emptyForm })

const rules: FormRules<AddressPayload> = {
  recipientName: [
    { required: true, message: '请输入收货人', trigger: 'blur' },
    { max: 50, message: '最多 50 个字符', trigger: 'blur' },
  ],
  recipientPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { max: 30, message: '最多 30 个字符', trigger: 'blur' },
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detailAddress: [
    { required: true, message: '请输入详细地址', trigger: 'blur' },
    { max: 255, message: '最多 255 个字符', trigger: 'blur' },
  ],
  postalCode: [{ max: 20, message: '最多 20 个字符', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(form, emptyForm)
  editingAddressId.value = undefined
  formRef.value?.clearValidate()
}

async function loadAddresses() {
  loading.value = true
  try {
    addresses.value = await listAddresses()
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(address: AddressItem) {
  Object.assign(form, {
    recipientName: address.recipientName,
    recipientPhone: address.recipientPhone,
    province: address.province,
    city: address.city,
    district: address.district,
    detailAddress: address.detailAddress,
    postalCode: address.postalCode || '',
    isDefault: address.isDefault,
  })
  editingAddressId.value = address.addressId
  dialogVisible.value = true
}

async function saveAddress() {
  if (!formRef.value) return

  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (editingAddressId.value) {
      await updateAddress(editingAddressId.value, payload)
      ElMessage.success('地址已更新')
    } else {
      await createAddress(payload)
      ElMessage.success('地址已新增')
    }
    dialogVisible.value = false
    await loadAddresses()
  } finally {
    saving.value = false
  }
}

async function removeAddress(address: AddressItem) {
  await ElMessageBox.confirm('确定删除这个收货地址吗？', '删除地址', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteAddress(address.addressId)
  ElMessage.success('地址已删除')
  await loadAddresses()
}

async function markDefault(address: AddressItem) {
  await setDefaultAddress(address.addressId)
  ElMessage.success('已设为默认地址')
  await loadAddresses()
}

function fullAddress(address: AddressItem) {
  return `${address.province}${address.city}${address.district}${address.detailAddress}`
}

onMounted(loadAddresses)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>地址管理</h1>
        <p>这里维护下单可用的收货地址，联系电话仅作为收货联系信息。</p>
      </div>
      <el-button :icon="Plus" type="primary" @click="openCreateDialog">新增地址</el-button>
    </div>

    <div class="address-list">
      <el-skeleton :loading="loading" animated :count="3">
        <template #template>
          <el-skeleton-item class="address-skeleton" variant="rect" />
        </template>

        <el-empty v-if="addresses.length === 0" description="还没有收货地址">
          <el-button :icon="Plus" type="primary" @click="openCreateDialog">新增地址</el-button>
        </el-empty>

        <article v-for="address in addresses" :key="address.addressId" class="address-card content-panel">
          <div class="address-main">
            <div class="address-title">
              <strong>{{ address.recipientName }}</strong>
              <span>{{ address.recipientPhone }}</span>
              <el-tag v-if="address.isDefault" type="success">默认</el-tag>
            </div>
            <p>{{ fullAddress(address) }}</p>
            <span class="muted">邮编：{{ address.postalCode || '未填写' }}</span>
          </div>

          <div class="address-actions">
            <el-button
              :icon="Star"
              plain
              :disabled="address.isDefault"
              @click="markDefault(address)"
            >
              设为默认
            </el-button>
            <el-button :icon="Edit" plain @click="openEditDialog(address)">编辑</el-button>
            <el-button :icon="Delete" plain type="danger" @click="removeAddress(address)">删除</el-button>
          </div>
        </article>
      </el-skeleton>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingAddressId ? '编辑地址' : '新增地址'"
      width="560px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="收货人" prop="recipientName">
            <el-input v-model.trim="form.recipientName" placeholder="请输入收货人" />
          </el-form-item>
          <el-form-item label="联系电话" prop="recipientPhone">
            <el-input v-model.trim="form.recipientPhone" placeholder="请输入收货联系电话" />
          </el-form-item>
          <el-form-item label="省份" prop="province">
            <el-input v-model.trim="form.province" placeholder="例如：广东省" />
          </el-form-item>
          <el-form-item label="城市" prop="city">
            <el-input v-model.trim="form.city" placeholder="例如：深圳市" />
          </el-form-item>
          <el-form-item label="区县" prop="district">
            <el-input v-model.trim="form.district" placeholder="例如：南山区" />
          </el-form-item>
          <el-form-item label="邮编" prop="postalCode">
            <el-input v-model.trim="form.postalCode" placeholder="可选" />
          </el-form-item>
        </div>

        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model.trim="form.detailAddress"
            placeholder="街道、门牌号等"
            type="textarea"
            :rows="3"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="form.isDefault">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.address-list {
  display: grid;
  gap: 14px;
}

.address-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
}

.address-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.address-title strong {
  color: #1f2a37;
  font-size: 18px;
  font-weight: 700;
}

.address-title span {
  color: #53606f;
}

.address-main p {
  margin: 10px 0 6px;
  color: #1f2a37;
}

.address-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.address-skeleton {
  height: 112px;
  border-radius: 8px;
}

@media (max-width: 760px) {
  .address-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .address-actions {
    justify-content: flex-start;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
