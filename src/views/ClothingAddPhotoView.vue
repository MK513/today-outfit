<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { analyzePhoto } from '@/lib/mockAi'
import { CATEGORIES, SEASONS, COLORS } from '@/lib/constants'
import TopBar from '@/components/TopBar.vue'
import Icon from '@/components/Icon.vue'
import { useToast } from '@/composables/useToast'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const router = useRouter()
const { show } = useToast()

const fileInput = ref(null)
const previewUrl = ref('')
const fileObj = ref(null)
const analyzing = ref(false)
const saving = ref(false)
const errorMsg = ref('')
const candidate = ref(null)

const MAX_SIZE = 5 * 1024 * 1024

function pickFile() {
  fileInput.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  errorMsg.value = ''
  candidate.value = null
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    errorMsg.value = '5MB 이하의 JPG 또는 PNG 파일만 업로드할 수 있어요.'
    return
  }
  if (file.size > MAX_SIZE) {
    errorMsg.value = '5MB 이하의 JPG 또는 PNG 파일만 업로드할 수 있어요.'
    return
  }
  fileObj.value = file
  const reader = new FileReader()
  reader.onload = () => {
    previewUrl.value = reader.result
  }
  reader.readAsDataURL(file)
  runAnalysis(file)
}

async function runAnalysis(file) {
  analyzing.value = true
  errorMsg.value = ''
  try {
    const result = await analyzePhoto(file)
    candidate.value = result
  } catch {
    errorMsg.value = 'AI 분석에 실패했어요. 수동 등록으로 이동해주세요.'
  } finally {
    analyzing.value = false
  }
}

function goManual() {
  router.replace({ name: 'wardrobe-add-manual' })
}

async function save() {
  if (!candidate.value) return
  saving.value = true
  wardrobe.add({
    ownerId: auth.currentUser.id,
    name: candidate.value.name,
    category: candidate.value.category,
    color: candidate.value.color,
    season: candidate.value.season,
    fileUrl: previewUrl.value,
    fileName: fileObj.value?.name ?? candidate.value.fileName,
    source: 'PHOTO',
  })
  saving.value = false
  show('의류를 등록했어요')
  router.replace({ name: 'wardrobe' })
}
</script>

<template>
  <div class="page">
    <TopBar title="사진으로 등록" back />

    <input ref="fileInput" type="file" accept="image/jpeg,image/png" style="display: none" @change="onFileChange" />

    <div v-if="!previewUrl" class="card empty-state" style="cursor: pointer" @click="pickFile">
      <span class="icon-wrap"><Icon name="camera" :size="20" :stroke-width="1.6" /></span>
      <p style="font-weight: 700; color: var(--ink)">의류 사진을 선택해주세요</p>
      <p>JPG 또는 PNG, 5MB 이하</p>
      <button class="btn btn-primary" type="button" @click.stop="pickFile">사진 선택하기</button>
    </div>

    <template v-else>
      <div class="card" style="display: flex; gap: 12px; align-items: center">
        <img :src="previewUrl" alt="선택한 사진" style="width: 96px; height: 96px; object-fit: cover; border-radius: 12px" />
        <button class="link-btn" type="button" @click="pickFile">다른 사진 선택</button>
      </div>

      <div v-if="analyzing" class="card" style="display: flex; align-items: center; gap: 10px; justify-content: center; padding: 26px">
        <span class="spinner"></span>
        <span class="hint-text">AI가 사진을 분석하고 있어요...</span>
      </div>

      <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>
      <button v-if="errorMsg" class="btn btn-secondary" type="button" @click="goManual">수동 등록으로 이동</button>

      <div v-if="candidate && !analyzing" class="card" style="display: flex; flex-direction: column; gap: 12px">
        <p class="section-title">AI 분석 결과 확인 및 수정</p>
        <div class="field">
          <label>이름</label>
          <input v-model="candidate.name" type="text" />
        </div>
        <div class="field">
          <label>카테고리</label>
          <select v-model="candidate.category">
            <option v-for="c in CATEGORIES" :key="c.value" :value="c.value">{{ c.label }}</option>
          </select>
        </div>
        <div class="field">
          <label>색상</label>
          <select v-model="candidate.color">
            <option v-for="c in COLORS" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
        <div class="field">
          <label>계절</label>
          <select v-model="candidate.season">
            <option v-for="s in SEASONS" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
        <button class="btn btn-primary" type="button" :disabled="saving" @click="save">
          <span v-if="saving" class="spinner"></span>
          <span>이 정보로 저장</span>
        </button>
      </div>
    </template>
  </div>
</template>
