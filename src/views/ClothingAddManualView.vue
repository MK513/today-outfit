<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { CATEGORIES, SEASONS, COLORS } from '@/lib/constants'
import TopBar from '@/components/TopBar.vue'
import Icon from '@/components/Icon.vue'
import { useToast } from '@/composables/useToast'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const router = useRouter()
const { show } = useToast()

const form = ref({ name: '', category: 'TOP', color: COLORS[0], season: 'ALL' })
const fileInput = ref(null)
const previewUrl = ref('')
const fileObj = ref(null)
const saving = ref(false)

function pickFile() {
  fileInput.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  fileObj.value = file
  const reader = new FileReader()
  reader.onload = () => {
    previewUrl.value = reader.result
  }
  reader.readAsDataURL(file)
}

async function save() {
  if (!form.value.name.trim()) return
  saving.value = true
  try {
    const image = fileObj.value ? await wardrobe.uploadImage(fileObj.value) : null
    await wardrobe.add({
      ownerId: auth.currentUser.id,
      name: form.value.name.trim(),
      category: form.value.category,
      color: form.value.color,
      season: form.value.season,
      imageUrl: image?.imageUrl ?? null,
      imageFileName: image?.imageFileName ?? null,
      source: 'MANUAL',
    })
    show('의류를 등록했어요')
    router.replace({ name: 'wardrobe' })
  } catch (e) {
    show(e.message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <TopBar title="직접 입력으로 등록" back />

    <div class="card" style="display: flex; flex-direction: column; gap: 14px">
      <input ref="fileInput" type="file" accept="image/*" style="display: none" @change="onFileChange" />
      <div style="display: flex; align-items: center; gap: 12px">
        <div
          style="width: 72px; height: 72px; border-radius: 12px; background: var(--accent-soft); display: flex; align-items: center; justify-content: center; overflow: hidden; cursor: pointer"
          @click="pickFile"
        >
          <img v-if="previewUrl" :src="previewUrl" alt="미리보기" style="width: 100%; height: 100%; object-fit: cover" />
          <Icon v-else name="camera" :size="22" :stroke-width="1.6" style="color: var(--muted)" />
        </div>
        <button class="link-btn" type="button" @click="pickFile">사진 추가 (선택)</button>
      </div>

      <div class="field">
        <label>이름</label>
        <input v-model="form.name" type="text" placeholder="예: 블랙 슬랙스" />
      </div>
      <div class="field">
        <label>카테고리</label>
        <select v-model="form.category">
          <option v-for="c in CATEGORIES" :key="c.value" :value="c.value">{{ c.label }}</option>
        </select>
      </div>
      <div class="field">
        <label>색상</label>
        <select v-model="form.color">
          <option v-for="c in COLORS" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>
      <div class="field">
        <label>계절</label>
        <select v-model="form.season">
          <option v-for="s in SEASONS" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
      </div>
      <button class="btn btn-primary" type="button" :disabled="!form.name.trim() || saving" @click="save">
        <span v-if="saving" class="spinner"></span>
        <span>등록하기</span>
      </button>
    </div>
  </div>
</template>
