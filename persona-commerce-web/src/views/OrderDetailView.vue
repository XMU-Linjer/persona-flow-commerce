<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CreditCard, Refresh } from '@element-plus/icons-vue'
import { cancelOrder, getOrderDetail, payOrder, type OrderDetail } from '@/api/trade'
import {
  formatMoney,
  formatTime,
  ORDER_STATUS_PENDING,
  orderStatusLabel,
  orderStatusTagType,
} from '@/utils/order'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const operating = ref(false)
const order = ref<OrderDetail | null>(null)

const orderId = computed(() => Number(route.params.orderId))

function fullAddress() {
  const address = order.value?.address
  if (!address) return '-'
  return `${address.province}${address.city}${address.district}${address.detailAddress}`
}

async function loadOrder() {
  if (!Number.isFinite(orderId.value)) return

  loading.value = true
  try {
    order.value = await getOrderDetail(orderId.value)
  } finally {
    loading.value = false
  }
}

async function cancelCurrentOrder() {
  if (!order.value) return

  await ElMessageBox.confirm(`确定取消订单 ${order.value.orderNo} 吗？`, '取消订单', {
    type: 'warning',
    confirmButtonText: '取消订单',
    cancelButtonText: '再想想',
  })
  operating.value = true
  try {
    await cancelOrder(order.value.orderId)
    ElMessage.success('订单已取消')
    await loadOrder()
  } finally {
    operating.value = false
  }
}

async function payCurrentOrder() {
  if (!order.value) return

  await ElMessageBox.confirm(`确认使用 MOCK 渠道模拟支付订单 ${order.value.orderNo} 吗？`, '模拟支付', {
    type: 'warning',
    confirmButtonText: '模拟支付',
    cancelButtonText: '取消',
  })
  operating.value = true
  try {
    await payOrder(order.value.orderId)
    ElMessage.success('支付成功')
    await loadOrder()
  } finally {
    operating.value = false
  }
}

onMounted(loadOrder)
</script>

<template>
  <section>
    <el-button :icon="ArrowLeft" text @click="router.push('/orders')">返回订单列表</el-button>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item class="detail-skeleton" variant="rect" />
      </template>

      <el-empty v-if="!order" description="订单不存在" />

      <div v-else class="order-detail">
        <div class="page-title">
          <div>
            <h1>订单详情</h1>
            <p>{{ order.orderNo }}</p>
          </div>
          <div class="title-actions">
            <el-tag size="large" :type="orderStatusTagType(order.status)">
              {{ orderStatusLabel(order.status) }}
            </el-tag>
            <el-button :icon="Refresh" plain @click="loadOrder">刷新</el-button>
          </div>
        </div>

        <section class="content-panel block">
          <div class="block-title">
            <h2>订单信息</h2>
            <div v-if="order.status === ORDER_STATUS_PENDING" class="action-row">
              <el-button :loading="operating" type="warning" plain @click="cancelCurrentOrder">
                取消订单
              </el-button>
              <el-button :icon="CreditCard" :loading="operating" type="success" @click="payCurrentOrder">
                模拟支付
              </el-button>
            </div>
          </div>

          <el-descriptions :column="3" border>
            <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="总金额">{{ formatMoney(order.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ orderStatusLabel(order.status) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(order.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ formatTime(order.paidAt) }}</el-descriptions-item>
            <el-descriptions-item label="取消时间">{{ formatTime(order.canceledAt) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="content-panel block">
          <div class="block-title">
            <h2>收货地址快照</h2>
          </div>
          <p class="address-line">
            {{ order.address.recipientName }}，{{ order.address.recipientPhone }}，{{ fullAddress() }}
          </p>
          <p class="muted">邮编：{{ order.address.postalCode || '未填写' }}</p>
        </section>

        <section class="content-panel block">
          <div class="block-title">
            <h2>商品快照</h2>
            <strong class="price">{{ formatMoney(order.totalAmount) }}</strong>
          </div>

          <div class="order-items">
            <article v-for="item in order.items" :key="item.skuId" class="order-item">
              <el-image class="item-thumb" fit="cover" :src="item.imageUrl">
                <template #error>
                  <div class="thumb-fallback">{{ item.categoryName }}</div>
                </template>
              </el-image>
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

        <section class="content-panel block">
          <div class="block-title">
            <h2>支付记录</h2>
          </div>

          <el-empty v-if="!order.payment" description="暂未支付" />
          <el-descriptions v-else :column="3" border>
            <el-descriptions-item label="支付单号">{{ order.payment.paymentNo }}</el-descriptions-item>
            <el-descriptions-item label="支付渠道">{{ order.payment.channel }}</el-descriptions-item>
            <el-descriptions-item label="支付金额">{{ formatMoney(order.payment.amount) }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ formatTime(order.payment.paidAt) }}</el-descriptions-item>
          </el-descriptions>
        </section>
      </div>
    </el-skeleton>
  </section>
</template>

<style scoped>
.order-detail {
  display: grid;
  gap: 16px;
}

.title-actions,
.block-title,
.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.block-title {
  justify-content: space-between;
  margin-bottom: 14px;
}

.block-title h2 {
  margin: 0;
  color: #1f2a37;
  font-size: 18px;
  font-weight: 700;
}

.block {
  padding: 18px;
}

.address-line {
  margin: 0 0 6px;
  color: #1f2a37;
  font-weight: 600;
}

.order-items {
  display: grid;
  gap: 12px;
}

.order-item {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) 110px 70px 120px;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #edf1f5;
}

.order-item:last-child {
  border-bottom: none;
}

.item-thumb,
.thumb-fallback {
  width: 72px;
  height: 72px;
  border-radius: 6px;
}

.thumb-fallback {
  display: grid;
  place-items: center;
  background: #edf3f0;
  color: #53606f;
  font-size: 12px;
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

.detail-skeleton {
  height: 520px;
  border-radius: 8px;
}

@media (max-width: 820px) {
  .order-item {
    grid-template-columns: 72px minmax(0, 1fr);
  }
}
</style>
