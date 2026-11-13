<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchReceivableCoupons, receiveCoupon, type CouponItem } from '@/api/promo'
import { money } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { useUiStore } from '@/stores/ui'

const list = ref<CouponItem[]>([])
const loading = ref(true)
const busyId = ref<number | null>(null)
const user = useUserStore()
const ui = useUiStore()
const router = useRouter()

async function load() {
  loading.value = true
  try {
    list.value = (await fetchReceivableCoupons()) || []
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function claim(c: CouponItem) {
  if (!user.loggedIn) {
    router.push({ name: 'login', query: { redirect: '/coupons' } })
    return
  }
  busyId.value = c.id
  try {
    await receiveCoupon(c.id)
    ui.toast('领取成功')
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '领取失败')
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <h1>领优惠券</h1>
    <p class="muted">可领取的满减券会在这里展示，结算时选用。</p>
    <div v-if="loading" class="empty">加载中…</div>
    <div v-else-if="!list.length" class="empty">暂无可领优惠券</div>
    <div v-else class="list">
      <article v-for="c in list" :key="c.id" class="card item">
        <div class="amount">
          <span class="yen">¥</span>{{ money(c.amount) }}
        </div>
        <div class="meta">
          <h3>{{ c.name }}</h3>
          <p class="muted">满 {{ money(c.minPoint) }} 可用 · 剩余 {{ c.remainCount ?? '—' }}</p>
          <p v-if="c.note" class="muted">{{ c.note }}</p>
        </div>
        <button class="btn" type="button" :disabled="busyId === c.id" @click="claim(c)">领取</button>
      </article>
    </div>
  </div>
</template>

<style scoped>
.page { padding-top: 24px; max-width: 860px; }
h1 { margin: 0 0 6px; }
.list { display: grid; gap: 12px; margin-top: 18px; }
.item {
  display: grid;
  grid-template-columns: 120px 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 16px;
}
.amount {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--brand);
  text-align: center;
}
.yen { font-size: .9rem; }
h3 { margin: 0 0 6px; font-size: 1rem; font-weight: 600; }
@media (max-width: 640px) {
  .item { grid-template-columns: 1fr; }
}
</style>
