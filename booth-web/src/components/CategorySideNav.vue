<script setup lang="ts">
import { computed, ref } from 'vue'
import type { CategoryTree } from '@/types/api'

const props = defineProps<{
  categories: CategoryTree[]
}>()

const activeId = ref<number | null>(null)

const roots = computed(() => props.categories.slice(0, 10))

const active = computed(() => roots.value.find((c) => c.id === activeId.value) || null)

const panelChildren = computed(() => {
  if (!active.value) return [] as CategoryTree[]
  return active.value.children?.length ? active.value.children : [active.value]
})

function open(id: number) {
  activeId.value = id
}

function close() {
  activeId.value = null
}
</script>

<template>
  <aside class="side" @mouseleave="close">
    <div v-if="!roots.length" class="empty-hint muted">暂无分类</div>
    <ul v-else class="list">
      <li
        v-for="c in roots"
        :key="c.id"
        :class="{ on: activeId === c.id }"
        @mouseenter="open(c.id)"
      >
        <RouterLink
          class="row"
          :to="{ name: 'category', query: { id: c.id } }"
        >
          <span class="name">{{ c.name }}</span>
          <span class="arrow">›</span>
        </RouterLink>
      </li>
    </ul>

    <div v-if="active" class="panel" @mouseenter="open(active.id)">
      <div class="panel-head">
        <strong>{{ active.name }}</strong>
        <RouterLink :to="{ name: 'category', query: { id: active.id } }">
          查看全部 ›
        </RouterLink>
      </div>
      <div class="panel-grid">
        <RouterLink
          v-for="child in panelChildren"
          :key="child.id"
          class="chip"
          :to="{ name: 'category', query: { id: child.id } }"
        >
          {{ child.name }}
        </RouterLink>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.side {
  position: relative;
  width: 234px;
  flex-shrink: 0;
  background: rgba(28, 25, 23, 0.72);
  color: #fff;
  z-index: 3;
}
.list {
  list-style: none;
  margin: 0;
  padding: 12px 0;
  height: 100%;
  box-sizing: border-box;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 18px 11px 22px;
  color: #fff;
  font-size: .92rem;
}
.list li.on .row,
.list li:hover .row {
  background: rgba(255, 255, 255, 0.1);
  color: #ffd6b8;
}
.name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 8px;
}
.arrow { opacity: .55; font-size: 1.1rem; line-height: 1; }
.empty-hint {
  padding: 24px 18px;
  color: rgba(255,255,255,.7);
  font-size: .9rem;
}
.panel {
  position: absolute;
  left: 100%;
  top: 0;
  bottom: 0;
  width: min(520px, 48vw);
  background: #fff;
  color: var(--ink);
  box-shadow: 8px 0 28px rgba(0,0,0,.12);
  padding: 20px 22px;
  z-index: 4;
}
.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--line);
}
.panel-head a {
  color: var(--muted);
  font-size: .85rem;
}
.panel-head a:hover { color: var(--brand); }
.panel-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px 12px;
}
.chip {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f5f5f4;
  font-size: .88rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chip:hover {
  background: #fff7ed;
  color: var(--brand);
}
@media (max-width: 900px) {
  .side { display: none; }
}
</style>
