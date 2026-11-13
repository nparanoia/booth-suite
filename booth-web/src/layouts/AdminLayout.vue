<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { useUiStore } from '@/stores/ui'
import { BRAND } from '@/config/brand'
import '@/styles/admin.css'

const admin = useAdminStore()
const ui = useUiStore()
const router = useRouter()
const brand = BRAND

/** 演示默认超管带 *；菜单与 permissionCodes 对齐，无权限则隐藏入口。 */
const nav = computed(() => [
  { to: '/admin', label: '今日总览', show: true },
  { to: '/admin/products', label: '我的货架', show: admin.can('product:product:view') },
  { to: '/admin/orders', label: '订单发货', show: admin.can('trade:order:view') },
  { to: '/admin/members', label: '本店会员', show: admin.can('user:member:view') },
  { to: '/admin/returns', label: '售后处理', show: admin.can('trade:order:edit') },
  { to: '/admin/ops', label: '客服助手', show: admin.can('*') || admin.can('trade:order:view') },
])

onMounted(async () => {
  if (admin.token) await admin.refreshProfile()
})

async function logout() {
  await admin.logout()
  ui.toast('已退出后台')
  router.push({ name: 'admin-login' })
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-side">
      <div class="admin-brand">
        <span class="dot">店</span>
        <span>{{ brand }} 店主</span>
      </div>
      <p class="perm-hint muted">
        演示默认超管（全部权限）。菜单按权限码过滤；接口仍做服务端鉴权。
      </p>
      <nav class="admin-nav">
        <RouterLink v-for="item in nav.filter((n) => n.show)" :key="item.to" :to="item.to">
          {{ item.label }}
        </RouterLink>
      </nav>
      <div style="flex:1" />
      <RouterLink to="/" style="padding:10px 12px;color:#9ca3af;font-size:.85rem">← 回本店前台</RouterLink>
    </aside>
    <div class="admin-main">
      <header class="admin-top">
        <strong>店主后台 · 一个人也能管</strong>
        <div style="display:flex;gap:12px;align-items:center">
          <span class="muted">{{ admin.displayName }}</span>
          <button class="btn ghost" type="button" style="padding:6px 14px" @click="logout">退出</button>
        </div>
      </header>
      <div class="admin-body">
        <RouterView />
      </div>
    </div>
    <div v-if="ui.toastText" class="toast">{{ ui.toastText }}</div>
  </div>
</template>

<style scoped>
.perm-hint {
  margin: 0 12px 8px;
  font-size: 0.75rem;
  line-height: 1.35;
}
</style>
