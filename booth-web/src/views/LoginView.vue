<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useUiStore } from '@/stores/ui'
import { BRAND, BRAND_TAGLINE } from '@/config/brand'

const brand = BRAND

const user = useUserStore()
const cart = useCartStore()
const ui = useUiStore()
const router = useRouter()
const route = useRoute()

const mode = ref<'login' | 'register'>('login')
const form = ref({
  username: '',
  password: '',
  phone: '',
  nickname: '',
})
const busy = ref(false)

async function submit() {
  busy.value = true
  try {
    if (mode.value === 'login') {
      await user.login(form.value.username, form.value.password)
    } else {
      await user.register({
        username: form.value.username,
        password: form.value.password,
        phone: form.value.phone,
        nickname: form.value.nickname || undefined,
      })
    }
    await cart.refresh()
    ui.toast('欢迎回来')
    const redirect = String(route.query.redirect || '/')
    router.replace(redirect)
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '失败')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <form class="card panel" @submit.prevent="submit">
      <h1>{{ mode === 'login' ? `登录 ${brand}` : `注册本店账号` }}</h1>
      <p class="muted">{{ BRAND_TAGLINE }}</p>
      <input v-model="form.username" required placeholder="用户名" autocomplete="username" />
      <input v-model="form.password" required type="password" placeholder="密码" autocomplete="current-password" />
      <template v-if="mode === 'register'">
        <input v-model="form.phone" required placeholder="手机号" />
        <input v-model="form.nickname" placeholder="昵称（可选）" />
      </template>
      <button class="btn block lg" type="submit" :disabled="busy">
        {{ mode === 'login' ? '登录' : '注册并登录' }}
      </button>
      <button class="switch" type="button" @click="mode = mode === 'login' ? 'register' : 'login'">
        {{ mode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.wrap {
  min-height: calc(100vh - var(--header) - 180px);
  display: grid;
  place-items: center;
  padding: 32px 16px;
}
.panel {
  width: min(100%, 420px);
  padding: 28px 24px;
  display: grid;
  gap: 12px;
}
h1 { margin: 0; font-size: 1.5rem; }
input {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 12px 14px;
}
.switch {
  border: 0;
  background: none;
  color: var(--brand);
  justify-self: center;
}
</style>
