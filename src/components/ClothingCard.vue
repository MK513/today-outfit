<script setup>
import { useRouter } from 'vue-router'
import ClothingThumb from './ClothingThumb.vue'
import { categoryLabel } from '@/lib/constants'

const props = defineProps({
  clothing: { type: Object, required: true },
  selectable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
})
const emit = defineEmits(['select'])

const router = useRouter()

function handleClick() {
  if (props.selectable) {
    emit('select', props.clothing)
  } else {
    router.push({ name: 'wardrobe-detail', params: { id: props.clothing.id } })
  }
}
</script>

<template>
  <button type="button" class="clothing-card" :class="{ selected }" @click="handleClick">
    <ClothingThumb :clothing="clothing" :size="96" />
    <div class="meta">
      <p class="name">{{ clothing.name }}</p>
      <span class="chip chip-muted">{{ categoryLabel(clothing.category) }}</span>
    </div>
    <span v-if="selectable" class="check-badge">{{ selected ? '✓' : '' }}</span>
  </button>
</template>

<style scoped>
.clothing-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 8px;
  text-align: left;
  cursor: pointer;
  width: 100%;
}

.clothing-card.selected {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px var(--accent-soft);
}

.clothing-card :deep(.thumb) {
  width: 100% !important;
  aspect-ratio: 1 / 1;
  height: auto !important;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.name {
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.check-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
