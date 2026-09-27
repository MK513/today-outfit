<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { useOutfitsStore } from '@/stores/outfits'
import { usePlannerStore } from '@/stores/planner'
import { recommendWeeklyOutfits } from '@/lib/mockAi'
import {
  startOfWeek,
  addDays,
  weekdayLabel,
  formatDateShort,
  todayKey,
  categoryLabel,
} from '@/lib/constants'
import ClothingThumb from '@/components/ClothingThumb.vue'
import OutfitCard from '@/components/OutfitCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import TopBar from '@/components/TopBar.vue'
import Icon from '@/components/Icon.vue'
import { useToast } from '@/composables/useToast'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const outfits = useOutfitsStore()
const planner = usePlannerStore()
const route = useRoute()
const router = useRouter()
const { show } = useToast()

const userId = computed(() => auth.currentUser.id)
const weekStart = ref(startOfWeek(todayKey()))
const days = computed(() => Array.from({ length: 7 }, (_, i) => addDays(weekStart.value, i)))

// 이번 주 일정. 주가 바뀌거나 배치를 바꾼 뒤 다시 불러온다.
async function loadWeek() {
  try {
    await planner.loadRange(weekStart.value, addDays(weekStart.value, 6))
  } catch (e) {
    show(e.message)
  }
}
watch(weekStart, loadWeek, { immediate: true })

// 코디 상세의 "플래너에 배치하기"로 들어온 배치 모드 (?placeOutfit=코디 id)
const placeOutfitId = ref(typeof route.query.placeOutfit === 'string' ? route.query.placeOutfit : null)
const placeOutfit = ref(null)

watch(
  () => route.query.placeOutfit,
  (v) => {
    placeOutfitId.value = typeof v === 'string' ? v : null
  },
)
watch(
  placeOutfitId,
  async (id) => {
    placeOutfit.value = null
    if (!id) return
    try {
      placeOutfit.value = await outfits.get(id)
    } catch (e) {
      show(e.message)
      cancelPlaceMode()
    }
  },
  { immediate: true },
)

function cancelPlaceMode() {
  placeOutfitId.value = null
  router.replace({ name: 'planner' })
}

function scheduleFor(dateKey) {
  return planner.findByDate(dateKey)
}

// 일정에 담긴 코디 요약 (thumbnails는 구성 순서 앞 3벌)
function outfitFor(dateKey) {
  return scheduleFor(dateKey)?.outfit ?? null
}

function prevWeek() {
  weekStart.value = addDays(weekStart.value, -7)
}
function nextWeek() {
  weekStart.value = addDays(weekStart.value, 7)
}
function thisWeek() {
  weekStart.value = startOfWeek(todayKey())
}

// --- 날짜 배치/교체/해제 ---
const pickerDate = ref(null)
const actionSheetDate = ref(null)

function onDayClick(dateKey) {
  if (placeOutfitId.value) {
    doPlaceOutfit(dateKey)
    return
  }
  const s = scheduleFor(dateKey)
  if (s) {
    actionSheetDate.value = dateKey
  } else {
    pickerDate.value = dateKey
  }
}

async function placeAndReload(dateKey, outfitId) {
  try {
    const { created } = await planner.upsert(dateKey, outfitId)
    show(created ? '코디를 배치했어요' : '코디를 교체했어요')
  } catch (e) {
    show(e.message)
  }
  await loadWeek()
}

async function doPlaceOutfit(dateKey) {
  const outfitId = placeOutfitId.value
  cancelPlaceMode()
  await placeAndReload(dateKey, outfitId)
}

async function choosePickerOutfit(outfitId) {
  const dateKey = pickerDate.value
  pickerDate.value = null
  await placeAndReload(dateKey, outfitId)
}

function replaceFromSheet() {
  pickerDate.value = actionSheetDate.value
  actionSheetDate.value = null
}

async function unscheduleFromSheet() {
  const dateKey = actionSheetDate.value
  actionSheetDate.value = null
  try {
    await planner.remove(dateKey)
    show('배치를 해제했어요')
  } catch (e) {
    show(e.message)
  }
  await loadWeek()
}

// 코디 선택 시트를 열 때마다 최신 코디 목록을 불러온다.
const savedOutfits = ref([])
watch(pickerDate, async (dateKey) => {
  if (!dateKey) return
  try {
    savedOutfits.value = await outfits.list()
  } catch (e) {
    show(e.message)
  }
})

// --- AI 주간 추천 ---
const showAiPanel = ref(false)
const aiSituation = ref('')
const aiStartDate = ref(weekStart.value)
const aiDays = ref(7)
const aiLoading = ref(false)
const aiError = ref('')
const aiResults = ref([])
const aiSaving = ref(false)

const myClothes = computed(() => wardrobe.byOwner(userId.value))

async function requestWeekly() {
  aiError.value = ''
  aiResults.value = []
  if (!aiSituation.value.trim()) {
    aiError.value = '상황을 입력해주세요.'
    return
  }
  aiLoading.value = true
  try {
    const r = await recommendWeeklyOutfits(aiSituation.value, aiStartDate.value, Number(aiDays.value), myClothes.value)
    if (!r.length) {
      aiError.value = '보유 의류가 부족해 추천을 만들 수 없어요.'
    } else {
      aiResults.value = r
      if (r.length < Number(aiDays.value)) {
        aiError.value = `보유 의류가 부족해 ${r.length}일치만 생성했어요.`
      }
    }
  } catch {
    aiError.value = '추천을 만드는 데 실패했어요.'
  } finally {
    aiLoading.value = false
  }
}

// 날짜별 코디 생성 + 배치를 서버가 한 트랜잭션으로 처리한다 (이름 "AI 주간 코디 M.D"는 서버가 붙임).
async function saveWeeklyResults() {
  aiSaving.value = true
  aiError.value = ''
  try {
    const saved = await planner.saveWeekly({
      requestText: aiSituation.value.trim(),
      days: aiResults.value.map((day) => ({
        planDate: addDays(aiStartDate.value, day.dayOffset),
        clothingIds: day.clothingIds,
        aiReason: day.aiReason,
      })),
    })
    show(`${saved.length}일치 코디를 플래너에 저장했어요`)
    aiResults.value = []
    aiSituation.value = ''
    showAiPanel.value = false
    await loadWeek()
  } catch (e) {
    aiError.value = e.message
  } finally {
    aiSaving.value = false
  }
}
</script>

<template>
  <div class="page">
    <TopBar title="주간 플래너" settings />

    <div v-if="placeOutfit" class="card" style="background: var(--ink); border-color: var(--ink); display: flex; flex-direction: column; gap: 8px">
      <p style="font-weight: 700; color: var(--accent-ink)">'{{ placeOutfit.name }}' 코디를 배치할 날짜를 선택하세요</p>
      <button class="link-btn" type="button" style="align-self: flex-start; color: var(--accent-ink)" @click="cancelPlaceMode">취소</button>
    </div>

    <div class="section-head">
      <button class="btn btn-ghost btn-sm" type="button" @click="prevWeek"><Icon name="chevron-left" :size="14" /> 이전 주</button>
      <button class="link-btn" type="button" @click="thisWeek">이번 주</button>
      <button class="btn btn-ghost btn-sm" type="button" @click="nextWeek">다음 주 <Icon name="chevron-right" :size="14" /></button>
    </div>

    <div style="display: flex; flex-direction: column; gap: 8px">
      <button
        v-for="d in days"
        :key="d"
        type="button"
        class="day-row"
        :class="{ today: d === todayKey() }"
        @click="onDayClick(d)"
      >
        <div class="day-label">
          <span class="weekday">{{ weekdayLabel(d) }}</span>
          <span class="date">{{ formatDateShort(d) }}</span>
        </div>
        <template v-if="outfitFor(d)">
          <ClothingThumb :clothing="outfitFor(d).thumbnails[0] ?? null" :size="46" />
          <span class="outfit-name">{{ outfitFor(d).name }}</span>
        </template>
        <span v-else class="empty-label">+ 코디 배치하기</span>
      </button>
    </div>

    <div class="card" style="display: flex; flex-direction: column; gap: 10px">
      <button class="link-btn" type="button" style="align-self: flex-start" @click="showAiPanel = !showAiPanel">
        <Icon :name="showAiPanel ? 'chevron-up' : 'chevron-down'" :size="14" />
        {{ showAiPanel ? 'AI 주간 코디 추천 닫기' : 'AI 주간 코디 추천 열기' }}
      </button>

      <template v-if="showAiPanel">
        <div class="field">
          <label>상황</label>
          <textarea v-model="aiSituation" rows="2" placeholder="예: 다음 주는 회사 출근이 많아요"></textarea>
        </div>
        <div style="display: flex; gap: 8px">
          <div class="field" style="flex: 1">
            <label>시작일</label>
            <input v-model="aiStartDate" type="date" />
          </div>
          <div class="field" style="width: 90px">
            <label>일수</label>
            <input v-model="aiDays" type="number" min="1" max="7" />
          </div>
        </div>
        <button class="btn btn-primary" type="button" :disabled="aiLoading" @click="requestWeekly">
          <span v-if="aiLoading" class="spinner"></span>
          <span>주간 추천 요청</span>
        </button>
        <p v-if="aiError" class="error-text">{{ aiError }}</p>

        <div v-if="aiResults.length" style="display: flex; flex-direction: column; gap: 8px">
          <div v-for="day in aiResults" :key="day.dayOffset" class="card" style="display: flex; gap: 10px; align-items: center">
            <span class="hint-text" style="width: 60px">{{ formatDateShort(addDays(aiStartDate, day.dayOffset)) }}</span>
            <div style="display: flex; gap: 6px">
              <ClothingThumb v-for="(item, cat) in day.slots" :key="cat" :clothing="item" :size="40" />
            </div>
          </div>
          <button class="btn btn-primary" type="button" :disabled="aiSaving" @click="saveWeeklyResults">
            <span v-if="aiSaving" class="spinner"></span>
            <span>플래너에 전체 저장</span>
          </button>
        </div>
      </template>
    </div>

    <!-- 날짜에 배치할 저장 코디 선택 -->
    <transition name="fade">
      <div v-if="pickerDate" class="sheet-backdrop" @click.self="pickerDate = null">
        <div class="sheet">
          <p class="section-title" style="margin-bottom: 10px">{{ formatDateShort(pickerDate) }}에 배치할 코디 선택</p>
          <EmptyState v-if="!savedOutfits.length" icon="stack-2" title="저장된 코디가 없어요" />
          <div v-else style="display: flex; flex-direction: column; gap: 8px; max-height: 50vh; overflow-y: auto">
            <OutfitCard
              v-for="o in savedOutfits"
              :key="o.id"
              :outfit="o"
              :clickable="false"
              @click="choosePickerOutfit(o.id)"
            />
          </div>
          <button class="btn btn-ghost btn-block" type="button" style="margin-top: 10px" @click="pickerDate = null">취소</button>
        </div>
      </div>
    </transition>

    <!-- 배치된 날짜 액션 -->
    <transition name="fade">
      <div v-if="actionSheetDate" class="sheet-backdrop" @click.self="actionSheetDate = null">
        <div class="sheet">
          <p class="section-title" style="margin-bottom: 10px">{{ formatDateShort(actionSheetDate) }} 배치 변경</p>
          <button
            class="btn btn-secondary btn-block"
            type="button"
            style="margin-bottom: 8px"
            @click="router.push({ name: 'outfit-detail', params: { id: outfitFor(actionSheetDate).id } }); actionSheetDate = null"
          >
            상세 보기
          </button>
          <button class="btn btn-secondary btn-block" type="button" style="margin-bottom: 8px" @click="replaceFromSheet">
            다른 코디로 교체
          </button>
          <button class="btn btn-danger btn-block" type="button" style="margin-bottom: 8px" @click="unscheduleFromSheet">
            배치 해제
          </button>
          <button class="btn btn-ghost btn-block" type="button" @click="actionSheetDate = null">닫기</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.day-row {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  cursor: pointer;
  text-align: left;
}

.day-row.today {
  border-color: var(--accent);
}

.day-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 36px;
  flex-shrink: 0;
}

.weekday {
  font-size: 12px;
  font-weight: 700;
  color: var(--muted);
}

.date {
  font-size: 11px;
  color: var(--muted);
}

.outfit-name {
  font-size: 13px;
  font-weight: 600;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-label {
  color: var(--muted);
  font-size: 13px;
}

.sheet-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(19, 19, 19, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 150;
}

.sheet {
  width: 100%;
  max-width: var(--shell-width);
  background: var(--surface);
  border-radius: 14px 14px 0 0;
  padding: 20px 18px calc(env(safe-area-inset-bottom, 0px) + 20px);
  max-height: 80vh;
  overflow-y: auto;
}
</style>
