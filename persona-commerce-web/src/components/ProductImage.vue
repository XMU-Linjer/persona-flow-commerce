<script setup lang="ts">
withDefaults(
  defineProps<{
    src?: string
    label?: string
    fit?: 'fill' | 'contain' | 'cover' | 'none' | 'scale-down'
  }>(),
  {
    fit: 'cover',
    label: 'PersonaFlow',
  },
)
</script>

<template>
  <div class="product-image-shell">
    <el-image v-if="src" class="product-image-shell__image" :src="src" :fit="fit">
      <template #error>
        <div class="product-image-shell__fallback">
          <span>PF</span>
          <strong>{{ label || 'PersonaFlow' }}</strong>
        </div>
      </template>
    </el-image>

    <div v-else class="product-image-shell__fallback">
      <span>PF</span>
      <strong>{{ label || 'PersonaFlow' }}</strong>
    </div>
  </div>
</template>

<style scoped>
.product-image-shell {
  overflow: hidden;
  background: #edf3f0;
}

.product-image-shell__image,
.product-image-shell__fallback {
  width: 100%;
  height: 100%;
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
