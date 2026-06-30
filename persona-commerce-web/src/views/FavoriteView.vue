<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete, View } from '@element-plus/icons-vue'
import { listFavorites, removeFavorite, type FavoriteItem } from '@/api/shopping'
import ProductImage from '@/components/ProductImage.vue'

const router = useRouter()
const loading = ref(false)
const favorites = ref<FavoriteItem[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
})

function formatPrice(value?: number) {
  return value == null ? '价格待确认' : `￥${value}`
}

async function loadFavorites() {
  loading.value = true
  try {
    const page = await listFavorites(query)
    favorites.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function cancelFavorite(item: FavoriteItem) {
  await removeFavorite(item.skuId)
  ElMessage.success('已取消收藏')
  if (favorites.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  await loadFavorites()
}

function openProduct(item: FavoriteItem) {
  router.push(`/products/${item.spuId}`)
}

onMounted(loadFavorites)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>我的收藏</h1>
        <p>收藏对象为 SKU，商品展示信息由 shopping 通过 ProductQueryApi 快照提供。</p>
      </div>
      <span class="muted">共 {{ total }} 条收藏</span>
    </div>

    <div class="content-panel favorite-panel">
      <el-table v-loading="loading" :data="favorites" row-key="favoriteId">
        <el-table-column label="商品" min-width="360">
          <template #default="{ row }: { row: FavoriteItem }">
            <div class="product-cell">
              <ProductImage
                class="product-thumb"
                :src="row.imageUrl"
                :label="row.categoryName"
                :product-name="row.productName"
                :sku-name="row.skuName"
                :category-name="row.categoryName"
              />
              <div>
                <strong>{{ row.productName }}</strong>
                <span>{{ row.skuName }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" prop="categoryName" width="140" />
        <el-table-column label="价格" width="140">
          <template #default="{ row }: { row: FavoriteItem }">
            <span class="price">{{ formatPrice(row.unitPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收藏时间" prop="createdAt" min-width="180" />
        <el-table-column fixed="right" label="操作" width="190">
          <template #default="{ row }: { row: FavoriteItem }">
            <el-button :icon="View" text type="primary" @click="openProduct(row)">查看</el-button>
            <el-button :icon="Delete" text type="danger" @click="cancelFavorite(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && favorites.length === 0" description="还没有收藏的商品" />

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="loadFavorites"
          @size-change="loadFavorites"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.favorite-panel {
  padding: 8px 8px 18px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-thumb {
  width: 72px;
  height: 72px;
  border-radius: 6px;
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

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin: 18px 10px 0;
}
</style>
