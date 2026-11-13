<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { fetchCategories, fetchProducts } from '@/api/catalog'
import type { CategoryTree, ProductListItem } from '@/types/api'
import { useUiStore } from '@/stores/ui'

const PAGE_SIZE = 20

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const categories = ref<CategoryTree[]>([])
const products = ref<ProductListItem[]>([])
const total = ref(0)
const loading = ref(false)
const activeId = ref<number | null>(null)
const pageNum = ref(1)
const openIds = ref<Set<number>>(new Set())

const activeName = computed(() => {
  if (!activeId.value) return '全部商品'
  const hit = findNode(categories.value, activeId.value)
  return hit?.name || '分类选购'
})

function findNode(nodes: CategoryTree[], id: number): CategoryTree | null {
  for (const n of nodes) {
    if (n.id === id) return n
    if (n.children?.length) {
      const c = findNode(n.children, id)
      if (c) return c
    }
  }
  return null
}

function toggle(id: number) {
  const next = new Set(openIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  openIds.value = next
}

function ensureOpenPath(id: number | null) {
  if (!id) return
  const next = new Set(openIds.value)
  const walk = (nodes: CategoryTree[], trail: number[]): boolean => {
    for (const n of nodes) {
      const path = [...trail, n.id]
      if (n.id === id) {
        path.slice(0, -1).forEach((x) => next.add(x))
        return true
      }
      if (n.children?.length && walk(n.children, path)) return true
    }
    return false
  }
  walk(categories.value, [])
  openIds.value = next
}

function syncFromRoute() {
  const q = Number(route.query.id)
  activeId.value = Number.isFinite(q) && q > 0 ? q : null
  const p = Number(route.query.page)
  pageNum.value = Number.isFinite(p) && p > 0 ? Math.floor(p) : 1
  ensureOpenPath(activeId.value)
}

async function loadProducts() {
  loading.value = true
  try {
    const page = await fetchProducts({
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      categoryId: activeId.value || undefined,
    })
    products.value = page.list || []
    total.value = page.total || 0
    const maxPage = Math.max(1, Math.ceil(total.value / PAGE_SIZE) || 1)
    if (pageNum.value > maxPage) {
      goPage(maxPage)
    }
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function replaceQuery(next: { id?: number | null; page?: number }) {
  const query: Record<string, string> = {}
  const id = next.id !== undefined ? next.id : activeId.value
  const page = next.page !== undefined ? next.page : pageNum.value
  if (id) query.id = String(id)
  if (page > 1) query.page = String(page)
  router.replace({ name: 'category', query })
}

function select(id: number | null) {
  activeId.value = id
  pageNum.value = 1
  if (id) {
    const node = findNode(categories.value, id)
    if (node?.children?.length) toggle(id)
  }
  replaceQuery({ id, page: 1 })
}

function goPage(page: number) {
  pageNum.value = page
  replaceQuery({ page })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  try {
    categories.value = await fetchCategories()
    categories.value.slice(0, 4).forEach((c) => openIds.value.add(c.id))
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '分类加载失败')
  }
  syncFromRoute()
  await loadProducts()
})

watch(
  () => [route.query.id, route.query.page] as const,
  async () => {
    syncFromRoute()
    await loadProducts()
  },
)
</script>

<template>
  <div class="container page">
    <aside class="side card">
      <button type="button" class="all" :class="{ on: !activeId }" @click="select(null)">
        全部分类
      </button>
      <div v-for="root in categories" :key="root.id" class="group">
        <div class="root-row">
          <button
            type="button"
            class="root"
            :class="{ on: activeId === root.id }"
            @click="select(root.id)"
          >
            {{ root.name }}
          </button>
          <button
            v-if="root.children?.length"
            type="button"
            class="caret"
            :aria-expanded="openIds.has(root.id)"
            @click="toggle(root.id)"
          >
            {{ openIds.has(root.id) ? '−' : '+' }}
          </button>
        </div>
        <div v-if="root.children?.length && openIds.has(root.id)" class="kids">
          <button
            v-for="child in root.children"
            :key="child.id"
            type="button"
            class="child"
            :class="{ on: activeId === child.id }"
            @click="select(child.id)"
          >
            {{ child.name }}
          </button>
        </div>
      </div>
    </aside>

    <section class="main">
      <header class="head">
        <div>
          <h1>{{ activeName }}</h1>
          <p class="muted">共 {{ total }} 件</p>
        </div>
        <RouterLink class="muted more" to="/search">搜索 ›</RouterLink>
      </header>
      <div v-if="loading" class="empty">加载中…</div>
      <div v-else-if="!products.length" class="empty">该分类暂无商品</div>
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
    </section>
  </div>
</template>

<style scoped>
.page {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 16px;
  padding-top: 18px;
  align-items: start;
}
.side {
  padding: 10px 8px;
  position: sticky;
  top: calc(var(--header) + 12px);
  max-height: calc(100vh - var(--header) - 24px);
  overflow: auto;
  border-radius: 10px;
}
.all, .root, .child, .caret {
  border: 0;
  background: transparent;
  color: var(--ink);
  font: inherit;
}
.all {
  display: block;
  width: 100%;
  text-align: left;
  padding: 9px 10px;
  border-radius: 8px;
  font-weight: 600;
  margin-bottom: 6px;
}
.group { margin-bottom: 2px; }
.root-row {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 2px;
}
.root {
  text-align: left;
  padding: 8px 10px;
  border-radius: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.caret {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  color: var(--muted);
}
.caret:hover { background: #f5f5f4; }
.kids {
  padding: 2px 0 6px 10px;
  display: grid;
  gap: 2px;
}
.child {
  text-align: left;
  padding: 7px 10px;
  border-radius: 8px;
  font-size: .9rem;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.all.on, .root.on, .child.on,
.all:hover, .root:hover, .child:hover {
  background: #fff7ed;
  color: var(--brand);
}
.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
h1 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}
.head .muted { margin: 4px 0 0; font-size: .86rem; }
.more:hover { color: var(--brand); }
.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
@media (max-width: 1200px) {
  .grid { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 900px) {
  .page { grid-template-columns: 1fr; }
  .side {
    position: static;
    max-height: none;
    display: flex;
    flex-wrap: nowrap;
    gap: 6px;
    overflow-x: auto;
    padding: 10px;
  }
  .all, .group { flex: 0 0 auto; }
  .group { display: flex; gap: 4px; align-items: center; }
  .root-row { display: flex; }
  .kids { display: flex; padding: 0; gap: 4px; }
  .caret { display: none; }
  .grid { grid-template-columns: repeat(3, 1fr); gap: 10px; }
}
@media (max-width: 560px) {
  .grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
