<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as tradeApi from '@/api/trade'
import type { OrderDetail } from '@/types/api'
import { money } from '@/utils/format'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const order = ref<OrderDetail | null>(null)
const busy = ref(false)

async function load() {
  order.value = await tradeApi.fetchOrder(String(route.params.orderSn))
  if (order.value.status !== 'WAIT_PAY') {
    ui.toast('订单无需再支付')
    router.replace({ name: 'order-detail', params: { orderSn: order.value.orderSn } })
  }
}

async function confirmPay() {
  if (!order.value || busy.value) return
  busy.value = true
  try {
    await tradeApi.confirmMockPay(order.value.orderSn)
    ui.toast('支付成功')
    router.replace({ name: 'orders' })
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '支付失败')
  } finally {
    busy.value = false
  }
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace({ name: 'order-detail', params: { orderSn: String(route.params.orderSn) } })
}

onMounted(async () => {
  try {
    await load()
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '订单不存在')
    router.replace({ name: 'orders' })
  }
})
</script>

<template>
  <div v-if="order" class="container page">
    <h1>收银台</h1>
    <p class="muted tip">模拟支付渠道（对齐 booth-swarm 沙箱确认页）。点确认后订单才会变为已支付。</p>
    <section class="card block">
      <p class="label">订单号</p>
      <p class="mono">{{ order.orderSn }}</p>
      <p class="label">应付金额</p>
      <p class="amount">¥{{ money(order.payAmount) }}</p>
      <div class="actions">
        <button class="btn" type="button" :disabled="busy" @click="confirmPay">确认支付</button>
        <button class="btn ghost" type="button" :disabled="busy" @click="goBack">返回订单</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page { padding-top: 24px; max-width: 520px; }
.tip { margin: -4px 0 16px; }
.block { padding: 20px; }
.label { margin: 0 0 4px; color: var(--muted); font-size: .85rem; }
.mono { margin: 0 0 16px; word-break: break-all; }
.amount {
  margin: 0 0 20px;
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--brand);
}
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
</style>
