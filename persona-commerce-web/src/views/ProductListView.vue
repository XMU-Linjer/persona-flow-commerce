<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, DataAnalysis } from '@element-plus/icons-vue'
import { listProducts, type ProductListItem } from '@/api/catalog'
import { getLatestProfile, getBehaviorSummary, type BehaviorSummary, type UserProfileLatest } from '@/api/behavior'
import ProductImage from '@/components/ProductImage.vue'

interface ProfilePayload {
  preferenceTags?: string[]
  complementOpportunities?: Array<{ label?: string }>
  doNotRecommend?: Array<{ spuId?: number; skuId?: number }>
}

const router = useRouter()
const loading = ref(false)
const products = ref<ProductListItem[]>([])
const total = ref(0)

const recommendationLoading = ref(false)
const recommendedProducts = ref<ProductListItem[]>([])
const recommendationReasons = ref<Record<number, string>>({})
const hasProfile = ref(false)

const query = reactive({
  keyword: '',
  page: 1,
  size: 10,
})

const pageTotalText = computed(() => `共 ${total.value} 件商品`)

function formatPrice(product: ProductListItem) {
  if (product.minPrice == null && product.maxPrice == null) return '价格待确认'
  if (product.minPrice === product.maxPrice || product.maxPrice == null) return `￥${product.minPrice}`
  return `￥${product.minPrice} - ￥${product.maxPrice}`
}

function tagsOf(product: ProductListItem) {
  return (product.tags || '')
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
    .slice(0, 3)
}

async function loadProducts() {
  loading.value = true
  try {
    const page = await listProducts({
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size,
    })
    products.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function loadHomeRecommendations() {
  recommendationLoading.value = true
  try {
    const [latest, summary] = await Promise.allSettled([
      getLatestProfile(),
      getBehaviorSummary(),
    ])

    const profileResult = latest.status === 'fulfilled' ? latest.value : null
    const summaryResult = summary.status === 'fulfilled' ? summary.value : null

    const keywords = extractRecommendationKeywords(profileResult, summaryResult)
    const suppressedSpuIds = extractSuppressedSpuIds(profileResult)

    if (keywords.length === 0) {
      return
    }

    hasProfile.value = profileResult?.exists ?? false
    const productBySpu = new Map<number, ProductListItem>()
    const reasonBySpu: Record<number, string> = {}

    for (const keyword of keywords) {
      const page = await listProducts({
        keyword,
        page: 1,
        size: 3,
      })

      for (const product of page.records) {
        if (
          suppressedSpuIds.has(product.spuId) ||
          productBySpu.has(product.spuId)
        ) {
          continue
        }
        productBySpu.set(product.spuId, product)
        reasonBySpu[product.spuId] = keyword
      }

      if (productBySpu.size >= 6) {
        break
      }
    }

    recommendedProducts.value = Array.from(productBySpu.values()).slice(0, 6)
    recommendationReasons.value = reasonBySpu
  } finally {
    recommendationLoading.value = false
  }
}

function extractRecommendationKeywords(
  latest: UserProfileLatest | null,
  summary: BehaviorSummary | null,
): string[] {
  const profileKeywords = profileComplementLabels(latest)
  if (profileKeywords.length > 0) {
    return profileKeywords.slice(0, 6)
  }

  const summaryKeywords = summary?.recentKeywords ?? []
  return summaryKeywords.slice(0, 6)
}

function profileComplementLabels(latest: UserProfileLatest | null): string[] {
  if (!latest?.profileJson) return []

  try {
    const doc = JSON.parse(latest.profileJson)
    const payload: ProfilePayload | undefined = doc?.profile
    const opportunities = payload?.complementOpportunities ?? []
    const labels = opportunities
      .map((item) => item.label ?? '')
      .filter(Boolean)
      .slice(0, 6)

    if (labels.length > 0) return labels

    return payload?.preferenceTags?.filter(Boolean).slice(0, 6) ?? []
  } catch {
    return []
  }
}

function extractSuppressedSpuIds(latest: UserProfileLatest | null): Set<number> {
  if (!latest?.profileJson) return new Set()

  try {
    const doc = JSON.parse(latest.profileJson)
    const payload: ProfilePayload | undefined = doc?.profile
    const doNotRecommend = payload?.doNotRecommend ?? []
    return new Set(
      doNotRecommend
        .map((item) => item.spuId)
        .filter((value): value is number => value != null),
    )
  } catch {
    return new Set()
  }
}

function formatMoney(value?: number) {
  if (value == null) return '-'
  return `¥${Number(value).toFixed(2)}`
}

function search() {
  query.page = 1
  loadProducts()
}

function openDetail(product: ProductListItem) {
  router.push(`/products/${product.spuId}`)
}

onMounted(() => {
  loadProducts()
  loadHomeRecommendations()
})
</script>

<template>
  <section>
    <!-- ---------- Personalized Recommendation Section ---------- -->
    <section v-if="recommendedProducts.length > 0" class="recommend-section">
      <div class="recommend-header">
        <div>
          <h2>
            <el-icon><DataAnalysis /></el-icon>
            为你推荐
          </h2>
          <p>
            <template v-if="hasProfile">基于你的行为画像和配套机会匹配</template>
            <template v-else>基于你的近期浏览和搜索关键词</template>
          </p>
        </div>
        <router-link class="insight-link" to="/ai-insights">
          查看更多 AI 购物洞察 →
        </router-link>
      </div>

      <el-skeleton :loading="recommendationLoading" animated :count="6">
        <template #template>
          <div class="product-grid">
            <el-skeleton-item
              v-for="item in 6"
              :key="item"
              class="product-skeleton"
              variant="rect"
            />
          </div>
        </template>

        <div class="product-grid">
          <article
            v-for="product in recommendedProducts"
            :key="product.spuId"
            class="product-card recommend-card"
            @click="openDetail(product)"
          >
            <ProductImage
              class="product-image"
              :src="product.mainImageUrl"
              :label="product.brand || product.categoryName"
              :product-name="product.name"
              :category-name="product.categoryName"
            />
            <div class="product-info">
              <div class="product-meta">
                <el-tag size="small" type="warning">{{ product.categoryName }}</el-tag>
                <span class="recommend-reason">
                  {{ recommendationReasons[product.spuId] || '画像推荐' }}
                </span>
              </div>
              <h2>{{ product.name }}</h2>
              <p>{{ product.subtitle || '根据你的兴趣推荐的优选商品' }}</p>
              <div class="product-footer">
                <span class="price">{{ formatPrice(product) }}</span>
                <span class="muted">销量 {{ product.salesCount || 0 }}</span>
              </div>
            </div>
          </article>
        </div>
      </el-skeleton>
    </section>
    <!-- ========================================================== -->

    <div class="page-title">
      <div>
        <h1>商品浏览</h1>
        <p>按关键词浏览 V1.0 商品目录，商品详情和 SKU 信息来自 catalog 模块。</p>
      </div>
      <span class="muted">{{ pageTotalText }}</span>
    </div>

    <div class="toolbar content-panel">
      <el-input
        v-model.trim="query.keyword"
        clearable
        placeholder="搜索商品名、副标题或品牌"
        :prefix-icon="Search"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">搜索</el-button>
    </div>

    <el-skeleton :loading="loading" animated :count="6">
      <template #template>
        <div class="product-grid">
          <el-skeleton-item v-for="item in 6" :key="item" class="product-skeleton" variant="rect" />
        </div>
      </template>

      <el-empty v-if="products.length === 0" description="暂时没有匹配的商品" />

      <div v-else class="product-grid">
        <article
          v-for="product in products"
          :key="product.spuId"
          class="product-card"
          @click="openDetail(product)"
        >
          <ProductImage
            class="product-image"
            :src="product.mainImageUrl"
            :label="product.brand || product.categoryName"
            :product-name="product.name"
            :category-name="product.categoryName"
          />

          <div class="product-info">
            <div class="product-meta">
              <el-tag size="small" type="success">{{ product.categoryName }}</el-tag>
              <span>{{ product.brand || '精选品牌' }}</span>
            </div>
            <h2>{{ product.name }}</h2>
            <p>{{ product.subtitle || '适合日常选购的优选商品' }}</p>
            <div class="tag-row">
              <el-tag v-for="tag in tagsOf(product)" :key="tag" effect="plain" size="small">
                {{ tag }}
              </el-tag>
            </div>
            <div class="product-footer">
              <span class="price">{{ formatPrice(product) }}</span>
              <span class="muted">销量 {{ product.salesCount || 0 }}</span>
            </div>
          </div>
        </article>
      </div>
    </el-skeleton>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="prev, pager, next, sizes"
        :page-sizes="[8, 10, 16, 20]"
        :total="total"
        @current-change="loadProducts"
        @size-change="search"
      />
    </div>
  </section>
</template>

<style scoped>
/* --- Recommendation Section --- */
.recommend-section {
  margin-bottom: 32px;
}

.recommend-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.recommend-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 20px;
  color: #1f2a37;
  font-weight: 800;
}

.recommend-header p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 14px;
}

.insight-link {
  color: #409eff;
  text-decoration: none;
  font-size: 14px;
  white-space: nowrap;
}

.insight-link:hover {
  text-decoration: underline;
}

.recommend-reason {
  overflow: hidden;
  color: #d97706;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
}

.recommend-card {
  border-left: 3px solid #e6a817;
}

/* --- Existing Styles --- */
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
}

.toolbar .el-input {
  max-width: 420px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 18px;
}

.product-card {
  min-height: 390px;
  overflow: hidden;
  border: 1px solid #e3e9f0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.product-card:hover {
  transform: translateY(-3px);
  border-color: #9ccbbc;
  box-shadow: 0 16px 34px rgba(31, 42, 55, 0.1);
}

.product-image {
  width: 100%;
  height: 180px;
}

.product-info {
  display: flex;
  min-height: 210px;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
}

.product-meta,
.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.product-meta span {
  overflow: hidden;
  color: #6b7280;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-info h2 {
  display: -webkit-box;
  min-height: 48px;
  margin: 0;
  overflow: hidden;
  color: #1f2a37;
  font-size: 18px;
  font-weight: 700;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.product-info p {
  display: -webkit-box;
  min-height: 44px;
  margin: 0;
  overflow: hidden;
  color: #6b7280;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.tag-row {
  display: flex;
  min-height: 24px;
  flex-wrap: wrap;
  gap: 6px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
}

.product-skeleton {
  height: 390px;
  border-radius: 8px;
}

@media (max-width: 620px) {
  .toolbar {
    flex-direction: column;
  }

  .toolbar .el-input {
    max-width: none;
  }

  .pagination-row {
    justify-content: center;
  }
}
</style>