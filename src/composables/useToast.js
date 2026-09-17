import { ref } from 'vue'

const message = ref('')
const visible = ref(false)
let timer = null

export function useToast() {
  function show(text, duration = 2200) {
    message.value = text
    visible.value = true
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      visible.value = false
    }, duration)
  }
  return { message, visible, show }
}
