<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOutfitsStore } from '@/stores/outfits'
import { useToast } from '@/composables/useToast'
import OutfitCard from '@/components/OutfitCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import TopBar from '@/components/TopBar.vue'
import Icon from '@/components/Icon.vue'

const outfits = useOutfitsStore()
const router = useRouter()
const { show } = useToast()

const list = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    list.value = await outfits.list()
  } catch (e) {
    show(e.message)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <TopBar title="내 코디" settings />

    <div style="display: flex; gap: 8px">
      <button class="btn btn-primary" type="button" style="flex: 1" @click="router.push({ name: 'outfits-ai' })">
        <Icon name="sparkles" :size="16" /> AI 추천
      </button>
      <button class="btn btn-secondary" type="button" style="flex: 1" @click="router.push({ name: 'outfits-manual-create' })">
        <Icon name="palette" :size="16" /> 직접 만들기
      </button>
    </div>

    <p v-if="loading" class="hint-text">불러오는 중…</p>
    <EmptyState
      v-else-if="list.length === 0"
      icon="stack-2"
      title="저장된 코디가 없어요"
      description="AI 추천을 받거나 직접 코디를 만들어보세요."
    />
    <div v-else style="display: flex; flex-direction: column; gap: 10px">
      <OutfitCard v-for="o in list" :key="o.id" :outfit="o" />
    </div>
  </div>
</template>
