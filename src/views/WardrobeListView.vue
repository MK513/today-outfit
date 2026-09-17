<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { CATEGORIES, SEASONS, COLORS } from '@/lib/constants'
import ClothingCard from '@/components/ClothingCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import TopBar from '@/components/TopBar.vue'
import Icon from '@/components/Icon.vue'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const router = useRouter()

const category = ref('')
const season = ref('')
const color = ref('')
const showAddSheet = ref(false)

const list = computed(() =>
  wardrobe.filtered(auth.currentUser.id, {
    category: category.value || undefined,
    season: season.value || undefined,
    color: color.value || undefined,
  }),
)

const hasFilter = computed(() => category.value || season.value || color.value)

function resetFilters() {
  category.value = ''
  season.value = ''
  color.value = ''
}

function goAdd(mode) {
  showAddSheet.value = false
  router.push({ name: `wardrobe-add-${mode}` })
}
</script>

<template>
  <div class="page">
    <TopBar title="내 옷장" settings />

    <div class="filters">
      <select v-model="category">
        <option value="">전체 카테고리</option>
        <option v-for="c in CATEGORIES" :key="c.value" :value="c.value">{{ c.label }}</option>
      </select>
      <select v-model="season">
        <option value="">전체 계절</option>
        <option v-for="s in SEASONS" :key="s.value" :value="s.value">{{ s.label }}</option>
      </select>
      <select v-model="color">
        <option value="">전체 색상</option>
        <option v-for="c in COLORS" :key="c" :value="c">{{ c }}</option>
      </select>
    </div>
    <button v-if="hasFilter" class="link-btn" type="button" style="align-self: flex-start" @click="resetFilters">
      필터 초기화
    </button>

    <EmptyState
      v-if="list.length === 0"
      icon="hanger"
      :title="hasFilter ? '조건에 맞는 옷이 없어요' : '아직 등록된 옷이 없어요'"
      description="사진, 문장, 직접 입력으로 옷을 등록해보세요."
    />
    <div v-else class="grid">
      <ClothingCard v-for="c in list" :key="c.id" :clothing="c" />
    </div>

    <button class="fab" type="button" aria-label="옷 추가" @click="showAddSheet = true">
      <Icon name="plus" :size="24" :stroke-width="1.8" />
    </button>

    <transition name="fade">
      <div v-if="showAddSheet" class="sheet-backdrop" @click.self="showAddSheet = false">
        <div class="sheet">
          <p class="section-title" style="margin-bottom: 10px">옷 등록 방법 선택</p>
          <button class="btn btn-secondary btn-block" type="button" style="margin-bottom: 8px" @click="goAdd('photo')">
            <Icon name="camera" :size="17" /> 사진으로 등록
          </button>
          <button class="btn btn-secondary btn-block" type="button" style="margin-bottom: 8px" @click="goAdd('text')">
            <Icon name="pencil" :size="17" /> 문장으로 여러 벌 등록
          </button>
          <button class="btn btn-secondary btn-block" type="button" style="margin-bottom: 8px" @click="goAdd('manual')">
            <Icon name="keyboard" :size="17" /> 직접 입력으로 등록
          </button>
          <button class="btn btn-ghost btn-block" type="button" @click="showAddSheet = false">취소</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  gap: 8px;
}

.filters select {
  flex: 1;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  padding: 9px 6px;
  background: var(--surface);
  color: var(--ink);
  font-size: 12px;
  min-width: 0;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.fab {
  position: fixed;
  bottom: 96px;
  left: 50%;
  transform: translateX(calc(var(--shell-width) / 2 - 70px));
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  box-shadow: 0 6px 16px rgba(19, 19, 19, 0.28);
  cursor: pointer;
  z-index: 100;
}

@media (max-width: 460px) {
  .fab {
    left: auto;
    right: 18px;
    transform: none;
  }
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
}
</style>
