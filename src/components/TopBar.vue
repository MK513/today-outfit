<script setup>
import { useRouter } from 'vue-router'
import Icon from './Icon.vue'

const props = defineProps({
  title: { type: String, required: true },
  back: { type: Boolean, default: false },
  settings: { type: Boolean, default: false },
})

const router = useRouter()
</script>

<template>
  <header class="top-bar">
    <button v-if="back" class="icon-btn" type="button" aria-label="뒤로가기" @click="router.back()">
      <Icon name="arrow-left" :size="20" />
    </button>
    <div v-else class="icon-btn-placeholder"></div>
    <h1>{{ title }}</h1>
    <button
      v-if="settings"
      class="icon-btn"
      type="button"
      aria-label="설정"
      @click="router.push({ name: 'settings' })"
    >
      <Icon name="settings" :size="20" />
    </button>
    <div v-else class="icon-btn-placeholder"></div>
  </header>
</template>

<style scoped>
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 14px 4px;
}

.top-bar h1 {
  font-size: 15px;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.icon-btn,
.icon-btn-placeholder {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--ink);
  border-radius: 50%;
}

.icon-btn:active {
  background: var(--accent-soft);
}
</style>
