<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Goods, ShoppingCart, Star } from '@element-plus/icons-vue'
import { getProductDetail, type ProductDetail, type SkuItem } from '@/api/catalog'
import { addCartItem, addFavorite } from '@/api/shopping'
import { useAuthStore } from '@/stores/auth'
import { saveCheckoutItems } from '@/utils/checkout'
import ProductImage from '@/components/ProductImage.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const actionLoading = ref(false)
const product = ref<ProductDetail | null>(null)
const selectedSkuId = ref<number>()
const quantity = ref(1)

const selectedSku = computed(() => {
  return product.value?.skus.find((sku) => sku.skuId === selectedSkuId.value) || product.value?.skus[0]
})

const gallery = computed(() => {
  if (!product.value) return []
  return [product.value.mainImageUrl, ...(product.value.detailImages || [])].filter(Boolean) as string[]
})

function formatPrice(value?: number) {
  return value == null ? '价格待确认' : `￥${value}`
}

function entriesOf(data?: Record<string, unknown>) {
  return Object.entries(data || {}).map(([key, value]) => ({
    key,
    value: String(value),
  }))
}

function skuSpecs(sku: SkuItem) {
  const entries = entriesOf(sku.specs)
  if (entries.length === 0) return '默认规格'
  return entries.map((entry) => `${entry.key}: ${entry.value}`).join(' / ')
}

async function loadDetail() {
  const spuId = Number(route.params.spuId)
  if (!Number.isFinite(spuId)) return

  loading.value = true
  try {
    product.value = await getProductDetail(spuId)
    selectedSkuId.value = product.value.skus[0]?.skuId
  } finally {
    loading.value = false
  }
}

function requireLogin() {
  if (auth.isLoggedIn) {
    return true
  }

  ElMessage.warning('请先登录后再操作')
  router.push({
    path: '/login',
    query: { redirect: route.fullPath },
  })
  return false
}

async function favoriteSelectedSku() {
  if (!selectedSku.value || !requireLogin()) return

  actionLoading.value = true
  try {
    await addFavorite(selectedSku.value.skuId)
    ElMessage.success('已收藏该 SKU')
  } finally {
    actionLoading.value = false
  }
}

async function addSelectedSkuToCart() {
  if (!selectedSku.value || !requireLogin()) return

  actionLoading.value = true
  try {
    await addCartItem({
      skuId: selectedSku.value.skuId,
      quantity: quantity.value,
    })
    ElMessage.success('已加入购物车')
  } finally {
    actionLoading.value = false
  }
}

function buyNow() {
  if (!product.value || !selectedSku.value || !requireLogin()) return

  saveCheckoutItems([
    {
      skuId: selectedSku.value.skuId,
      spuId: product.value.spuId,
      categoryId: product.value.categoryId,
      categoryName: product.value.categoryName,
      productName: product.value.name,
      skuName: selectedSku.value.skuName,
      imageUrl: selectedSku.value.imageUrl || product.value.mainImageUrl,
      unitPrice: Number(selectedSku.value.price || 0),
      quantity: quantity.value,
      subtotal: Number(selectedSku.value.price || 0) * quantity.value,
    },
  ])
  router.push('/checkout')
}

onMounted(loadDetail)
watch(() => route.params.spuId, loadDetail)
</script>

<template>
  <section>
    <el-button :icon="ArrowLeft" text @click="router.push('/products')">返回商品列表</el-button>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <div class="detail-layout">
          <el-skeleton-item class="detail-image" variant="image" />
          <div>
            <el-skeleton-item variant="h1" />
            <el-skeleton-item variant="text" />
            <el-skeleton-item variant="text" />
          </div>
        </div>
      </template>

      <el-empty v-if="!product" description="商品不存在或已下架" />

      <div v-else class="detail-layout">
        <div class="media-panel content-panel">
          <ProductImage
            class="detail-image"
            :src="product.mainImageUrl"
            :label="product.brand || product.categoryName"
            :product-name="product.name"
            :category-name="product.categoryName"
          />

          <div v-if="gallery.length > 1" class="thumb-row">
            <ProductImage
              v-for="image in gallery"
              :key="image"
              class="thumb"
              :src="image"
              :label="product.brand || product.categoryName"
              :product-name="product.name"
              :category-name="product.categoryName"
            />
          </div>
        </div>

        <div class="info-panel content-panel">
          <el-tag type="success">{{ product.categoryName }}</el-tag>
          <h1>{{ product.name }}</h1>
          <p class="subtitle">{{ product.subtitle || product.description || '精选商品' }}</p>

          <div class="price-box">
            <span class="price">{{ formatPrice(selectedSku?.price) }}</span>
            <span v-if="selectedSku?.originalPrice" class="original-price">
              原价 {{ formatPrice(selectedSku.originalPrice) }}
            </span>
          </div>

          <el-descriptions :column="2" border class="descriptions">
            <el-descriptions-item label="品牌">{{ product.brand || '未标注' }}</el-descriptions-item>
            <el-descriptions-item label="SPU">{{ product.spuId }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ product.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="标签">{{ product.tags || '暂无' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-block">
            <h2>SKU</h2>
            <el-radio-group v-model="selectedSkuId" class="sku-list">
              <el-radio-button v-for="sku in product.skus" :key="sku.skuId" :label="sku.skuId">
                {{ sku.skuName }}
              </el-radio-button>
            </el-radio-group>
            <p class="muted">{{ selectedSku ? skuSpecs(selectedSku) : '暂无规格' }}</p>
          </div>

          <div class="section-block">
            <h2>数量</h2>
            <el-input-number v-model="quantity" :min="1" :max="99" />
          </div>

          <div v-if="entriesOf(product.attributes).length" class="section-block">
            <h2>商品属性</h2>
            <div class="attribute-grid">
              <div v-for="item in entriesOf(product.attributes)" :key="item.key">
                <span>{{ item.key }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </div>

          <div class="action-row">
            <el-button :icon="Star" :loading="actionLoading" plain @click="favoriteSelectedSku">
              收藏
            </el-button>
            <el-button :icon="ShoppingCart" type="primary" :loading="actionLoading" plain @click="addSelectedSkuToCart">
              加入购物车
            </el-button>
            <el-button :icon="Goods" type="warning" @click="buyNow">立即购买</el-button>
          </div>
        </div>
      </div>
    </el-skeleton>
  </section>
</template>

<style scoped>
.detail-layout {
  display: grid;
  grid-template-columns: minmax(280px, 440px) minmax(0, 1fr);
  gap: 22px;
  margin-top: 14px;
}

.media-panel,
.info-panel {
  padding: 18px;
}

.detail-image {
  width: 100%;
  height: 430px;
  border-radius: 8px;
}

.thumb-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-top: 12px;
}

.thumb {
  height: 76px;
  border-radius: 6px;
}

.info-panel h1 {
  margin: 14px 0 8px;
  color: #1f2a37;
  font-size: 30px;
  font-weight: 700;
}

.subtitle {
  margin: 0 0 18px;
  color: #6b7280;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 18px;
}

.price-box .price {
  font-size: 30px;
}

.original-price {
  color: #9ca3af;
  text-decoration: line-through;
}

.descriptions {
  margin-bottom: 20px;
}

.section-block {
  margin-top: 20px;
}

.section-block h2 {
  margin: 0 0 10px;
  color: #1f2a37;
  font-size: 18px;
  font-weight: 700;
}

.sku-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.attribute-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
}

.attribute-grid div {
  padding: 10px 12px;
  border: 1px solid #e3e9f0;
  border-radius: 6px;
  background: #f8fafc;
}

.attribute-grid span,
.attribute-grid strong {
  display: block;
}

.attribute-grid span {
  color: #6b7280;
  font-size: 13px;
}

.attribute-grid strong {
  margin-top: 4px;
  color: #1f2a37;
  font-weight: 600;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

@media (max-width: 980px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .detail-image {
    height: 320px;
  }
}
</style>
