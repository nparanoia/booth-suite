<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import * as adminApi from '@/api/admin'
import { money, orderStatusLabel } from '@/utils/format'
import { useUiStore } from '@/stores/ui'
import type { OrderSummary, ProductListItem } from '@/types/api'
import type { MemberAdmin, ReturnApply } from '@/api/admin'

const ui = useUiStore()
const loading = ref(true)
const products = ref<ProductListItem[]>([])
const productTotal = ref(0)
const orders = ref<OrderSummary[]>([])
const orderTotal = ref(0)
const members = ref<MemberAdmin[]>([])
const memberTotal = ref(0)
const returns = ref<ReturnApply[]>([])

const onShelf = computed(() => products.value.filter((p) => p.publishStatus === 'ON_SHELF').length)
const waitDeliver = computed(() => orders.value.filter((o) => o.status === 'WAIT_DELIVER').length)
const waitPay = computed(() => orders.value.filter((o) => o.status === 'WAIT_PAY').length)
const pendingReturn = computed(() => returns.value.filter((r) => r.status === 'PENDING').length)

const statusBars = computed(() => {
  const keys = ['WAIT_PAY', 'WAIT_DELIVER', 'DELIVERED', 'COMPLETED', 'CLOSED'] as const
  const counts = keys.map((k) => orders.value.filter((o) => o.status === k).length)
  const max = Math.max(1, ...counts)
  return keys.map((k, i) => ({
    key: k,
    label: orderStatusLabel(k),
    value: counts[i],
    height: `${Math.round((counts[i] / max) * 100)}%`,
  }))
})

const amountBars = computed(() => {
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() - (6 - i))
    return d.toISOString().slice(0, 10)
  })
  const sums = days.map((day) =>
    orders.value
      .filter((o) => String(o.createTime || '').startsWith(day))
      .reduce((s, o) => s + Number(o.payAmount || 0), 0),
  )
  const max = Math.max(1, ...sums)
  return days.map((day, i) => ({
    label: day.slice(5),
    value: sums[i],
    height: `${Math.round((sums[i] / max) * 100)}%`,
  }))
})

onMounted(async () => {
  try {
    const [p, o, m, r] = await Promise.all([
      adminApi.adminProducts({ pageNum: 1, pageSize: 100 }),
      adminApi.adminOrders({ pageNum: 1, pageSize: 100 }),
      adminApi.adminMembers({ pageNum: 1, pageSize: 100 }),
      adminApi.adminReturns(),
    ])
    products.value = p.list || []
    productTotal.value = p.total || 0
    orders.value = o.list || []
    orderTotal.value = o.total || 0
    members.value = m.list || []
    memberTotal.value = m.total || 0
    returns.value = r || []
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '仪表盘加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <h1 style="margin:0 0 16px;font-size:1.35rem">今日店况</h1>
    <div v-if="loading" class="empty">加载中…</div>
    <template v-else>
      <div class="stat-grid">
        <div class="stat"><div class="n">{{ productTotal }}</div><div class="l">商品总数 · 上架 {{ onShelf }}</div></div>
        <div class="stat"><div class="n">{{ orderTotal }}</div><div class="l">订单总数 · 待发货 {{ waitDeliver }}</div></div>
        <div class="stat"><div class="n">{{ memberTotal }}</div><div class="l">会员总数 · 待付款 {{ waitPay }}</div></div>
        <div class="stat"><div class="n">{{ pendingReturn }}</div><div class="l">待处理售后</div></div>
      </div>

      <div class="chart-wrap">
        <section class="admin-card">
          <h2 style="margin:0 0 8px;font-size:1rem">近 7 日成交额（抽样页）</h2>
          <div class="bars">
            <div v-for="b in amountBars" :key="b.label" class="bar-col">
              <div class="bar" :style="{ height: b.height }" :title="`¥${money(b.value)}`" />
              <span>{{ b.label }}</span>
            </div>
          </div>
        </section>
        <section class="admin-card">
          <h2 style="margin:0 0 8px;font-size:1rem">订单状态分布</h2>
          <div class="bars">
            <div v-for="b in statusBars" :key="b.key" class="bar-col">
              <div class="bar" :style="{ height: b.height }" :title="String(b.value)" />
              <span>{{ b.label }}</span>
            </div>
          </div>
        </section>
      </div>

      <section class="admin-card" style="margin-top:14px">
        <h2 style="margin:0 0 12px;font-size:1rem">最近订单</h2>
        <table class="admin-table">
          <thead>
            <tr><th>订单号</th><th>状态</th><th>金额</th><th>时间</th></tr>
          </thead>
          <tbody>
            <tr v-for="o in orders.slice(0, 8)" :key="o.orderSn">
              <td>
                <RouterLink :to="`/admin/orders?sn=${o.orderSn}`" style="color:var(--brand)">{{ o.orderSn }}</RouterLink>
              </td>
              <td><span class="tag warn">{{ orderStatusLabel(o.status) }}</span></td>
              <td>¥{{ money(o.payAmount) }}</td>
              <td class="muted">{{ o.createTime }}</td>
            </tr>
            <tr v-if="!orders.length"><td colspan="4" class="muted">暂无订单</td></tr>
          </tbody>
        </table>
      </section>
    </template>
  </div>
</template>
