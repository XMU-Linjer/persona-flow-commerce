<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, ShoppingCart, View } from '@element-plus/icons-vue'
import {
  clearCart,
  deleteCartItem,
  listCartItems,
  updateCartItem,
  type CartItem,
} from '@/api/shopping'
import { saveCheckoutItems } from '@/utils/checkout'

const router = useRouter()
const loading = ref(false)
const updatingId = ref<number>()
const cartItems = ref<CartItem[]>([])

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + Number(item.subtotal || 0), 0)
})

function formatMoney(value?: number) {
  return value == null ? '￥0.00' : `￥${Number(value).toFixed(2)}`
}

async function loadCart() {
  loading.value = true
  try {
    cartItems.value = await listCartItems()
  } finally {
    loading.value = false
  }
}

async function changeQuantity(item: CartItem, quantity: number | undefined) {
  if (!quantity || quantity <= 0) return

  updatingId.value = item.cartItemId
  try {
    await updateCartItem(item.cartItemId, { quantity })
    ElMessage.success('数量已更新')
    await loadCart()
  } finally {
    updatingId.value = undefined
  }
}

async function removeItem(item: CartItem) {
  await deleteCartItem(item.cartItemId)
  ElMessage.success('已删除购物车项')
  await loadCart()
}

async function clearAll() {
  if (cartItems.value.length === 0) return

  await ElMessageBox.confirm('确定清空购物车吗？', '清空购物车', {
    type: 'warning',
    confirmButtonText: '清空',
    cancelButtonText: '取消',
  })
  await clearCart()
  ElMessage.success('购物车已清空')
  await loadCart()
}

function openProduct(item: CartItem) {
  router.push(`/products/${item.spuId}`)
}

function goCheckout() {
  if (cartItems.value.length === 0) return

  saveCheckoutItems(
    cartItems.value.map((item) => ({
      skuId: item.skuId,
      spuId: item.spuId,
      categoryId: item.categoryId,
      categoryName: item.categoryName,
      productName: item.productName,
      skuName: item.skuName,
      imageUrl: item.imageUrl,
      unitPrice: Number(item.unitPrice || 0),
      quantity: item.quantity,
      subtotal: Number(item.subtotal || 0),
    })),
  )
  router.push('/checkout')
}

onMounted(loadCart)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>购物车</h1>
        <p>购物车只保存 skuId 和 quantity，展示信息来自商品快照。</p>
      </div>
      <el-button :icon="Delete" plain type="danger" :disabled="cartItems.length === 0" @click="clearAll">
        清空购物车
      </el-button>
    </div>

    <div class="content-panel cart-panel">
      <el-table v-loading="loading" :data="cartItems" row-key="cartItemId">
        <el-table-column label="商品" min-width="360">
          <template #default="{ row }: { row: CartItem }">
            <div class="product-cell">
              <el-image class="product-thumb" fit="cover" :src="row.imageUrl">
                <template #error>
                  <div class="thumb-fallback">{{ row.categoryName }}</div>
                </template>
              </el-image>
              <div>
                <strong>{{ row.productName }}</strong>
                <span>{{ row.skuName }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" prop="categoryName" width="130" />
        <el-table-column label="单价" width="120">
          <template #default="{ row }: { row: CartItem }">
            <span class="price">{{ formatMoney(row.unitPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="170">
          <template #default="{ row }: { row: CartItem }">
            <el-input-number
              :model-value="row.quantity"
              :min="1"
              :max="99"
              :disabled="updatingId === row.cartItemId"
              size="small"
              @change="(value: number | undefined) => changeQuantity(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="130">
          <template #default="{ row }: { row: CartItem }">
            <strong class="price">{{ formatMoney(row.subtotal) }}</strong>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="190">
          <template #default="{ row }: { row: CartItem }">
            <el-button :icon="View" text type="primary" @click="openProduct(row)">查看</el-button>
            <el-button :icon="Delete" text type="danger" @click="removeItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && cartItems.length === 0" description="购物车还是空的">
        <el-button :icon="ShoppingCart" type="primary" @click="router.push('/products')">去逛逛</el-button>
      </el-empty>

      <div class="cart-summary">
        <span>合计</span>
        <strong>{{ formatMoney(totalAmount) }}</strong>
        <el-button type="primary" :disabled="cartItems.length === 0" @click="goCheckout">
          去下单
        </el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.cart-panel {
  padding: 8px 8px 18px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-thumb,
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

.product-cell strong,
.product-cell span {
  display: block;
}

.product-cell strong {
  color: #1f2a37;
  font-weight: 700;
}

.product-cell span {
  margin-top: 4px;
  color: #6b7280;
}

.cart-summary {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  margin: 18px 10px 0;
}

.cart-summary span {
  color: #6b7280;
}

.cart-summary strong {
  color: #c25b19;
  font-size: 24px;
  font-weight: 800;
}
</style>
