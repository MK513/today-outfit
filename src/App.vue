<script setup>
import { RouterView, useRoute } from 'vue-router'
import { computed, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useWardrobeStore } from '@/stores/wardrobe'
import BottomNav from '@/components/BottomNav.vue'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const auth = useAuthStore()
const { message, visible } = useToast()

const showNav = computed(() => auth.isLoggedIn && !route.meta.guest)

// 로그인 · 로그아웃 · 계정 전환 시 서버 옷장을 다시 불러온다.
const wardrobe = useWardrobeStore()
watch(
  () => auth.currentUser?.id,
  (userId) => {
    if (!userId) {
      wardrobe.reset()
      return
    }
    wardrobe.load(userId).catch(() => {
      // 401은 api.js가 로그아웃 처리, 그 외 오류는 다음 화면 진입 시 다시 시도한다.
    })
  },
  { immediate: true },
)
</script>

<template>
  <div class="app-shell">
    <RouterView />
    <BottomNav v-if="showNav" />
    <transition name="fade">
      <div v-if="visible" class="toast">{{ message }}</div>
    </transition>
  </div>
</template>
