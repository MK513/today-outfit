import { defineStore } from 'pinia'
import { api } from '@/lib/api'
import { loadJSON, saveJSON, removeKey } from '@/lib/storage'

// 세션: { token, expiresAt(ms), user: { id, email, name, isDemo, createdAt } }
function loadSession() {
  const session = loadJSON('session', null)
  if (!session?.token || !(session.expiresAt > Date.now())) return null
  return session
}

function fail(error) {
  return { ok: false, message: error.message, errorCode: error.errorCode }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    session: loadSession(),
  }),
  getters: {
    // 만료는 서버가 401로 알려주면 api.js → clearSession()으로 처리한다.
    isLoggedIn: (state) => !!state.session,
    currentUser: (state) => state.session?.user ?? null,
    token: (state) => state.session?.token ?? null,
  },
  actions: {
    setSession(auth) {
      this.session = {
        token: auth.accessToken,
        expiresAt: Date.now() + auth.expiresIn * 1000,
        user: auth.user,
      }
      saveJSON('session', this.session)
    },
    clearSession() {
      this.session = null
      removeKey('session')
    },
    /** 저장된 세션이 서버에서도 유효한지 확인하고 사용자 정보를 갱신한다. 401이면 api.js가 세션을 지운다. */
    async refreshMe() {
      try {
        const user = await api.get('/users/me')
        if (this.session) {
          this.session = { ...this.session, user }
          saveJSON('session', this.session)
        }
      } catch {
        // 401은 onUnauthorized에서 처리, 네트워크 오류는 저장된 세션을 유지한다.
      }
    },
    async signup({ email, password, passwordConfirm, name }) {
      try {
        await api.post('/auth/signup', { email, password, passwordConfirm, name })
        return { ok: true }
      } catch (e) {
        return fail(e)
      }
    },
    async login({ email, password }) {
      try {
        this.setSession(await api.post('/auth/login', { email, password }))
        return { ok: true }
      } catch (e) {
        return fail(e)
      }
    },
    async loginDemo() {
      try {
        this.setSession(await api.post('/auth/demo'))
        return { ok: true }
      } catch (e) {
        return fail(e)
      }
    },
    async logout() {
      try {
        await api.post('/auth/logout')
      } catch {
        // 서버 무효화에 실패해도 이 기기에서는 로그아웃한다.
      }
      this.clearSession()
    },
    async withdraw(currentPassword) {
      const userId = this.currentUser?.id
      try {
        await api.post('/users/me/withdrawal', { password: currentPassword })
      } catch (e) {
        return fail(e)
      }
      this.clearSession()
      return { ok: true, withdrawnUserId: userId }
    },
  },
})

/** 서버 인증 도입 전 localStorage에 평문 비밀번호로 저장하던 계정 목록을 지운다. */
export function removeLegacyLocalAccounts() {
  removeKey('users')
  removeKey('currentUserId')
}
