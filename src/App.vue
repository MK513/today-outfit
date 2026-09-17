<script setup>
import { RouterView, useRoute } from 'vue-router'
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import BottomNav from '@/components/BottomNav.vue'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const auth = useAuthStore()
const { message, visible } = useToast()

const showNav = computed(() => auth.isLoggedIn && !route.meta.guest)
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
