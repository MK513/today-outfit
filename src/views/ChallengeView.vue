<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { useChallengeStore } from '@/stores/challenge'
import { CHALLENGE_ORDER, categoryLabel } from '@/lib/constants'
import ClothingThumb from '@/components/ClothingThumb.vue'
import EmptyState from '@/components/EmptyState.vue'
import TopBar from '@/components/TopBar.vue'
import Icon from '@/components/Icon.vue'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const challenge = useChallengeStore()
const router = useRouter()

const myClothes = computed(() => wardrobe.byOwner(auth.currentUser.id))

function shuffle(arr) {
  return [...arr].sort(() => Math.random() - 0.5)
}

function buildRounds() {
  const rounds = []
  for (const cat of CHALLENGE_ORDER) {
    const pool = shuffle(myClothes.value.filter((c) => c.category === cat))
    if (pool.length < 2) continue
    rounds.push({ category: cat, left: pool[0], right: pool[1] })
  }
  return rounds
}

const started = ref(false)
const currentIndex = ref(0)
const currentRounds = ref([])

const skippedCount = computed(() => CHALLENGE_ORDER.length - currentRounds.value.length)
const currentRound = computed(() => currentRounds.value[currentIndex.value])
const canStart = computed(() => buildRounds().length > 0)

function begin() {
  const rounds = buildRounds()
  currentRounds.value = rounds
  currentIndex.value = 0
  challenge.start(rounds)
  started.value = true
}

function choose(side) {
  const item = side === 'left' ? currentRound.value.left : currentRound.value.right
  challenge.choose(item.id)
  if (currentIndex.value + 1 < currentRounds.value.length) {
    currentIndex.value += 1
  } else {
    router.push({ name: 'challenge-result' })
  }
}
</script>

<template>
  <div class="page">
    <TopBar title="코디 챌린지" back />

    <EmptyState v-if="!canStart" icon="dice-5" title="챌린지를 시작할 수 없어요" description="한 부위에 2벌 이상의 옷이 있어야 챌린지를 진행할 수 있어요." />

    <div v-else-if="!started" class="card" style="display: flex; flex-direction: column; gap: 12px; align-items: center; text-align: center; padding: 30px 18px">
      <span class="icon-wrap" style="width: 56px; height: 56px"><Icon name="dice-5" :size="26" :stroke-width="1.4" /></span>
      <p class="section-title">좌 · 우 중 마음에 드는 옷을 골라보세요</p>
      <p class="hint-text">상의 → 하의 → 신발 → 모자 → 액세서리 순서로 진행돼요. 후보가 부족한 부위는 건너뛰어요.</p>
      <button class="btn btn-primary btn-block" type="button" @click="begin">챌린지 시작하기</button>
    </div>

    <template v-else-if="currentRound">
      <div class="section-head">
        <span class="chip">{{ categoryLabel(currentRound.category) }}</span>
        <span class="hint-text">{{ currentIndex + 1 }} / {{ currentRounds.length }}</span>
        <label style="display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--muted)">
          <input v-model="challenge.blurMode" type="checkbox" /> 가리고 고르기
        </label>
      </div>

      <div style="display: flex; gap: 12px">
        <button type="button" class="pick-card" @click="choose('left')">
          <ClothingThumb :clothing="currentRound.left" :size="140" :class="{ blur: challenge.blurMode }" />
          <p>{{ challenge.blurMode ? '???' : currentRound.left.name }}</p>
        </button>
        <button type="button" class="pick-card" @click="choose('right')">
          <ClothingThumb :clothing="currentRound.right" :size="140" :class="{ blur: challenge.blurMode }" />
          <p>{{ challenge.blurMode ? '???' : currentRound.right.name }}</p>
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.pick-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 14px;
  cursor: pointer;
}

.pick-card p {
  font-size: 12px;
  font-weight: 600;
  text-align: center;
}

.pick-card :deep(.thumb) {
  width: 100% !important;
  height: auto !important;
  aspect-ratio: 1 / 1;
}

.pick-card :deep(.blur) {
  filter: blur(14px);
}
</style>
