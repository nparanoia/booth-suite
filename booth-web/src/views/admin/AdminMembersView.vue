<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as adminApi from '@/api/admin'
import type { MemberAdmin } from '@/api/admin'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const list = ref<MemberAdmin[]>([])
const total = ref(0)
const keyword = ref('')
const status = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await adminApi.adminMembers({
      pageNum: 1,
      pageSize: 50,
      keyword: keyword.value.trim() || undefined,
      status: status.value || undefined,
    })
    list.value = page.list || []
    total.value = page.total || 0
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function setStatus(m: MemberAdmin, next: 'ENABLED' | 'DISABLED') {
  try {
    await adminApi.adminUpdateMemberStatus(m.id, next)
    ui.toast(next === 'ENABLED' ? '已启用' : '已禁用')
    await load()
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '操作失败')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1 style="margin:0 0 16px;font-size:1.35rem">会员管理</h1>
    <div class="admin-toolbar">
      <input v-model="keyword" placeholder="用户名 / 手机号" @keyup.enter="load" />
      <select v-model="status">
        <option value="">全部状态</option>
        <option value="ENABLED">启用</option>
        <option value="DISABLED">禁用</option>
      </select>
      <button class="btn" type="button" @click="load">查询</button>
      <span class="muted">共 {{ total }} 人</span>
    </div>
    <div class="admin-card" style="padding:0;overflow:auto">
      <table class="admin-table">
        <thead>
          <tr><th>ID</th><th>用户</th><th>手机</th><th>积分</th><th>状态</th><th>注册时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="muted">加载中…</td></tr>
          <tr v-for="m in list" :key="m.id">
            <td>{{ m.id }}</td>
            <td>{{ m.nickname || m.username }}</td>
            <td>{{ m.phone }}</td>
            <td>{{ m.integration }}</td>
            <td>
              <span class="tag" :class="m.status === 'ENABLED' ? 'on' : 'off'">
                {{ m.status === 'ENABLED' ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="muted">{{ m.createTime }}</td>
            <td>
              <button
                v-if="m.status === 'ENABLED'"
                class="btn ghost"
                type="button"
                style="padding:4px 10px"
                @click="setStatus(m, 'DISABLED')"
              >
                禁用
              </button>
              <button
                v-else
                class="btn ghost"
                type="button"
                style="padding:4px 10px"
                @click="setStatus(m, 'ENABLED')"
              >
                启用
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
