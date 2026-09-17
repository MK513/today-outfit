<script setup>
import { computed } from 'vue'
import { ICON_PATHS } from '@/lib/icons'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 20 },
  strokeWidth: { type: [Number, String], default: 2 },
})

const markup = computed(() => ICON_PATHS[props.name] ?? '')
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    class="icon"
    v-html="markup"
  ></svg>
</template>

<style scoped>
.icon {
  flex-shrink: 0;
  display: block;
}
</style>
