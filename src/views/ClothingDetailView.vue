<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useWardrobeStore } from '@/stores/wardrobe'
import { categoryLabel, seasonLabel } from '@/lib/constants'
import ClothingThumb from '@/components/ClothingThumb.vue'
import TopBar from '@/components/TopBar.vue'
import { useToast } from '@/composables/useToast'

const props = defineProps({ id: { type: String, required: true } })
const wardrobe = useWardrobeStore()
const router = useRouter()
const { show } = useToast()

const clothing = computed(() => wardrobe.byId(props.id))

const SOURCE_LABEL = { PHOTO: '사진 등록', TEXT: '문장 등록', MANUAL: '직접 등록' }

function remove() {
  if (!clothing.value) return
  wardrobe.remove(clothing.value.id)
  show('삭제했어요')
  router.replace({ name: 'wardrobe' })
}
</script>

<template>
  <div class="page" v-if="clothing">
    <TopBar title="의류 상세" back />

    <div class="card" style="display: flex; flex-direction: column; gap: 16px; align-items: center">
      <ClothingThumb :clothing="clothing" :size="180" />
      <div style="width: 100%; display: flex; flex-direction: column; gap: 10px">
        <p style="font-size: 19px; font-weight: 800">{{ clothing.name }}</p>
        <div style="display: flex; gap: 6px; flex-wrap: wrap">
          <span class="chip">{{ categoryLabel(clothing.category) }}</span>
          <span class="chip chip-muted">{{ clothing.color }}</span>
          <span class="chip chip-muted">{{ seasonLabel(clothing.season) }}</span>
        </div>
        <div class="detail-row">
          <span>등록 경로</span>
          <span>{{ SOURCE_LABEL[clothing.source] ?? clothing.source }}</span>
        </div>
        <div v-if="clothing.fileName" class="detail-row">
          <span>파일명</span>
          <span>{{ clothing.fileName }}</span>
        </div>
        <div class="detail-row">
          <span>등록일</span>
          <span>{{ new Date(clothing.createdAt).toLocaleDateString('ko-KR') }}</span>
        </div>
      </div>
    </div>

    <button class="btn btn-danger btn-block" type="button" @click="remove">이 옷 삭제하기</button>
  </div>
  <div class="page" v-else>
    <TopBar title="의류 상세" back />
    <p class="hint-text">삭제되었거나 존재하지 않는 의류입니다.</p>
  </div>
</template>

<style scoped>
.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--muted);
  border-top: 1px solid var(--line);
  padding-top: 8px;
}
</style>
