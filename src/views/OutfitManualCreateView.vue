<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { useOutfitsStore } from '@/stores/outfits'
import ClothingCard from '@/components/ClothingCard.vue'
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
const selectedIds = ref([])
const name = ref('')
const memo = ref('')
const saving = ref(false)

const selectedItems = computed(() => selectedIds.value.map((id) => wardrobe.byId(id)).filter(Boolean))

function toggle(clothing) {
  const i = selectedIds.value.indexOf(clothing.id)
  if (i >= 0) {
    selectedIds.value.splice(i, 1)
  } else {
    selectedIds.value.push(clothing.id)
  }
}

function moveUp(index) {
  if (index === 0) return
  const arr = selectedIds.value
  ;[arr[index - 1], arr[index]] = [arr[index], arr[index - 1]]
}

function moveDown(index) {
  const arr = selectedIds.value
  if (index === arr.length - 1) return
  ;[arr[index + 1], arr[index]] = [arr[index], arr[index + 1]]
}

async function save() {
  if (!selectedIds.value.length || !name.value.trim()) return
  saving.value = true
  const outfit = outfits.add({
    ownerId: auth.currentUser.id,
    name: name.value.trim(),
    memo: memo.value.trim(),
    clothingIds: [...selectedIds.value],
    source: 'MANUAL',
  })
  saving.value = false
  show('코디를 저장했어요')
  router.replace({ name: 'outfit-detail', params: { id: outfit.id } })
}
</script>

<template>
  <div class="page">
    <TopBar title="직접 코디 만들기" back />

    <EmptyState v-if="myClothes.length === 0" icon="hanger" title="보유 의류가 없어요" description="옷을 먼저 등록해주세요." />

    <template v-else>
      <div v-if="selectedItems.length" class="card" style="display: flex; flex-direction: column; gap: 8px">
        <p class="section-title">선택한 의류 ({{ selectedItems.length }})</p>
        <div v-for="(item, i) in selectedItems" :key="item.id" style="display: flex; align-items: center; gap: 10px">
          <span class="hint-text" style="width: 16px">{{ i + 1 }}</span>
          <ClothingThumb :clothing="item" :size="44" />
          <span style="flex: 1; font-size: 13px">{{ item.name }}</span>
          <button class="btn btn-ghost btn-sm" type="button" @click="moveUp(i)">↑</button>
          <button class="btn btn-ghost btn-sm" type="button" @click="moveDown(i)">↓</button>
          <button class="btn btn-ghost btn-sm" type="button" @click="toggle(item)">✕</button>
        </div>
      </div>

      <p class="section-title">옷장에서 선택</p>
      <div class="grid">
        <ClothingCard
          v-for="c in myClothes"
          :key="c.id"
          :clothing="c"
          selectable
          :selected="selectedIds.includes(c.id)"
          @select="toggle"
        />
      </div>

      <div class="card" style="display: flex; flex-direction: column; gap: 10px; position: sticky; bottom: 0">
        <div class="field">
          <label>코디 이름</label>
          <input v-model="name" type="text" placeholder="예: 주말 나들이룩" />
        </div>
        <div class="field">
          <label>메모 (선택)</label>
          <textarea v-model="memo" rows="2"></textarea>
        </div>
        <button class="btn btn-primary" type="button" :disabled="!selectedIds.length || !name.trim() || saving" @click="save">
          <span v-if="saving" class="spinner"></span>
          <span>코디 저장하기</span>
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
</style>
