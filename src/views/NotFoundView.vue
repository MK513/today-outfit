<script setup>
import { useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
const router = useRouter()
</script>

<template>
  <div class="page" style="justify-content: center; align-items: center">
    <div class="empty-state">
      <span class="icon-wrap"><Icon name="search" :size="20" :stroke-width="1.6" /></span>
      <p style="font-weight: 700; color: var(--ink)">페이지를 찾을 수 없어요</p>
      <button class="btn btn-primary" type="button" @click="router.push({ name: 'home' })">홈으로</button>
    </div>
  </div>
</template>
