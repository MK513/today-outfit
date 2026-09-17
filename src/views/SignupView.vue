<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'

const auth = useAuthStore()
const router = useRouter()
const { show } = useToast()

const form = ref({ email: '', password: '', passwordConfirm: '', name: '' })
const error = ref('')
const loading = ref(false)

function submit() {
  error.value = ''
  loading.value = true
  const result = auth.signup(form.value)
  loading.value = false
  if (!result.ok) {
    error.value = result.message
    return
  }
  show('회원가입이 완료되었습니다. 로그인해주세요.')
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="page-auth">
    <div>
      <button
        type="button"
        style="background: none; border: none; color: var(--muted); padding: 0; margin-bottom: 12px; cursor: pointer"
        @click="router.push({ name: 'login' })"
      >
        ← 로그인으로
      </button>
      <h1 style="font-size: 24px; font-weight: 800">회원가입</h1>
    </div>

    <form class="card" style="display: flex; flex-direction: column; gap: 14px" @submit.prevent="submit">
      <div class="field">
        <label for="name">이름</label>
        <input id="name" v-model="form.name" type="text" placeholder="이름" required autocomplete="name" />
      </div>
      <div class="field">
        <label for="s-email">이메일</label>
        <input id="s-email" v-model="form.email" type="email" placeholder="you@example.com" required autocomplete="email" />
      </div>
      <div class="field">
        <label for="s-password">비밀번호</label>
        <input
          id="s-password"
          v-model="form.password"
          type="password"
          placeholder="비밀번호"
          required
          autocomplete="new-password"
        />
      </div>
      <div class="field">
        <label for="s-password2">비밀번호 확인</label>
        <input
          id="s-password2"
          v-model="form.passwordConfirm"
          type="password"
          placeholder="비밀번호 확인"
          required
          autocomplete="new-password"
        />
      </div>
      <p v-if="error" class="error-text">{{ error }}</p>
      <button class="btn btn-primary" type="submit" :disabled="loading">
        <span v-if="loading" class="spinner"></span>
        <span>가입하기</span>
      </button>
    </form>
  </div>
</template>
