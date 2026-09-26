import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { configureApi } from './lib/api'
import { useAuthStore, removeLegacyLocalAccounts } from './stores/auth'

const app = createApp(App)

app.use(createPinia())
app.use(router)

removeLegacyLocalAccounts()

const auth = useAuthStore()
configureApi({
  getToken: () => auth.token,
  // 토큰 만료·무효화: 세션을 지우고 현재 화면으로 돌아올 수 있게 로그인으로 보낸다.
  onUnauthorized: () => {
    auth.clearSession()
    const current = router.currentRoute.value
    if (!current.meta.guest) {
      router.replace({ name: 'login', query: { redirect: current.fullPath } })
    }
  },
})

app.mount('#app')

if (auth.isLoggedIn) auth.refreshMe()
