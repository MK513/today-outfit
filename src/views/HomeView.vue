<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { useOutfitsStore } from '@/stores/outfits'
import { usePlannerStore } from '@/stores/planner'
import { OUTFIT_SLOT_CATEGORIES, categoryLabel, todayKey } from '@/lib/constants'
import { randomPickByCategory } from '@/lib/mockAi'
import ClothingThumb from '@/components/ClothingThumb.vue'
import TopBar from '@/components/TopBar.vue'
import Icon from '@/components/Icon.vue'
import { useToast } from '@/composables/useToast'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const outfits = useOutfitsStore()
const planner = usePlannerStore()
const router = useRouter()
const { show } = useToast()

const userId = computed(() => auth.currentUser.id)
const myClothes = computed(() => wardrobe.byOwner(userId.value))
const clothesCount = computed(() => myClothes.value.length)

const todaySchedule = ref(planner.findByDate(userId.value, todayKey()))
const todayOutfit = computed(() => (todaySchedule.value ? outfits.byId(todaySchedule.value.outfitId) : null))
const rerolling = ref(false)

function emptySlots() {
  return Object.fromEntries(OUTFIT_SLOT_CATEGORIES.map((cat) => [cat, null]))
}
function emptyLocks() {
  return Object.fromEntries(OUTFIT_SLOT_CATEGORIES.map((cat) => [cat, false]))
}

const slots = ref(emptySlots())
const locked = ref(emptyLocks())
const hasDrawn = computed(() => Object.values(slots.value).some((v) => v))
const canConfirm = computed(() => Object.values(slots.value).some((v) => v))

function draw() {
  for (const cat of OUTFIT_SLOT_CATEGORIES) {
    if (locked.value[cat] && slots.value[cat]) continue
    slots.value[cat] = randomPickByCategory(myClothes.value, cat, slots.value[cat]?.id)
  }
}

function toggleLock(cat) {
  if (!slots.value[cat]) return
  locked.value[cat] = !locked.value[cat]
}

function confirmOutfit() {
  const clothingIds = OUTFIT_SLOT_CATEGORIES.map((cat) => slots.value[cat]?.id).filter(Boolean)
  if (!clothingIds.length) return
  const d = new Date()
  const outfit = outfits.add({
    ownerId: userId.value,
    name: `오늘의 코디 ${d.getMonth() + 1}.${d.getDate()}`,
    clothingIds,
    source: 'RANDOM',
  })
  const result = planner.upsert({ ownerId: userId.value, planDate: todayKey(), outfitId: outfit.id })
  todaySchedule.value = result.schedule
  slots.value = emptySlots()
  locked.value = emptyLocks()
  show('오늘의 코디로 확정했어요')
}

function restart() {
  rerolling.value = true
  draw()
}
</script>

<template>
  <div class="page">
    <TopBar title="오늘 뭐 입지" settings />

    <div v-if="clothesCount === 0" class="card empty-state">
      <span class="icon-wrap"><Icon name="hanger" :size="20" :stroke-width="1.6" /></span>
      <p style="font-weight: 700; color: var(--ink)">아직 등록된 옷이 없어요</p>
      <p>옷을 등록하면 오늘의 픽을 받을 수 있어요.</p>
      <button class="btn btn-primary" type="button" @click="router.push({ name: 'wardrobe-add-manual' })">
        옷 추가하러 가기
      </button>
    </div>

    <template v-else-if="todayOutfit && !rerolling">
      <div class="card" style="display: flex; flex-direction: column; gap: 14px">
        <div class="section-head">
          <p class="section-title">오늘의 코디</p>
          <span class="chip">확정됨</span>
        </div>
        <div style="display: flex; gap: 10px">
          <ClothingThumb
            v-for="id in todayOutfit.clothingIds"
            :key="id"
            :clothing="wardrobe.byId(id)"
            :size="86"
          />
        </div>
        <p style="font-weight: 700">{{ todayOutfit.name }}</p>
        <div style="display: flex; gap: 8px">
          <button class="btn btn-secondary" type="button" @click="router.push({ name: 'outfit-detail', params: { id: todayOutfit.id } })">
            상세 보기
          </button>
          <button class="btn btn-ghost" type="button" @click="restart">다른 코디로 바꾸기</button>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="card" style="display: flex; flex-direction: column; gap: 16px">
        <div>
          <p class="section-title">오늘 뭐 입지?</p>
          <p class="hint-text">보유 의류 {{ clothesCount }}벌 중에서 골라드릴게요.</p>
        </div>

        <div v-if="!hasDrawn" class="empty-state" style="padding: 24px 8px">
          <span class="icon-wrap"><Icon name="dice-5" :size="20" :stroke-width="1.6" /></span>
          <p>AI 오늘의 픽을 받아보세요</p>
        </div>

        <div v-else style="display: flex; gap: 10px; justify-content: space-between">
          <div
            v-for="cat in OUTFIT_SLOT_CATEGORIES"
            :key="cat"
            style="display: flex; flex-direction: column; align-items: center; gap: 6px; flex: 1"
          >
            <div v-if="slots[cat]" style="position: relative">
              <ClothingThumb :clothing="slots[cat]" :size="88" />
              <button
                type="button"
                class="lock-btn"
                :class="{ locked: locked[cat] }"
                @click="toggleLock(cat)"
              >
                <Icon :name="locked[cat] ? 'lock' : 'lock-open'" :size="13" :stroke-width="2.2" />
              </button>
            </div>
            <div v-else class="card" style="width: 88px; height: 88px; display: flex; align-items: center; justify-content: center; color: var(--muted); font-size: 12px; text-align: center">
              {{ categoryLabel(cat) }} 없음
            </div>
            <span style="font-size: 12px; color: var(--muted)">{{ categoryLabel(cat) }}</span>
          </div>
        </div>

        <button class="btn btn-secondary" type="button" @click="draw">
          <Icon v-if="hasDrawn" name="dice-5" :size="16" />
          {{ hasDrawn ? '다시 돌리기 (잠금 제외)' : 'AI 오늘의 픽 받기' }}
        </button>
        <button class="btn btn-primary" type="button" :disabled="!canConfirm" @click="confirmOutfit">
          이걸로 입을래요
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.lock-btn {
  position: absolute;
  bottom: -6px;
  right: -6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid var(--surface);
  background: var(--line);
  color: var(--muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lock-btn.locked {
  background: var(--ink);
  color: var(--accent-ink);
}
</style>
