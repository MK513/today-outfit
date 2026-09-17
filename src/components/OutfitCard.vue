<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useWardrobeStore } from '@/stores/wardrobe'
import ClothingThumb from './ClothingThumb.vue'
import { OUTFIT_SOURCE_LABEL } from '@/lib/constants'

const props = defineProps({
  outfit: { type: Object, required: true },
  clickable: { type: Boolean, default: true },
})

const router = useRouter()
const wardrobe = useWardrobeStore()

const items = computed(() =>
  props.outfit.clothingIds.map((id) => wardrobe.byId(id)).filter(Boolean).slice(0, 3),
)

function open() {
  if (props.clickable) router.push({ name: 'outfit-detail', params: { id: props.outfit.id } })
}
</script>

<template>
  <button type="button" class="outfit-card" @click="open">
    <div class="thumbs">
      <ClothingThumb v-for="item in items" :key="item.id" :clothing="item" :size="56" />
    </div>
    <div class="meta">
      <p class="name">{{ outfit.name }}</p>
      <div class="row">
        <span class="chip">{{ OUTFIT_SOURCE_LABEL[outfit.source] ?? outfit.source }}</span>
        <span v-if="outfit.aiScore != null" class="chip chip-muted">AI {{ outfit.aiScore }}점</span>
      </div>
    </div>
  </button>
</template>

<style scoped>
.outfit-card {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 12px;
  cursor: pointer;
  text-align: left;
}

.thumbs {
  display: flex;
}

.thumbs :deep(.thumb) {
  border: 2px solid var(--surface);
  margin-left: -12px;
}

.thumbs :deep(.thumb:first-child) {
  margin-left: 0;
}

.meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.name {
  font-weight: 700;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row {
  display: flex;
  gap: 6px;
}
</style>
