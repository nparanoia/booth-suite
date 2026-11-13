<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { BRAND, BRAND_MARK } from '@/config/brand'

const brand = BRAND
const mark = BRAND_MARK
const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const keyword = ref('')
const menuOpen = ref(false)

function search() {
  const q = keyword.value.trim()
  router.push({ name: 'search', query: q ? { q } : {} })
  menuOpen.value = false
}
</script>

<template>
  <header class="header">
    <div class="container bar">
      <RouterLink class="brand" to="/" @click="menuOpen = false">
        <span class="mark">{{ mark }}</span>
        <span class="name">{{ brand }}</span>
      </RouterLink>

      <form class="search" @submit.prevent="search">
        <input v-model="keyword" type="search" placeholder="搜本店商品 / 分类" aria-label="搜索商品" />
        <button class="btn" type="submit">搜索</button>
      </form>

      <nav class="nav desktop">
        <RouterLink to="/category">分类</RouterLink>
        <RouterLink to="/seckill">秒杀</RouterLink>
        <RouterLink to="/coupons">领券</RouterLink>
        <RouterLink to="/support">店员</RouterLink>
        <RouterLink to="/cart" class="cart-link">
          购物车
          <em v-if="cart.count">{{ cart.count > 99 ? '99+' : cart.count }}</em>
        </RouterLink>
        <RouterLink v-if="user.loggedIn" to="/me">{{ user.displayName }}</RouterLink>
        <RouterLink v-else to="/login">登录</RouterLink>
      </nav>

      <button class="burger" type="button" aria-label="菜单" @click="menuOpen = !menuOpen">
        <span /><span /><span />
      </button>
    </div>

    <div v-if="menuOpen" class="mobile-panel">
      <RouterLink to="/category" @click="menuOpen = false">全部分类</RouterLink>
      <RouterLink to="/seckill" @click="menuOpen = false">限时秒杀</RouterLink>
      <RouterLink to="/coupons" @click="menuOpen = false">领优惠券</RouterLink>
      <RouterLink to="/cart" @click="menuOpen = false">购物车（{{ cart.count }}）</RouterLink>
      <RouterLink to="/orders" @click="menuOpen = false">我的订单</RouterLink>
      <RouterLink to="/support" @click="menuOpen = false">智能客服</RouterLink>
      <RouterLink v-if="user.loggedIn" to="/me" @click="menuOpen = false">个人中心</RouterLink>
      <RouterLink v-else to="/login" @click="menuOpen = false">登录 / 注册</RouterLink>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: var(--header);
  background: rgba(255,255,255,.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--line);
}
.bar {
  height: var(--header);
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: "DM Sans", "Noto Sans SC", sans-serif;
}
.mark {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--brand);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
}
.name { font-size: 1.2rem; font-weight: 700; letter-spacing: .04em; }
.search {
  display: flex;
  gap: 8px;
  max-width: min(720px, 100%);
  width: 100%;
  justify-self: center;
}
.search input {
  flex: 1;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 10px 16px;
  outline: none;
  background: #fff;
}
.search input:focus { border-color: var(--brand); }
.search .btn { padding: 10px 18px; }
.nav {
  display: flex;
  align-items: center;
  gap: 18px;
  font-size: .95rem;
}
.nav a:hover { color: var(--brand); }
.cart-link { position: relative; }
.cart-link em {
  position: absolute;
  top: -10px;
  right: -16px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--brand);
  color: #fff;
  font-size: .7rem;
  font-style: normal;
  display: grid;
  place-items: center;
}
.burger {
  display: none;
  width: 40px;
  height: 40px;
  border: 0;
  background: transparent;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
}
.burger span {
  display: block;
  height: 2px;
  background: var(--ink);
  border-radius: 2px;
}
.mobile-panel {
  display: none;
  border-top: 1px solid var(--line);
  background: #fff;
  padding: 8px 16px 16px;
}
.mobile-panel a {
  display: block;
  padding: 12px 4px;
  border-bottom: 1px solid #f0f0f0;
}

@media (max-width: 900px) {
  .desktop { display: none; }
  .burger { display: inline-flex; }
  .bar { grid-template-columns: auto 1fr auto; }
  .mobile-panel { display: block; }
  .search .btn { display: none; }
}
</style>
