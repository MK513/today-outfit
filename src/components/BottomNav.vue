<script setup>
import { useRoute, useRouter } from 'vue-router'
import Icon from './Icon.vue'

const route = useRoute()
const router = useRouter()

const tabs = [
  { name: 'home', label: 'HOME', icon: 'home' },
  { name: 'wardrobe', label: 'CLOSET', icon: 'hanger' },
  { name: 'outfits', label: 'OUTFIT', icon: 'stack-2' },
  { name: 'planner', label: 'PLANNER', icon: 'calendar' },
  { name: 'challenge', label: 'CHALLENGE', icon: 'dice-5' },
]

function isActive(tabName) {
  return route.name === tabName || route.matched.some((r) => r.name === tabName)
}

function go(tabName) {
  router.push({ name: tabName })
}
</script>

<template>
  <nav class="bottom-nav">
    <button
      v-for="tab in tabs"
      :key="tab.name"
      class="nav-item"
      :class="{ active: isActive(tab.name) }"
      type="button"
      @click="go(tab.name)"
    >
      <Icon :name="tab.icon" :size="21" :stroke-width="isActive(tab.name) ? 2 : 1.5" />
      <span class="nav-label">{{ tab.label }}</span>
    </button>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: var(--shell-width);
  display: flex;
  background: var(--surface);
  border-top: 1px solid var(--line);
  padding: 10px 6px calc(env(safe-area-inset-bottom, 0px) + 10px);
  z-index: 100;
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  background: none;
  border: none;
  color: #c2c2c2;
  padding: 4px 2px;
  cursor: pointer;
}

.nav-item.active {
  color: var(--ink);
}

.nav-label {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.04em;
}
</style>
