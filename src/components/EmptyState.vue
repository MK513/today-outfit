<script setup>
import Icon from './Icon.vue'

defineProps({
  icon: { type: String, default: 'search' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<template>
  <div class="empty-state">
    <span class="icon-wrap">
      <Icon :name="icon" :size="20" :stroke-width="1.6" />
    </span>
    <p style="font-weight: 700; color: var(--ink)">{{ title }}</p>
    <p v-if="description">{{ description }}</p>
    <slot />
  </div>
</template>
