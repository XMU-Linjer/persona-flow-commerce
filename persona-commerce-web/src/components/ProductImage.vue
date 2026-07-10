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
  [['pillowcase', '枕套'], '/product-images/pillowcase.webp'],
  [['pillow', '记忆棉枕', '枕头'], '/product-images/pillow.webp'],
  [['bedding', '四件套', '床品', 'linennest'], '/product-images/bedding.webp'],
  [['aroma-lamp', 'aroma', 'diffuser', '香薰', '助眠灯'], '/product-images/aroma-lamp.webp'],

  [['coffee-machine', 'coffee machine', '咖啡机', '胶囊咖啡机'], '/product-images/coffee-machine.webp'],
  [['coffee-filter', 'coffee filter', '滤纸'], '/product-images/coffee-filter.webp'],
  [['coffee-beans', 'coffee beans', '咖啡豆', 'mountain roast'], '/product-images/coffee-beans.webp'],
  [['thermos', '保温杯', '随行杯'], '/product-images/thermos.webp'],
  [['tea', 'oolong', 'calmleaf', '乌龙茶', '茶饮'], '/product-images/tea.webp'],

  [['backpack', '背包', '通勤包'], '/product-images/backpack.webp'],
  [['organizer', '收纳包', '数码收纳'], '/product-images/organizer.webp'],
  [['power-bank', 'power bank', '移动电源'], '/product-images/power-bank.webp'],
  [['travel-bottles', 'travel bottles', '分装瓶'], '/product-images/travel-bottles.webp'],

  [['yoga-mat', 'yoga mat', '瑜伽垫'], '/product-images/yoga-mat.webp'],
  [['massage-ball', 'massage ball', '筋膜球'], '/product-images/massage-ball.webp'],
  [['sports-bottle', 'sports bottle', '运动水杯', '运动水壶'], '/product-images/sports-bottle.webp'],
  [['towel', '速干毛巾', '毛巾'], '/product-images/towel.webp'],
  [['running-shoes', 'running shoes', '跑鞋'], '/product-images/running-shoes.webp'],
  [['jacket', '冲锋衣', '防风衣'], '/product-images/jacket.webp'],

  [['monitor-stand', 'monitor stand', '显示器支架'], '/product-images/monitor-stand.webp'],
  [['laptop-stand', 'laptop stand', '笔记本支架'], '/product-images/laptop-stand.webp'],
  [['keyboard', 'keyforge', '机械键盘', '键盘'], '/product-images/keyboard.webp'],
  [['mouse', '鼠标'], '/product-images/mouse.webp'],
  [['deskmat', 'desk mat', '桌垫'], '/product-images/deskmat.webp'],
  [['wrist-rest', 'wrist rest', '腕托'], '/product-images/wrist-rest.webp'],
  [['monitor', 'viewmate', '显示器'], '/product-images/monitor.webp'],
  [['chair', 'homeease', '人体工学椅'], '/product-images/chair.webp'],

  [['earbuds', 'cloudpods', '耳机', '降噪'], '/product-images/earbuds.webp'],
  [['speaker', 'soundbar', '音箱', '蓝牙音箱'], '/product-images/speaker.webp'],
  [['charger', 'voltix', 'gan', '快充', '充电器'], '/product-images/charger.webp'],
  [['ssd', 'datapocket', '固态硬盘', '移动硬盘'], '/product-images/ssd.webp'],
  [['steamer', 'puresteam', '挂烫机'], '/product-images/steamer.webp'],
  [['food-container', 'freshlock', '保鲜盒'], '/product-images/food-container.webp'],
  [['nuts', '坚果'], '/product-images/nuts.webp'],
  [['energy-bar', '能量棒', '燕麦棒'], '/product-images/energy-bar.webp'],

  [['storage-box', 'storage box', '收纳盒'], '/product-images/storage-box.webp'],
  [['cable-clip', 'cable clip', '理线夹'], '/product-images/cable-clip.webp'],
  [['pen-holder', 'pen holder', '笔筒'], '/product-images/pen-holder.webp'],
  [['desk-bin', 'desk bin', '桌面垃圾桶'], '/product-images/desk-bin.webp'],
  [['cleaning-brush', 'cleaning brush', '清洁刷'], '/product-images/cleaning-brush.webp'],
  [['duster', '除尘掸'], '/product-images/duster.webp'],
  [['wet-wipes-box', 'wet wipes', '湿巾盒'], '/product-images/wet-wipes-box.webp'],
  [['screen-cleaning-kit', 'screen cleaning', '屏幕清洁'], '/product-images/screen-cleaning-kit.webp'],
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

const primaryImage = computed(() => {
  const src = props.src?.trim()
  if (!src) {
    return ''
  }

  const lowerSrc = src.toLowerCase()
  const isLegacyLocalSvg = lowerSrc.startsWith('/product-images/') && lowerSrc.endsWith('.svg')
  if (isLegacyLocalSvg && mappedDemoImage.value) {
    return mappedDemoImage.value
  }

  return src
})
</script>

<template>
  <div class="product-image-shell">
    <el-image v-if="primaryImage" class="product-image-shell__image" :src="primaryImage" :fit="props.fit">
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
