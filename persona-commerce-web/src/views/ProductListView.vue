<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { listProducts, type ProductListItem } from '@/api/catalog'
import ProductImage from '@/components/ProductImage.vue'

const router = useRouter()
const loading = ref(false)
const products = ref<ProductListItem[]>([])
const total = ref(0)

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

function search() {
  query.page = 1
  loadProducts()
}

function openDetail(product: ProductListItem) {
  router.push(`/products/${product.spuId}`)
}

onMounted(loadProducts)
</script>

<template>
  <section>
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
