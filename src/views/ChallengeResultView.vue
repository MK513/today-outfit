<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { useOutfitsStore } from '@/stores/outfits'
import { useChallengeStore } from '@/stores/challenge'
import { evaluateChallengeOutfit } from '@/lib/mockAi'
import ClothingThumb from '@/components/ClothingThumb.vue'
import TopBar from '@/components/TopBar.vue'
import { useToast } from '@/composables/useToast'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const outfits = useOutfitsStore()
const challenge = useChallengeStore()
const router = useRouter()
const { show } = useToast()

const items = computed(() => challenge.chosenClothingIds.map((id) => wardrobe.byId(id)).filter(Boolean))
const loading = ref(true)
const failed = ref(false)
const evalResult = ref(null)
const name = ref('챌린지 코디')
const saving = ref(false)

onMounted(async () => {
  try {
    evalResult.value = await evaluateChallengeOutfit(challenge.chosenClothingIds)
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
})

function save() {
  if (!name.value.trim()) return
  saving.value = true
  const outfit = outfits.add({
    ownerId: auth.currentUser.id,
    name: name.value.trim(),
    clothingIds: [...challenge.chosenClothingIds],
    source: 'CHALLENGE',
    aiScore: evalResult.value?.score ?? null,
    aiComment: evalResult.value?.comment ?? null,
    aiTags: evalResult.value?.tags ?? null,
  })
  saving.value = false
  challenge.reset()
  show('챌린지 코디를 저장했어요')
  router.replace({ name: 'outfit-detail', params: { id: outfit.id } })
}

function retry() {
  challenge.reset()
  router.replace({ name: 'challenge' })
}
</script>

<template>
  <div class="page">
    <TopBar title="챌린지 결과" back />

    <div v-if="items.length" class="card" style="display: flex; gap: 10px; justify-content: center">
      <ClothingThumb v-for="item in items" :key="item.id" :clothing="item" :size="64" />
    </div>

    <div v-if="loading" class="card" style="display: flex; align-items: center; gap: 10px; justify-content: center; padding: 30px">
      <span class="spinner"></span>
      <span class="hint-text">AI가 코디를 평가하고 있어요...</span>
    </div>

    <div v-else class="card" style="display: flex; flex-direction: column; gap: 12px">
      <template v-if="failed">
        <p class="error-text">평가에 실패했어요. 점수 없이도 저장할 수 있어요.</p>
      </template>
      <template v-else-if="evalResult">
        <div style="text-align: center">
          <p style="font-size: 34px; font-weight: 800; color: var(--accent)">{{ evalResult.score }}점</p>
          <p class="hint-text">{{ evalResult.comment }}</p>
        </div>
        <div style="display: flex; gap: 6px; justify-content: center; flex-wrap: wrap">
          <span v-for="t in evalResult.tags" :key="t" class="chip">#{{ t }}</span>
        </div>
      </template>

      <div class="field">
        <label>코디 이름</label>
        <input v-model="name" type="text" />
      </div>
      <button class="btn btn-primary" type="button" :disabled="!name.trim() || saving" @click="save">
        <span v-if="saving" class="spinner"></span>
        <span>코디로 저장하기</span>
      </button>
      <button class="btn btn-ghost" type="button" @click="retry">다시 도전하기</button>
    </div>
  </div>
</template>