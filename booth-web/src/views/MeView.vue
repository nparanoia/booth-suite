<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useUiStore } from '@/stores/ui'
import { BRAND } from '@/config/brand'

const user = useUserStore()
const cart = useCartStore()
const ui = useUiStore()
const router = useRouter()
const brand = BRAND

onMounted(() => {
  if (user.loggedIn) user.refreshProfile()
})

async function logout() {
  await user.logout()
  cart.cart = null
  ui.toast('已退出')
  router.push('/')
}
</script>

<template>
  <div class="container page">
    <section class="hero card">
      <template v-if="user.loggedIn && user.profile">
        <div class="avatar">{{ (user.displayName || '青').slice(0, 1) }}</div>
        <div>
          <h1>{{ user.displayName }}</h1>
          <p class="muted">{{ user.profile.phone }} · 积分 {{ user.profile.integration }}</p>
        </div>
      </template>
      <template v-else>
        <div>
          <h1>欢迎来到 {{ brand }}</h1>
          <p class="muted">个人自营小店 · 登录后同步购物车与订单</p>
          <RouterLink class="btn" to="/login">去登录</RouterLink>
        </div>
      </template>
    </section>

    <section class="grid">
      <RouterLink class="tile card" to="/orders">我的订单</RouterLink>
      <RouterLink class="tile card" to="/cart">购物车</RouterLink>
      <RouterLink class="tile card" to="/coupons">优惠券</RouterLink>
      <RouterLink class="tile card" to="/seckill">限时秒杀</RouterLink>
      <RouterLink class="tile card" to="/support">店内客服</RouterLink>
      <button class="tile card dead" type="button" disabled>收藏夹（即将上线）</button>
      <button v-if="user.loggedIn" class="tile card" type="button" @click="logout">退出登录</button>
    </section>
  </div>
</template>

<style scoped>
.page { padding-top: 24px; }
.hero {
  padding: 24px;
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  background:
    linear-gradient(120deg, #fff7f0, #fff 50%);
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--brand);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 1.4rem;
  font-weight: 700;
}
h1 { margin: 0 0 6px; font-size: 1.4rem; }
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.tile {
  padding: 22px 18px;
  text-align: left;
  border: 0;
  font-size: 1rem;
  font-weight: 500;
}
.tile:hover:not(:disabled) { color: var(--brand); }
.dead { color: var(--muted); cursor: default; }
@media (max-width: 700px) {
  .grid { grid-template-columns: 1fr 1fr; }
}
</style>
