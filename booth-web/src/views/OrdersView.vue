<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchOrders } from '@/api/trade'
import type { OrderSummary } from '@/types/api'
import { money, orderStatusLabel } from '@/utils/format'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const list = ref<OrderSummary[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const page = await fetchOrders({ pageNum: 1, pageSize: 50 })
    list.value = page.list || []
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '加载订单失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container page">
    <h1>我的订单</h1>
    <div v-if="loading" class="empty">加载中…</div>
    <div v-else-if="!list.length" class="empty">还没有订单</div>
    <RouterLink
      v-for="o in list"
      :key="o.orderSn"
      class="row card"
      :to="`/orders/${o.orderSn}`"
    >
      <div>
        <strong>{{ o.orderSn }}</strong>
        <p class="muted">{{ o.createTime }}</p>
      </div>
      <div class="right">
        <span class="status">{{ orderStatusLabel(o.status) }}</span>
        <span class="price">¥{{ money(o.payAmount) }}</span>
      </div>
    </RouterLink>
  </div>
</template>

<style scoped>
.page { padding-top: 24px; max-width: 860px; }
h1 { margin: 0 0 16px; }
.row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  margin-bottom: 10px;
}
.right { text-align: right; display: grid; gap: 6px; }
.status { color: var(--brand); font-size: .9rem; }
</style>
