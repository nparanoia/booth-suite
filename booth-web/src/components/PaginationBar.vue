<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  pageNum: number
  pageSize: number
  total: number
}>(), {
  pageNum: 1,
  pageSize: 20,
  total: 0,
})

const emit = defineEmits<{
  change: [page: number]
}>()

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const pages = computed(() => {
  const cur = props.pageNum
  const max = pageCount.value
  const set = new Set<number>([1, max, cur - 1, cur, cur + 1])
  return [...set]
    .filter((n) => n >= 1 && n <= max)
    .sort((a, b) => a - b)
})

const from = computed(() => (props.total === 0 ? 0 : (props.pageNum - 1) * props.pageSize + 1))
const to = computed(() => Math.min(props.total, props.pageNum * props.pageSize))

function go(p: number) {
  if (p < 1 || p > pageCount.value || p === props.pageNum) return
  emit('change', p)
}
</script>

<template>
  <nav v-if="total > 0" class="pager" aria-label="分页">
    <span class="meta muted">{{ from }}-{{ to }} / {{ total }}</span>
    <div class="btns">
      <button type="button" class="nav" :disabled="pageNum <= 1" @click="go(pageNum - 1)">上一页</button>
      <template v-for="(p, i) in pages" :key="p">
        <span v-if="i > 0 && p - pages[i - 1]! > 1" class="dots">…</span>
        <button type="button" class="num" :class="{ on: p === pageNum }" @click="go(p)">{{ p }}</button>
      </template>
      <button type="button" class="nav" :disabled="pageNum >= pageCount" @click="go(pageNum + 1)">下一页</button>
    </div>
  </nav>
</template>

<style scoped>
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
  padding: 4px 0 8px;
}
.meta { font-size: .86rem; }
.btns {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.nav, .num {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 8px;
  padding: 7px 12px;
  color: var(--ink);
  font: inherit;
  font-size: .88rem;
}
.num.on {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}
.nav:hover:not(:disabled),
.num:hover:not(.on) {
  border-color: var(--brand);
  color: var(--brand);
}
.nav:disabled {
  opacity: .4;
  cursor: not-allowed;
}
.dots {
  color: var(--muted);
  padding: 0 2px;
}
</style>
