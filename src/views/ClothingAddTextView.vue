<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { parseClothesSentence } from '@/lib/mockAi'
import { CATEGORIES, SEASONS, COLORS } from '@/lib/constants'
import TopBar from '@/components/TopBar.vue'
import { useToast } from '@/composables/useToast'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const router = useRouter()
const { show } = useToast()

const sentence = ref('')
const parsing = ref(false)
const saving = ref(false)
const errorMsg = ref('')
const candidates = ref([])

async function analyze() {
  errorMsg.value = ''
  candidates.value = []
  parsing.value = true
  try {
    const result = await parseClothesSentence(sentence.value)
    candidates.value = result
  } catch {
    errorMsg.value = 'AI 파싱에 실패했어요. 수동 등록으로 이동해주세요.'
  } finally {
    parsing.value = false
  }
}

function removeCandidate(index) {
  candidates.value.splice(index, 1)
}

function goManual() {
  router.replace({ name: 'wardrobe-add-manual' })
}

async function saveAll() {
  if (!candidates.value.length) return
  saving.value = true
  errorMsg.value = ''
  try {
    const created = await wardrobe.addBulk(
      auth.currentUser.id,
      candidates.value.map((c) => ({
        name: c.name,
        category: c.category,
        color: c.color,
        season: c.season,
        source: 'TEXT',
      })),
    )
    show(`${created.length}벌을 등록했어요`)
    router.replace({ name: 'wardrobe' })
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <TopBar title="문장으로 등록" back />

    <div class="card" style="display: flex; flex-direction: column; gap: 10px">
      <p class="hint-text">여러 벌의 옷을 한 문장으로 설명해주세요. 예: "베이지 니트, 블루 데님 팬츠, 화이트 스니커즈"</p>
      <textarea v-model="sentence" placeholder="가지고 있는 옷을 문장으로 설명해주세요" rows="4"></textarea>
      <button class="btn btn-primary" type="button" :disabled="!sentence.trim() || parsing" @click="analyze">
        <span v-if="parsing" class="spinner"></span>
        <span>AI로 분석하기</span>
      </button>
    </div>

    <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>
    <button v-if="errorMsg" class="btn btn-secondary" type="button" @click="goManual">수동 등록으로 이동</button>

    <template v-if="candidates.length">
      <div class="section-head">
        <p class="section-title">후보 {{ candidates.length }}건</p>
        <span class="hint-text">수정 또는 삭제 후 저장하세요</span>
      </div>
      <div
        v-for="(c, i) in candidates"
        :key="i"
        class="card"
        style="display: flex; flex-direction: column; gap: 8px"
      >
        <div class="section-head">
          <input v-model="c.name" type="text" style="border: none; font-weight: 700; font-size: 14px; flex: 1; outline: none" />
          <button class="link-btn" type="button" style="color: var(--danger)" @click="removeCandidate(i)">삭제</button>
        </div>
        <div style="display: flex; gap: 8px">
          <select v-model="c.category" style="flex: 1; border: 1px solid var(--line); border-radius: 8px; padding: 6px">
            <option v-for="cat in CATEGORIES" :key="cat.value" :value="cat.value">{{ cat.label }}</option>
          </select>
          <select v-model="c.color" style="flex: 1; border: 1px solid var(--line); border-radius: 8px; padding: 6px">
            <option v-for="col in COLORS" :key="col" :value="col">{{ col }}</option>
          </select>
          <select v-model="c.season" style="flex: 1; border: 1px solid var(--line); border-radius: 8px; padding: 6px">
            <option v-for="s in SEASONS" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
      </div>
      <button class="btn btn-primary" type="button" :disabled="saving" @click="saveAll">
        <span v-if="saving" class="spinner"></span>
        <span>{{ candidates.length }}벌 일괄 저장</span>
      </button>
    </template>
  </div>
</template>
