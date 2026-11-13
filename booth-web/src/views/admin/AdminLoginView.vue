<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { useUiStore } from '@/stores/ui'
import { BRAND } from '@/config/brand'
import '@/styles/admin.css'

const admin = useAdminStore()
const ui = useUiStore()
const router = useRouter()
const route = useRoute()
const brand = BRAND
const form = ref({
  username: import.meta.env.DEV ? 'admin' : '',
  password: '',
})
const busy = ref(false)

async function submit() {
  busy.value = true
  try {
    await admin.login(form.value.username, form.value.password)
    ui.toast('登录成功')
    router.replace(String(route.query.redirect || '/admin'))
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '登录失败')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <form class="admin-card panel" @submit.prevent="submit">
      <h1>{{ brand }} 店主后台</h1>
      <p class="muted">个人商户入口：上架、接单、发货、售后都在这里。演示账号常见为 admin（密码不预填）。</p>
      <input v-model="form.username" required placeholder="店主账号" />
      <input v-model="form.password" required type="password" placeholder="密码" />
      <button class="btn block lg" type="submit" :disabled="busy">进入后台</button>
      <RouterLink to="/" class="back">回本店前台</RouterLink>
    </form>
    <div v-if="ui.toastText" class="toast">{{ ui.toastText }}</div>
  </div>
</template>

<style scoped>
.wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at top right, rgba(255,103,0,.18), transparent 40%),
    #111827;
  padding: 24px;
}
.panel {
  width: min(100%, 400px);
  display: grid;
  gap: 12px;
}
h1 { margin: 0; font-size: 1.4rem; }
input {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 12px 14px;
}
.back {
  text-align: center;
  color: var(--muted);
  font-size: .9rem;
}
</style>
