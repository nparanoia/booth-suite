<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as adminApi from '@/api/admin'
import type { ReturnApply } from '@/api/admin'
import { money } from '@/utils/format'
import { useUiStore } from '@/stores/ui'

const RETURN_STATUS: Record<string, string> = {
  PENDING: '待处理',
  RETURNING: '退货中',
  COMPLETED: '已完成',
  REJECTED: '已拒绝',
}

const ui = useUiStore()
const list = ref<ReturnApply[]>([])
const status = ref('')
const loading = ref(false)
const busy = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = (await adminApi.adminReturns(status.value || undefined)) || []
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function act(kind: 'approve' | 'reject' | 'receive', id: number) {
  busy.value = true
  try {
    if (kind === 'approve') await adminApi.adminApproveReturn(id, '后台同意')
    if (kind === 'reject') await adminApi.adminRejectReturn(id, '不符合退货条件')
    if (kind === 'receive') await adminApi.adminReceiveReturn(id, '已收到退货')
    ui.toast('已处理')
    await load()
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '处理失败')
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1 style="margin:0 0 16px;font-size:1.35rem">售后管理</h1>
    <div class="admin-toolbar">
      <select v-model="status" @change="load">
        <option value="">全部</option>
        <option value="PENDING">待处理</option>
        <option value="RETURNING">退货中</option>
        <option value="COMPLETED">已完成</option>
        <option value="REJECTED">已拒绝</option>
      </select>
      <button class="btn" type="button" @click="load">刷新</button>
    </div>
    <div class="admin-card" style="padding:0;overflow:auto">
      <table class="admin-table">
        <thead>
          <tr><th>ID</th><th>订单</th><th>原因</th><th>金额</th><th>状态</th><th>时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="muted">加载中…</td></tr>
          <tr v-for="r in list" :key="r.id">
            <td>{{ r.id }}</td>
            <td>{{ r.orderSn }}</td>
            <td>{{ r.reason }}</td>
            <td>¥{{ money(r.returnAmount) }}</td>
            <td><span class="tag warn">{{ RETURN_STATUS[r.status] || r.status }}</span></td>
            <td class="muted">{{ r.createTime }}</td>
            <td style="display:flex;gap:6px;flex-wrap:wrap">
              <button v-if="r.status === 'PENDING'" class="btn" type="button" style="padding:4px 10px" :disabled="busy" @click="act('approve', r.id)">同意</button>
              <button v-if="r.status === 'PENDING'" class="btn ghost" type="button" style="padding:4px 10px" :disabled="busy" @click="act('reject', r.id)">拒绝</button>
              <button v-if="r.status === 'RETURNING'" class="btn" type="button" style="padding:4px 10px" :disabled="busy" @click="act('receive', r.id)">确认收货</button>
            </td>
          </tr>
          <tr v-if="!loading && !list.length"><td colspan="7" class="muted">暂无售后单</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
