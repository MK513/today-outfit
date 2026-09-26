<script setup>
import { computed } from 'vue'
import { categoryPhotoUrl } from '@/lib/categoryPhotos'

const props = defineProps({
  clothing: { type: Object, default: null },
  size: { type: Number, default: 64 },
})

// 실사 사진(사용자 업로드 또는 카테고리+색상 대표 사진)만 표시한다. 매칭되는 사진이 없으면 빈 상태로 둔다.
const photoUrl = computed(
  () => props.clothing?.imageUrl || categoryPhotoUrl(props.clothing?.category, props.clothing?.color),
)
</script>

<template>
  <div class="thumb" :style="{ width: size + 'px', height: size + 'px' }">
    <img v-if="photoUrl" class="photo" :src="photoUrl" :alt="clothing?.name ?? clothing?.category" />
    <span v-else class="empty" :title="clothing?.name">사진 없음</span>
  </div>
</template>

<style scoped>
.thumb {
  border-radius: var(--radius-sm);
  background: var(--accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.thumb .photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb .empty {
  font-size: 10px;
  color: var(--muted);
  text-align: center;
  padding: 0 4px;
}
</style>
