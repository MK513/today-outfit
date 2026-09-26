<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import { useOutfitsStore } from '@/stores/outfits'
import { usePlannerStore } from '@/stores/planner'
import { seedLocalDemoData } from '@/lib/seedDemo'
import { useToast } from '@/composables/useToast'
import Icon from '@/components/Icon.vue'

const auth = useAuthStore()
const wardrobe = useWardrobeStore()
const outfits = useOutfitsStore()
const planner = usePlannerStore()
const router = useRouter()
const route = useRoute()
const { show } = useToast()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

function afterLogin() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  router.replace(redirect)
}

async function submit() {
  error.value = ''
  loading.value = true
  const result = await auth.login({ email: email.value, password: password.value })
  loading.value = false
  if (!result.ok) {
    error.value = result.message
    return
  }
  afterLogin()
}

async function tryDemo() {
  error.value = ''
  loading.value = true
  const result = await auth.loginDemo()
  loading.value = false
  if (!result.ok) {
    error.value = result.message
    return
  }
  seedLocalDemoData(auth.currentUser.id, wardrobe, outfits, planner)
  show('데모 계정으로 체험을 시작합니다')
  afterLogin()
}
</script>

<template>
  <div class="page-auth">
    <div>
      <p style="font-size: 12px; color: var(--muted); font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase">
        Today Outfit
      </p>
      <h1 style="font-size: 28px; font-weight: 800; letter-spacing: -0.02em; margin-top: 8px; line-height: 1.3">
        로그인하고<br />오늘의 코디를 확인하세요
      </h1>
    </div>

    <form class="card" style="display: flex; flex-direction: column; gap: 14px" @submit.prevent="submit">
      <div class="field">
        <label for="email">이메일</label>
        <input id="email" v-model="email" type="email" placeholder="you@example.com" required autocomplete="email" />
      </div>
      <div class="field">
        <label for="password">비밀번호</label>
        <input
          id="password"
          v-model="password"
          type="password"
          placeholder="비밀번호"
          required
          autocomplete="current-password"
        />
      </div>
      <p v-if="error" class="error-text">{{ error }}</p>
      <button class="btn btn-primary" type="submit" :disabled="loading">
        <span v-if="loading" class="spinner"></span>
        <span>로그인</span>
      </button>
    </form>

    <button class="btn btn-ghost btn-block" type="button" :disabled="loading" @click="tryDemo">
      <Icon name="sparkles" :size="15" /> 데모 계정으로 체험하기
    </button>

    <p style="text-align: center; font-size: 13px; color: var(--muted)">
      아직 계정이 없으신가요?
      <RouterLink to="/signup" style="color: var(--accent); font-weight: 700">회원가입</RouterLink>
    </p>
  </div>
</template>
