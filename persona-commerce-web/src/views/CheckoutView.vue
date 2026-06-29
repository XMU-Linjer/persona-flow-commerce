<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Location, Tickets } from '@element-plus/icons-vue'
import { listAddresses, type AddressItem } from '@/api/address'
import { createOrder } from '@/api/trade'
import { clearCheckoutItems, loadCheckoutItems, type CheckoutItem } from '@/utils/checkout'
import { formatMoney } from '@/utils/order'
import ProductImage from '@/components/ProductImage.vue'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const addresses = ref<AddressItem[]>([])
const selectedAddressId = ref<number>()
const checkoutItems = ref<CheckoutItem[]>([])

const totalAmount = computed(() => {
  return checkoutItems.value.reduce((sum, item) => sum + Number(item.subtotal || 0), 0)
})

const selectedAddress = computed(() => {
  return addresses.value.find((item) => item.addressId === selectedAddressId.value)
})

function fullAddress(address: AddressItem) {
  return `${address.province}${address.city}${address.district}${address.detailAddress}`
}

async function loadPage() {
  loading.value = true
  try {
    checkoutItems.value = loadCheckoutItems()
    addresses.value = await listAddresses()
    selectedAddressId.value =
      addresses.value.find((address) => address.isDefault)?.addressId || addresses.value[0]?.addressId
  } finally {
    loading.value = false
  }
}

async function submitOrder() {
  if (checkoutItems.value.length === 0) {
    ElMessage.warning('没有可提交的结算商品')
    return
  }

  if (!selectedAddressId.value) {
    ElMessage.warning('请先选择收货地址')
    return
  }

  submitting.value = true
  try {
    const result = await createOrder({
      addressId: selectedAddressId.value,
      items: checkoutItems.value.map((item) => ({
        skuId: item.skuId,
        quantity: item.quantity,
      })),
    })
    clearCheckoutItems()
    ElMessage.success('订单创建成功')
    router.push(`/orders/${result.orderId}`)
  } finally {
    submitting.value = false
  }
}

onMounted(loadPage)
</script>

<template>
  <section>
    <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>

    <div class="page-title">
      <div>
        <h1>确认订单</h1>
        <p>确认收货地址和商品明细后提交订单，库存将在后端创建订单时锁定。</p>
      </div>
    </div>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item class="checkout-skeleton" variant="rect" />
      </template>

      <el-empty v-if="checkoutItems.length === 0" description="没有待结算商品">
        <el-button type="primary" @click="router.push('/products')">去浏览商品</el-button>
      </el-empty>

      <div v-else class="checkout-layout">
        <div class="checkout-main">
          <section class="content-panel block">
            <div class="block-title">
              <h2>收货地址</h2>
              <el-button :icon="Location" text type="primary" @click="router.push('/addresses')">
                管理地址
              </el-button>
            </div>

            <el-empty v-if="addresses.length === 0" description="还没有收货地址">
              <el-button type="primary" @click="router.push('/addresses')">去新增地址</el-button>
            </el-empty>

            <el-radio-group v-else v-model="selectedAddressId" class="address-options">
              <el-radio v-for="address in addresses" :key="address.addressId" :label="address.addressId">
                <div class="address-option">
                  <strong>{{ address.recipientName }}</strong>
                  <span>{{ address.recipientPhone }}</span>
                  <el-tag v-if="address.isDefault" size="small" type="success">默认</el-tag>
                  <p>{{ fullAddress(address) }}</p>
                </div>
              </el-radio>
            </el-radio-group>
          </section>

          <section class="content-panel block">
            <div class="block-title">
              <h2>商品明细</h2>
              <span class="muted">{{ checkoutItems.length }} 件商品</span>
            </div>

            <div class="checkout-items">
              <article v-for="item in checkoutItems" :key="item.skuId" class="checkout-item">
                <ProductImage class="item-thumb" :src="item.imageUrl" :label="item.categoryName" />
                <div class="item-info">
                  <strong>{{ item.productName }}</strong>
                  <span>{{ item.skuName }}</span>
                  <small>{{ item.categoryName }}</small>
                </div>
                <span>{{ formatMoney(item.unitPrice) }}</span>
                <span>x {{ item.quantity }}</span>
                <strong class="price">{{ formatMoney(item.subtotal) }}</strong>
              </article>
            </div>
          </section>
        </div>

        <aside class="content-panel summary">
          <h2>订单汇总</h2>
          <div class="summary-row">
            <span>商品金额</span>
            <strong>{{ formatMoney(totalAmount) }}</strong>
          </div>
          <div class="summary-row">
            <span>收货地址</span>
            <span>{{ selectedAddress ? selectedAddress.recipientName : '未选择' }}</span>
          </div>
          <el-button
            :icon="Tickets"
            type="primary"
            size="large"
            :loading="submitting"
            :disabled="addresses.length === 0 || checkoutItems.length === 0"
            @click="submitOrder"
          >
            提交订单
          </el-button>
        </aside>
      </div>
    </el-skeleton>
  </section>
</template>

<style scoped>
.checkout-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
}

.checkout-main {
  display: grid;
  gap: 16px;
}

.block,
.summary {
  padding: 18px;
}

.block-title,
.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.block-title h2,
.summary h2 {
  margin: 0;
  color: #1f2a37;
  font-size: 18px;
  font-weight: 700;
}

.address-options {
  display: grid;
  gap: 12px;
}

.address-options :deep(.el-radio) {
  align-items: flex-start;
  height: auto;
  margin-right: 0;
  padding: 12px;
  border: 1px solid #e3e9f0;
  border-radius: 8px;
}

.address-option {
  line-height: 1.5;
}

.address-option strong,
.address-option span,
.address-option p {
  margin-right: 10px;
}

.address-option p {
  margin-top: 6px;
  margin-bottom: 0;
  color: #53606f;
}

.checkout-items {
  display: grid;
  gap: 12px;
}

.checkout-item {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) 110px 70px 120px;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #edf1f5;
}

.checkout-item:last-child {
  border-bottom: none;
}

.item-thumb {
  width: 72px;
  height: 72px;
  border-radius: 6px;
}

.item-info strong,
.item-info span,
.item-info small {
  display: block;
}

.item-info strong {
  color: #1f2a37;
  font-weight: 700;
}

.item-info span,
.item-info small {
  margin-top: 4px;
  color: #6b7280;
}

.summary {
  position: sticky;
  top: 88px;
  align-self: start;
}

.summary-row {
  margin: 18px 0;
}

.summary-row strong {
  color: #c25b19;
  font-size: 24px;
  font-weight: 800;
}

.summary .el-button {
  width: 100%;
}

.checkout-skeleton {
  height: 420px;
  border-radius: 8px;
}

@media (max-width: 960px) {
  .checkout-layout {
    grid-template-columns: 1fr;
  }

  .summary {
    position: static;
  }

  .checkout-item {
    grid-template-columns: 72px minmax(0, 1fr);
  }
}
</style>
