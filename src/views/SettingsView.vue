<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { useOutfitsStore } from '@/stores/outfits'
import { usePlannerStore } from '@/stores/planner'
import TopBar from '@/components/TopBar.vue'
import { useToast } from '@/composables/useToast'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const outfits = useOutfitsStore()
const planner = usePlannerStore()
const router = useRouter()
const { show } = useToast()

const step = ref('root') // root | confirm | done
const password = ref('')
const error = ref('')
const submitting = ref(false)

async function logout() {
  await auth.logout()
  router.replace({ name: 'login' })
}

function goConfirm() {
  step.value = 'confirm'
  error.value = ''
  password.value = ''
}

async function withdraw() {
  error.value = ''
  submitting.value = true
  const result = await auth.withdraw(password.value)
  submitting.value = false
  if (!result.ok) {
    error.value = result.message
    return
  }
  wardrobe.purgeOwner(result.withdrawnUserId)
  outfits.purgeOwner(result.withdrawnUserId)
  planner.purgeOwner(result.withdrawnUserId)
  show('계정 탈퇴가 완료되었습니다')
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="page">
    <TopBar title="설정" back />

    <div class="card">
      <p style="font-weight: 700; margin-bottom: 6px">{{ auth.currentUser?.name }}</p>
      <p class="hint-text">{{ auth.currentUser?.email }}</p>
    </div>

    <button class="btn btn-ghost btn-block" type="button" @click="logout">로그아웃</button>

    <template v-if="step === 'root'">
      <button class="btn btn-danger btn-block" type="button" @click="goConfirm">계정 탈퇴</button>
    </template>

    <div v-else-if="step === 'confirm'" class="card" style="display: flex; flex-direction: column; gap: 12px">
      <p class="section-title" style="color: var(--danger)">정말 탈퇴하시겠어요?</p>
      <p class="hint-text">
        탈퇴 시 등록한 의류 사진, 의류 정보, 저장된 코디, 플래너 배치 등 모든 개인 데이터가 삭제되며
        <strong>삭제 후에는 복구할 수 없습니다.</strong>
      </p>
      <div class="field">
        <label>현재 비밀번호</label>
        <input v-model="password" type="password" placeholder="비밀번호를 입력해주세요" />
      </div>
      <p v-if="error" class="error-text">{{ error }}</p>
      <button class="btn btn-danger btn-block" type="button" :disabled="!password || submitting" @click="withdraw">
        <span v-if="submitting" class="spinner"></span>
        <span>최종 탈퇴하기</span>
      </button>
      <button class="btn btn-ghost btn-block" type="button" @click="step = 'root'">취소</button>
    </div>
  </div>
</template>
