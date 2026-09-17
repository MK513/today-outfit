import { defineStore } from 'pinia'
import { loadJSON, saveJSON } from '@/lib/storage'
import { uid } from '@/lib/constants'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    users: loadJSON('users', []),
    currentUserId: loadJSON('currentUserId', null),
  }),
  getters: {
    isLoggedIn: (state) => !!state.currentUserId,
    currentUser: (state) => state.users.find((u) => u.id === state.currentUserId) ?? null,
  },
  actions: {
    persist() {
      saveJSON('users', this.users)
      saveJSON('currentUserId', this.currentUserId)
    },
    signup({ email, password, passwordConfirm, name }) {
      const normalizedEmail = email.trim().toLowerCase()
      if (!normalizedEmail || !password || !name.trim()) {
        return { ok: false, message: '이메일, 비밀번호, 이름을 모두 입력해주세요.' }
      }
      if (password !== passwordConfirm) {
        return { ok: false, message: '비밀번호가 일치하지 않습니다.' }
      }
      if (this.users.some((u) => u.email === normalizedEmail && !u.withdrawnAt)) {
        return { ok: false, message: '이미 가입된 이메일입니다.' }
      }
      const user = {
        id: uid('user'),
        email: normalizedEmail,
        password,
        name: name.trim(),
        createdAt: new Date().toISOString(),
        withdrawnAt: null,
      }
      this.users.push(user)
      this.persist()
      return { ok: true }
    },
    login({ email, password }) {
      const normalizedEmail = email.trim().toLowerCase()
      const user = this.users.find((u) => u.email === normalizedEmail && !u.withdrawnAt)
      if (!user || user.password !== password) {
        return { ok: false, message: '이메일 또는 비밀번호가 올바르지 않습니다.' }
      }
      this.currentUserId = user.id
      this.persist()
      return { ok: true }
    },
    loginAsUserId(userId) {
      this.currentUserId = userId
      this.persist()
    },
    logout() {
      this.currentUserId = null
      this.persist()
    },
    withdraw(currentPassword) {
      const user = this.currentUser
      if (!user) return { ok: false, message: '로그인이 필요합니다.' }
      if (user.password !== currentPassword) {
        return { ok: false, message: '현재 비밀번호가 올바르지 않습니다.' }
      }
      user.withdrawnAt = new Date().toISOString()
      this.currentUserId = null
      this.persist()
      return { ok: true, withdrawnUserId: user.id }
    },
  },
})
