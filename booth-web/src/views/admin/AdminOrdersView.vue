<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import * as adminApi from '@/api/admin'
import type { OrderDetail, OrderSummary } from '@/types/api'
import { money, orderStatusLabel, specsText } from '@/utils/format'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const ui = useUiStore()
const list = ref<OrderSummary[]>([])
const status = ref('')
const orderSn = ref('')
const loading = ref(false)
const detail = ref<OrderDetail | null>(null)
const shipForm = ref({ deliveryCompany: 'Booth 快递', deliverySn: '' })
const busy = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await adminApi.adminOrders({
      pageNum: 1,
      pageSize: 50,
      status: status.value || undefined,
      orderSn: orderSn.value.trim() || undefined,
    })
    list.value = page.list || []
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(sn: string) {
  try {
    detail.value = await adminApi.adminOrderDetail(sn)
    shipForm.value.deliverySn = `QL${Date.now().toString().slice(-10)}`
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '详情失败')
  }
}

async function ship() {
  if (!detail.value) return
  busy.value = true
  try {
    await adminApi.adminShipOrder(
      detail.value.orderSn,
      shipForm.value.deliveryCompany,
      shipForm.value.deliverySn,
    )
    ui.toast('已发货')
    await openDetail(detail.value.orderSn)
    await load()
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '发货失败')
  } finally {
    busy.value = false
  }
}

async function close() {
  if (!detail.value) return
  busy.value = true
  try {
    await adminApi.adminCloseOrder(detail.value.orderSn)
    ui.toast('已关闭')
    await openDetail(detail.value.orderSn)
    await load()
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '关闭失败')
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  if (route.query.sn) orderSn.value = String(route.query.sn)
  await load()
  if (route.query.sn) await openDetail(String(route.query.sn))
})

watch(() => route.query.sn, async (sn) => {
  if (sn) {
    orderSn.value = String(sn)
    await openDetail(String(sn))
  }
})
</script>

<template>
  <div>
    <h1 style="margin:0 0 16px;font-size:1.35rem">订单管理</h1>
    <div class="admin-toolbar">
      <input v-model="orderSn" placeholder="订单号" @keyup.enter="load" />
      <select v-model="status">
        <option value="">全部状态</option>
        <option value="WAIT_PAY">待付款</option>
        <option value="WAIT_DELIVER">待发货</option>
        <option value="DELIVERED">已发货</option>
        <option value="COMPLETED">已完成</option>
        <option value="CLOSED">已关闭</option>
      </select>
      <button class="btn" type="button" @click="load">查询</button>
    </div>

    <div class="layout">
      <div class="admin-card" style="padding:0;overflow:auto">
        <table class="admin-table">
          <thead>
            <tr><th>订单号</th><th>状态</th><th>金额</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="4" class="muted">加载中…</td></tr>
            <tr v-for="o in list" :key="o.orderSn">
              <td>{{ o.orderSn }}</td>
              <td>{{ orderStatusLabel(o.status) }}</td>
              <td>¥{{ money(o.payAmount) }}</td>
              <td><button class="btn ghost" type="button" style="padding:4px 10px" @click="openDetail(o.orderSn)">详情</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <aside v-if="detail" class="admin-card">
        <h2 style="margin:0 0 8px;font-size:1.05rem">{{ detail.orderSn }}</h2>
        <p><span class="tag warn">{{ orderStatusLabel(detail.status) }}</span></p>
        <p class="muted">{{ detail.receiverName }} {{ detail.receiverPhone }}</p>
        <p class="muted">
          {{ detail.receiverProvince }}{{ detail.receiverCity }}{{ detail.receiverRegion }}{{ detail.receiverDetailAddress }}
        </p>
        <div v-for="(item, i) in detail.items" :key="i" style="padding:8px 0;border-bottom:1px solid var(--line)">
          <div>{{ item.productName }}</div>
          <div class="muted">{{ specsText(item.specs) }} × {{ item.quantity }} · ¥{{ money(item.price) }}</div>
        </div>
        <p style="margin-top:12px">应付 <strong class="price">¥{{ money(detail.payAmount) }}</strong></p>

        <div v-if="detail.status === 'WAIT_DELIVER'" class="ship">
          <input v-model="shipForm.deliveryCompany" placeholder="物流公司" />
          <input v-model="shipForm.deliverySn" placeholder="运单号" />
          <button class="btn" type="button" :disabled="busy" @click="ship">确认发货</button>
        </div>
        <button
          v-if="detail.status === 'WAIT_PAY'"
          class="btn ghost"
          type="button"
          style="margin-top:10px"
          :disabled="busy"
          @click="close"
        >
          关闭待付款订单
        </button>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 1.2fr .8fr;
  gap: 14px;
  align-items: start;
}
.ship {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}
.ship input {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 8px 10px;
}
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
}
</style>
