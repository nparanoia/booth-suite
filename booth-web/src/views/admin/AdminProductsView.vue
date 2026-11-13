<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as adminApi from '@/api/admin'
import type { ProductListItem } from '@/types/api'
import { money, placeholderPic } from '@/utils/format'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const list = ref<ProductListItem[]>([])
const total = ref(0)
const keyword = ref('')
const status = ref('')
const loading = ref(false)
const selected = ref<number[]>([])

async function load() {
  loading.value = true
  try {
    const page = await adminApi.adminProducts({
      pageNum: 1,
      pageSize: 50,
      keyword: keyword.value.trim() || undefined,
      publishStatus: status.value || undefined,
    })
    list.value = page.list || []
    total.value = page.total || 0
    selected.value = []
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function toggle(id: number, on: boolean) {
  if (on) selected.value = [...new Set([...selected.value, id])]
  else selected.value = selected.value.filter((x) => x !== id)
}

async function publish(next: 'ON_SHELF' | 'OFF_SHELF') {
  if (!selected.value.length) {
    ui.toast('请先勾选商品')
    return
  }
  if (next === 'ON_SHELF') {
    const ok = window.confirm(
      `即将上架 ${selected.value.length} 件商品。请确认：标题/卖点无虚假承诺、库存与价格正确、售后政策可在店内客服知识库中说明。继续？`,
    )
    if (!ok) return
  }
  try {
    await adminApi.adminPublishProducts(selected.value, next)
    ui.toast(next === 'ON_SHELF' ? '已上架' : '已下架')
    await load()
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '操作失败')
  }
}

async function remove(id: number) {
  try {
    await adminApi.adminDeleteProduct(id)
    ui.toast('已删除')
    await load()
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '删除失败（上架商品需先下架）')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1 style="margin:0 0 16px;font-size:1.35rem">商品管理</h1>
    <div class="admin-toolbar">
      <input v-model="keyword" placeholder="搜索商品名" @keyup.enter="load" />
      <select v-model="status">
        <option value="">全部状态</option>
        <option value="ON_SHELF">上架</option>
        <option value="OFF_SHELF">下架</option>
      </select>
      <button class="btn" type="button" @click="load">查询</button>
      <button class="btn ghost" type="button" @click="publish('ON_SHELF')">批量上架</button>
      <button class="btn ghost" type="button" @click="publish('OFF_SHELF')">批量下架</button>
      <span class="muted">共 {{ total }} 件</span>
    </div>
    <div class="admin-card" style="padding:0;overflow:auto">
      <table class="admin-table">
        <thead>
          <tr>
            <th style="width:40px"></th>
            <th>商品</th>
            <th>价格</th>
            <th>销量</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="6" class="muted">加载中…</td></tr>
          <tr v-for="p in list" :key="p.id">
            <td>
              <input
                type="checkbox"
                :checked="selected.includes(p.id)"
                @change="toggle(p.id, ($event.target as HTMLInputElement).checked)"
              />
            </td>
            <td>
              <div style="display:flex;gap:10px;align-items:center">
                <img :src="p.pic || placeholderPic(p.id)" alt="" style="width:48px;height:48px;object-fit:cover;border-radius:8px" />
                <div>
                  <div>{{ p.name }}</div>
                  <div class="muted" style="font-size:.8rem">{{ p.productSn }}</div>
                </div>
              </div>
            </td>
            <td>¥{{ money(p.price) }}</td>
            <td>{{ p.sale }}</td>
            <td>
              <span class="tag" :class="p.publishStatus === 'ON_SHELF' ? 'on' : 'off'">
                {{ p.publishStatus === 'ON_SHELF' ? '上架' : '下架' }}
              </span>
            </td>
            <td>
              <button class="btn ghost" type="button" style="padding:4px 10px" @click="remove(p.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && !list.length"><td colspan="6" class="muted">暂无商品</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
