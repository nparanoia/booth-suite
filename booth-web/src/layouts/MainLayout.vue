<script setup lang="ts">
import { onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import AgentFab from '@/components/AgentFab.vue'
import { useUiStore } from '@/stores/ui'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const ui = useUiStore()
const user = useUserStore()
const cart = useCartStore()

onMounted(async () => {
  if (user.token) {
    await user.refreshProfile()
    await cart.refresh()
  }
})
</script>

<template>
  <div class="shell">
    <AppHeader />
    <main class="main">
      <RouterView />
    </main>
    <AppFooter />
    <AgentFab />
    <div v-if="ui.toastText" class="toast">{{ ui.toastText }}</div>
  </div>
</template>

<style scoped>
.shell { min-height: 100vh; display: flex; flex-direction: column; }
.main { flex: 1; padding-bottom: 24px; }
</style>
