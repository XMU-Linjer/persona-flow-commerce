<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CreditCard, Refresh, View } from '@element-plus/icons-vue'
import { cancelOrder, listOrders, payOrder, type OrderListItem } from '@/api/trade'
import {
  formatMoney,
  formatTime,
  ORDER_STATUS_CANCELED,
  ORDER_STATUS_PAID,
  ORDER_STATUS_PENDING,
  orderStatusLabel,
  orderStatusTagType,
} from '@/utils/order'

const router = useRouter()
const loading = ref(false)
const orders = ref<OrderListItem[]>([])
const total = ref(0)

const query = reactive({
  status: undefined as number | undefined,
  page: 1,
  size: 10,
})

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待支付', value: ORDER_STATUS_PENDING },
  { label: '已支付', value: ORDER_STATUS_PAID },
  { label: '已取消', value: ORDER_STATUS_CANCELED },
]

async function loadOrders() {
  loading.value = true
  try {
    const page = await listOrders(query)
    orders.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function changeStatus(value: number | undefined) {
  query.status = value
  query.page = 1
  loadOrders()
}

function openDetail(order: OrderListItem) {
  router.push(`/orders/${order.orderId}`)
}

async function cancel(order: OrderListItem) {
  await ElMessageBox.confirm(`确定取消订单 ${order.orderNo} 吗？`, '取消订单', {
    type: 'warning',
    confirmButtonText: '取消订单',
    cancelButtonText: '再想想',
  })
  await cancelOrder(order.orderId)
  ElMessage.success('订单已取消')
  await loadOrders()
}

async function pay(order: OrderListItem) {
  await ElMessageBox.confirm(`确认使用 MOCK 渠道模拟支付订单 ${order.orderNo} 吗？`, '模拟支付', {
    type: 'warning',
    confirmButtonText: '模拟支付',
    cancelButtonText: '取消',
  })
  await payOrder(order.orderId)
  ElMessage.success('支付成功')
  await loadOrders()
}

onMounted(loadOrders)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>我的订单</h1>
        <p>查看订单状态，待支付订单可取消或执行本地模拟支付。</p>
      </div>
      <el-button :icon="Refresh" plain @click="loadOrders">刷新</el-button>
    </div>

    <div class="content-panel order-panel">
      <div class="toolbar">
        <el-segmented
          :model-value="query.status"
          :options="statusOptions"
          @update:model-value="(value: number | undefined) => changeStatus(value)"
        />
      </div>

      <el-table v-loading="loading" :data="orders" row-key="orderId">
        <el-table-column label="订单号" prop="orderNo" min-width="220" />
        <el-table-column label="总金额" width="130">
          <template #default="{ row }: { row: OrderListItem }">
            <strong class="price">{{ formatMoney(row.totalAmount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }: { row: OrderListItem }">
            <el-tag :type="orderStatusTagType(row.status)">{{ orderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }: { row: OrderListItem }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="支付时间" min-width="170">
          <template #default="{ row }: { row: OrderListItem }">{{ formatTime(row.paidAt) }}</template>
        </el-table-column>
        <el-table-column label="取消时间" min-width="170">
          <template #default="{ row }: { row: OrderListItem }">{{ formatTime(row.canceledAt) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="260">
          <template #default="{ row }: { row: OrderListItem }">
            <el-button :icon="View" text type="primary" @click="openDetail(row)">详情</el-button>
            <template v-if="row.status === ORDER_STATUS_PENDING">
              <el-button text type="warning" @click="cancel(row)">取消</el-button>
              <el-button :icon="CreditCard" text type="success" @click="pay(row)">支付</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && orders.length === 0" description="暂无订单">
        <el-button type="primary" @click="router.push('/products')">去浏览商品</el-button>
      </el-empty>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="loadOrders"
          @size-change="loadOrders"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.order-panel {
  padding: 14px 8px 18px;
}

.toolbar {
  padding: 0 8px 14px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin: 18px 10px 0;
}
</style>
