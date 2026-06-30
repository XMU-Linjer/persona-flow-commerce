<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string
    label?: string
    productName?: string
    skuName?: string
    categoryName?: string
    fit?: 'fill' | 'contain' | 'cover' | 'none' | 'scale-down'
  }>(),
  {
    fit: 'cover',
    label: 'PersonaFlow',
  },
)

const demoImageRules: Array<[string[], string]> = [
  [['pillowcase', '枕套'], '/product-images/pillowcase.svg'],
  [['pillow', '记忆棉枕', '枕头'], '/product-images/pillow.svg'],
  [['bedding', '四件套', '床品'], '/product-images/bedding.svg'],
  [['aroma', 'sleep lamp', '香薰', '助眠灯'], '/product-images/aroma-lamp.svg'],
  [['keyboard', 'keyforge', '机械键盘', '键盘'], '/product-images/keyboard.svg'],
  [['mouse', '鼠标'], '/product-images/mouse.svg'],
  [['deskmat', 'desk mat', '桌垫'], '/product-images/deskmat.svg'],
  [['monitor-stand', 'monitor stand', '显示器支架'], '/product-images/monitor-stand.svg'],
  [['wrist-rest', 'wrist rest', '腕托'], '/product-images/wrist-rest.svg'],
  [['backpack', '背包'], '/product-images/backpack.svg'],
  [['organizer', '收纳包', '数码收纳'], '/product-images/organizer.svg'],
  [['power-bank', 'power bank', '移动电源'], '/product-images/power-bank.svg'],
  [['travel-bottles', 'travel bottles', '分装瓶'], '/product-images/travel-bottles.svg'],
  [['coffee-machine', 'coffee machine', '咖啡机'], '/product-images/coffee-machine.svg'],
  [['coffee-beans', 'coffee beans', '咖啡豆'], '/product-images/coffee-beans.svg'],
  [['coffee-filter', 'coffee filter', '滤纸'], '/product-images/coffee-filter.svg'],
  [['thermos', '保温杯'], '/product-images/thermos.svg'],
  [['yoga-mat', 'yoga mat', '瑜伽垫'], '/product-images/yoga-mat.svg'],
  [['massage-ball', 'massage ball', '筋膜球'], '/product-images/massage-ball.svg'],
  [['sports-bottle', 'sports bottle', '运动水杯'], '/product-images/sports-bottle.svg'],
  [['towel', '速干毛巾', '毛巾'], '/product-images/towel.svg'],
]

const mappedDemoImage = computed(() => {
  const sourceText = [
    props.productName,
    props.skuName,
    props.categoryName,
    props.label,
    props.src,
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()

  for (const [patterns, image] of demoImageRules) {
    if (patterns.some((pattern) => sourceText.includes(pattern.toLowerCase()))) {
      return image
    }
  }
  return ''
})
</script>

<template>
  <div class="product-image-shell">
    <el-image v-if="props.src" class="product-image-shell__image" :src="props.src" :fit="props.fit">
      <template #error>
        <img
          v-if="mappedDemoImage"
          class="product-image-shell__local"
          :src="mappedDemoImage"
          :alt="props.productName || props.skuName || props.label || 'demo product image'"
        />
        <div v-else class="product-image-shell__fallback">
          <span>PF</span>
          <strong>{{ props.label || 'PersonaFlow' }}</strong>
        </div>
      </template>
    </el-image>

    <img
      v-else-if="mappedDemoImage"
      class="product-image-shell__local"
      :src="mappedDemoImage"
      :alt="props.productName || props.skuName || props.label || 'demo product image'"
    />

    <div v-else class="product-image-shell__fallback">
      <span>PF</span>
      <strong>{{ props.label || 'PersonaFlow' }}</strong>
    </div>
  </div>
</template>

<style scoped>
.product-image-shell {
  overflow: hidden;
  background: #edf3f0;
}

.product-image-shell__image,
.product-image-shell__local,
.product-image-shell__fallback {
  width: 100%;
  height: 100%;
}

.product-image-shell__local {
  display: block;
  object-fit: cover;
}

.product-image-shell__image :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
}

.product-image-shell__fallback {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: inherit;
  background:
    linear-gradient(135deg, rgba(31, 143, 117, 0.14), rgba(221, 159, 84, 0.18)),
    radial-gradient(circle at 22% 18%, rgba(255, 255, 255, 0.86), transparent 28%),
    #edf3f0;
  color: #53606f;
  text-align: center;
}

.product-image-shell__fallback span {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid rgba(31, 143, 117, 0.22);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  color: #1f8f75;
  font-size: 13px;
  font-weight: 800;
}

.product-image-shell__fallback strong {
  max-width: 92%;
  overflow: hidden;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
