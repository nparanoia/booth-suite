<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useUiStore } from '@/stores/ui'

const router = useRouter()
const user = useUserStore()
const ui = useUiStore()

function openSupport() {
  if (!user.loggedIn) {
    ui.toast('请先登录后再使用客服')
    void router.push({ name: 'login', query: { redirect: '/support' } })
    return
  }
  void router.push('/support')
}
</script>

<template>
  <button class="fab" type="button" aria-label="店内客服" @click="openSupport">
    客服
  </button>
</template>

<style scoped>
.fab {
  position: fixed;
  right: 20px;
  bottom: 24px;
  z-index: 40;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 0;
  background: var(--brand);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 10px 28px rgba(255, 103, 0, .35);
}
.fab:hover { background: var(--brand-dark); }
@media (max-width: 768px) {
  .fab { right: 14px; bottom: 18px; width: 52px; height: 52px; }
}
</style>
