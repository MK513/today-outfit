<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useOutfitsStore } from '@/stores/outfits'
import { useWardrobeStore } from '@/stores/wardrobe'
import { OUTFIT_SOURCE_LABEL } from '@/lib/constants'
import ClothingThumb from '@/components/ClothingThumb.vue'
import TopBar from '@/components/TopBar.vue'
import Icon from '@/components/Icon.vue'
import { useToast } from '@/composables/useToast'

const props = defineProps({ id: { type: String, required: true } })
const outfits = useOutfitsStore()
const wardrobe = useWardrobeStore()
const router = useRouter()
const { show } = useToast()

const outfit = computed(() => outfits.byId(props.id))
const items = computed(() => (outfit.value ? outfit.value.clothingIds.map((id) => wardrobe.byId(id)).filter(Boolean) : []))

function remove() {
  if (!outfit.value) return
  outfits.remove(outfit.value.id)
  show('코디를 삭제했어요')
  router.replace({ name: 'outfits' })
}

function placeToPlanner() {
  router.push({ name: 'planner', query: { placeOutfit: outfit.value.id } })
}
</script>

<template>
  <div class="page" v-if="outfit">
    <TopBar title="코디 상세" back />

    <div class="card" style="display: flex; flex-direction: column; gap: 14px">
      <div style="display: flex; gap: 10px">
        <ClothingThumb v-for="item in items" :key="item.id" :clothing="item" :size="80" />
      </div>
      <div>
        <p style="font-size: 19px; font-weight: 800">{{ outfit.name }}</p>
        <div style="display: flex; gap: 6px; margin-top: 6px">
          <span class="chip">{{ OUTFIT_SOURCE_LABEL[outfit.source] ?? outfit.source }}</span>
          <span v-if="outfit.aiScore != null" class="chip chip-muted">AI {{ outfit.aiScore }}점</span>
        </div>
      </div>

      <div v-if="outfit.requestText" class="detail-block">
        <p class="detail-label">입력한 상황</p>
        <p>{{ outfit.requestText }}</p>
      </div>
      <div v-if="outfit.aiReason" class="detail-block">
        <p class="detail-label">AI 추천 이유</p>
        <p>{{ outfit.aiReason }}</p>
      </div>
      <div v-if="outfit.aiComment" class="detail-block">
        <p class="detail-label">AI 코멘트</p>
        <p>{{ outfit.aiComment }}</p>
        <div v-if="outfit.aiTags?.length" style="display: flex; gap: 6px; margin-top: 6px; flex-wrap: wrap">
          <span v-for="t in outfit.aiTags" :key="t" class="chip chip-muted">#{{ t }}</span>
        </div>
      </div>
      <div v-if="outfit.memo" class="detail-block">
        <p class="detail-label">메모</p>
        <p>{{ outfit.memo }}</p>
      </div>

      <p class="hint-text">구성 의류</p>
      <ul style="list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px">
        <li v-for="item in items" :key="item.id" style="display: flex; justify-content: space-between; font-size: 13px">
          <span>{{ item.name }}</span>
          <span class="hint-text">{{ item.color }}</span>
        </li>
      </ul>
    </div>

    <button class="btn btn-secondary btn-block" type="button" @click="placeToPlanner">
      <Icon name="calendar" :size="16" /> 플래너에 배치하기
    </button>
    <button class="btn btn-danger btn-block" type="button" @click="remove">코디 삭제하기</button>
  </div>
  <div class="page" v-else>
    <TopBar title="코디 상세" back />
    <p class="hint-text">삭제되었거나 존재하지 않는 코디입니다.</p>
  </div>
</template>

<style scoped>
.detail-block {
  border-top: 1px solid var(--line);
  padding-top: 10px;
  font-size: 13px;
}

.detail-label {
  font-weight: 700;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 4px;
}
</style>
