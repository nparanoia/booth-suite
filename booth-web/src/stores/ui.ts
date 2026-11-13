import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const toastText = ref('')
  let timer: number | undefined

  function toast(message: string) {
    toastText.value = message
    window.clearTimeout(timer)
    timer = window.setTimeout(() => {
      toastText.value = ''
    }, 2200)
  }

  return { toastText, toast }
})
