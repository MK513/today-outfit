<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { useOutfitsStore } from '@/stores/outfits'
import { recommendOutfit } from '@/lib/mockAi'
import { categoryLabel } from '@/lib/constants'
import ClothingThumb from '@/components/ClothingThumb.vue'
import EmptyState from '@/components/EmptyState.vue'
import TopBar from '@/components/TopBar.vue'
import { useToast } from '@/composables/useToast'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const outfits = useOutfitsStore()
const router = useRouter()
const { show } = useToast()

const myClothes = computed(() => wardrobe.byOwner(auth.currentUser.id))
const situation = ref('')
const loading = ref(false)
const errorMsg = ref('')
const result = ref(null)
const name = ref('')
const memo = ref('')
const saving = ref(false)

async function requestRecommend() {
  errorMsg.value = ''
  result.value = null
  if (!situation.value.trim()) {
    errorMsg.value = '상황을 입력해주세요.'
    return
  }
  loading.value = true
  try {
    const r = await recommendOutfit(situation.value, myClothes.value)
    result.value = r
    name.value = `${situation.value.trim().slice(0, 12)} 코디`
  } catch {
    errorMsg.value = '추천을 만드는 데 실패했어요. 다시 시도해주세요.'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!result.value || !name.value.trim()) return
  saving.value = true
  const outfit = outfits.add({
    ownerId: auth.currentUser.id,
    name: name.value.trim(),
    memo: memo.value.trim(),
    clothingIds: result.value.clothingIds,
    source: 'AI',
    requestText: situation.value.trim(),
    aiReason: result.value.aiReason,
  })
  saving.value = false
  show('AI 코디를 저장했어요')
  router.replace({ name: 'outfit-detail', params: { id: outfit.id } })
}
</script>

<template>
  <div class="page">
    <TopBar title="AI 코디 추천" back />

    <EmptyState v-if="myClothes.length === 0" icon="hanger" title="보유 의류가 없어요" description="옷을 먼저 등록해주세요." />

    <template v-else>
      <div class="card" style="display: flex; flex-direction: column; gap: 10px">
        <label class="field">
          <span style="font-size: 13px; font-weight: 600; color: var(--muted)">오늘의 상황을 알려주세요</span>
          <textarea v-model="situation" rows="3" placeholder="예: 친구와 카페에서 만나는 캐주얼한 자리"></textarea>
        </label>
        <button class="btn btn-primary" type="button" :disabled="!situation.trim() || loading" @click="requestRecommend">
          <span v-if="loading" class="spinner"></span>
          <span>AI 코디 추천받기</span>
        </button>
        <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>
      </div>

      <div v-if="result" class="card" style="display: flex; flex-direction: column; gap: 12px">
        <p class="section-title">추천 결과</p>
        <div style="display: flex; gap: 10px">
          <div v-for="(item, cat) in result.slots" :key="cat" style="display: flex; flex-direction: column; align-items: center; gap: 4px">
            <ClothingThumb :clothing="item" :size="76" />
            <span style="font-size: 11px; color: var(--muted)">{{ categoryLabel(cat) }}</span>
          </div>
        </div>
        <p class="hint-text">{{ result.aiReason }}</p>

        <div class="field">
          <label>코디 이름</label>
          <input v-model="name" type="text" />
        </div>
        <div class="field">
          <label>메모 (선택)</label>
          <textarea v-model="memo" rows="2" placeholder="메모를 남겨보세요"></textarea>
        </div>
        <button class="btn btn-primary" type="button" :disabled="!name.trim() || saving" @click="save">
          <span v-if="saving" class="spinner"></span>
          <span>이 코디 저장하기</span>
        </button>
      </div>
    </template>
  </div>
</template>
