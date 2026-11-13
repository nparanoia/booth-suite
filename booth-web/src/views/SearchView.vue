<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { fetchProducts } from '@/api/catalog'
import type { ProductListItem } from '@/types/api'
import { useUiStore } from '@/stores/ui'

const PAGE_SIZE = 20

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const keyword = ref(String(route.query.q || ''))
const products = ref<ProductListItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)

function syncFromRoute() {
  keyword.value = String(route.query.q || '')
  const p = Number(route.query.page)
  pageNum.value = Number.isFinite(p) && p > 0 ? Math.floor(p) : 1
}

async function load() {
  loading.value = true
  try {
    const page = await fetchProducts({
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      keyword: keyword.value.trim() || undefined,
    })
    products.value = page.list || []
    total.value = page.total || 0
    const maxPage = Math.max(1, Math.ceil(total.value / PAGE_SIZE) || 1)
    if (pageNum.value > maxPage) {
      goPage(maxPage)
    }
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '搜索失败')
  } finally {
    loading.value = false
  }
}

function replaceQuery(next: { q?: string; page?: number }) {
  const q = next.q !== undefined ? next.q : keyword.value.trim()
  const page = next.page !== undefined ? next.page : pageNum.value
  const query: Record<string, string> = {}
  if (q) query.q = q
  if (page > 1) query.page = String(page)
  router.replace({ name: 'search', query })
}

function submit() {
  pageNum.value = 1
  replaceQuery({ q: keyword.value.trim(), page: 1 })
}

function goPage(page: number) {
  pageNum.value = page
  replaceQuery({ page })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  syncFromRoute()
  load()
})

watch(
  () => [route.query.q, route.query.page] as const,
  () => {
    syncFromRoute()
    load()
  },
)
</script>

<template>
  <div class="container page">
    <form class="bar" @submit.prevent="submit">
      <input v-model="keyword" type="search" placeholder="输入关键词搜索" />
      <button class="btn" type="submit">搜索</button>
    </form>
    <p class="muted">共 {{ total }} 件相关商品</p>
    <div v-if="loading" class="empty">搜索中…</div>
    <div v-else-if="!products.length" class="empty">没有找到商品</div>
    <template v-else>
      <div class="grid">
        <ProductCard v-for="p in products" :key="p.id" :product="p" dense />
      </div>
      <PaginationBar
        :page-num="pageNum"
        :page-size="PAGE_SIZE"
        :total="total"
        @change="goPage"
      />
    </template>
  </div>
</template>

<style scoped>
.page { padding-top: 18px; }
.bar {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}
.bar input {
  flex: 1;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 12px 16px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-top: 12px;
}
@media (max-width: 1200px) {
  .grid { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 900px) {
  .grid { grid-template-columns: repeat(3, 1fr); gap: 10px; }
}
@media (max-width: 560px) {
  .grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
