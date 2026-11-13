<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as tradeApi from '@/api/trade'
import type { OrderDetail } from '@/types/api'
import { money, orderStatusLabel, placeholderPic, specsText } from '@/utils/format'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const order = ref<OrderDetail | null>(null)
const busy = ref(false)

/** 进客服时带上单号；按状态给弱提示（改地址/取消/退货）。 */
const supportLink = computed(() => {
  const sn = order.value?.orderSn
  if (!sn) return '/support'
  const status = order.value?.status
  let intent = 'query'
  if (status === 'WAIT_PAY') intent = 'cancel'
  else if (status === 'WAIT_DELIVER') intent = 'address'
  else if (status === 'COMPLETED') intent = 'return'
  return { path: '/support', query: { orderSn: sn, intent } }
})

async function load() {
  order.value = await tradeApi.fetchOrder(String(route.params.orderSn))
}

/**
 * 对齐 booth-swarm：先拿支付凭证，再进收银台/支付宝页；本接口不改订单状态。
 * mock://pay/{sn} → 站内收银台；HTML 表单 → 写页跳支付宝；http(s) → 外链。
 */
async function pay() {
  if (!order.value || busy.value) return
  busy.value = true
  try {
    const payUrl = await tradeApi.payOrder(order.value.orderSn)
    const sn = order.value.orderSn
    if (!payUrl) {
      ui.toast('未返回支付凭证')
      return
    }
    if (payUrl.startsWith('mock://pay/')) {
      await router.push({ name: 'order-pay', params: { orderSn: sn } })
      return
    }
    if (payUrl.includes('<form') || payUrl.includes('<FORM')) {
      const win = window.open('', '_blank')
      if (!win) {
        ui.toast('请允许弹窗以打开支付页')
        return
      }
      win.document.open()
      win.document.write(payUrl)
      win.document.close()
      return
    }
    if (/^https?:\/\//i.test(payUrl)) {
      window.location.href = payUrl
      return
    }
    ui.toast('无法识别的支付凭证')
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '发起支付失败')
  } finally {
    busy.value = false
  }
}

async function cancel() {
  if (!order.value || busy.value) return
  busy.value = true
  try {
    await tradeApi.cancelOrder(order.value.orderSn)
    ui.toast('订单已取消')
    if (window.history.length > 1) router.back()
    else router.replace({ name: 'orders' })
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '取消失败')
  } finally {
    busy.value = false
  }
}

async function receive() {
  if (!order.value || busy.value) return
  busy.value = true
  try {
    await tradeApi.receiveOrder(order.value.orderSn)
    ui.toast('已确认收货')
    await load()
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '操作失败')
  } finally {
    busy.value = false
  }
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
    <h1>订单详情</h1>
    <section class="card block">
      <p class="status">{{ orderStatusLabel(order.status) }}</p>
      <p>订单号 {{ order.orderSn }}</p>
      <p class="muted">下单时间 {{ order.createTime }}</p>
      <p>
        {{ order.receiverName }} {{ order.receiverPhone }}<br />
        {{ order.receiverProvince }}{{ order.receiverCity }}{{ order.receiverRegion }}{{ order.receiverDetailAddress }}
      </p>
    </section>
    <section class="card block">
      <article v-for="(item, idx) in order.items" :key="idx" class="goods">
        <img :src="item.pic || placeholderPic(item.skuId)" alt="" />
        <div>
          <h3>{{ item.productName }}</h3>
          <p class="muted">{{ specsText(item.specs) }} × {{ item.quantity }}</p>
          <p class="price">¥{{ money(item.price) }}</p>
        </div>
      </article>
      <div class="pay">应付 <span class="price lg">¥{{ money(order.payAmount) }}</span></div>
      <div class="actions">
        <button v-if="order.status === 'WAIT_PAY'" class="btn" type="button" :disabled="busy" @click="pay">去支付</button>
        <button v-if="order.status === 'WAIT_PAY'" class="btn ghost" type="button" :disabled="busy" @click="cancel">取消订单</button>
        <button v-if="order.status === 'DELIVERED' || order.status === 'WAIT_RECEIVE'" class="btn" type="button" :disabled="busy" @click="receive">确认收货</button>
        <RouterLink class="btn ghost" :to="supportLink">找客服</RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page { padding-top: 24px; max-width: 860px; }
.block { padding: 18px; margin-bottom: 14px; }
.status { color: var(--brand); font-size: 1.2rem; font-weight: 700; margin: 0 0 8px; }
.goods {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}
.goods img { width: 72px; height: 72px; object-fit: cover; border-radius: 8px; }
.goods h3 { margin: 0; font-size: .95rem; font-weight: 500; }
.pay { margin: 16px 0; text-align: right; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
</style>
