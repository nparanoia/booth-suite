<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchSeckillSessions, type SeckillSession } from '@/api/promo'
import { money, placeholderPic } from '@/utils/format'
import { useUiStore } from '@/stores/ui'

const sessions = ref<SeckillSession[]>([])
const loading = ref(true)
const ui = useUiStore()

onMounted(async () => {
  try {
    sessions.value = (await fetchSeckillSessions()) || []
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '秒杀加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container page">
    <h1>限时秒杀</h1>
    <p class="muted">展示当前场次；正式抢购需登录并完成下单地址校验。</p>
    <div v-if="loading" class="empty">加载中…</div>
    <div v-else-if="!sessions.length" class="empty">当前没有秒杀场次</div>
    <section v-for="s in sessions" :key="s.id" class="block">
      <div class="head">
        <h2>{{ s.name }}</h2>
        <span class="tag" :class="s.ongoing ? 'on' : 'warn'">
          {{ s.ongoing ? '进行中' : '未开始/已结束' }}
        </span>
        <span class="muted">{{ s.startTime }} - {{ s.endTime }}</span>
      </div>
      <div v-if="!s.skus?.length" class="muted">本场暂无商品</div>
      <div v-else class="grid">
        <article v-for="sku in s.skus" :key="sku.id" class="card item">
          <img :src="sku.productPic || sku.pic || placeholderPic(sku.id)" alt="" />
          <h3>{{ sku.productName || `秒杀商品 #${sku.id}` }}</h3>
          <p class="price">¥{{ money(sku.seckillPrice ?? 0) }}</p>
          <p class="muted">剩余 {{ sku.remainStock ?? sku.seckillStock ?? '—' }} · 限购 {{ sku.perLimit ?? '—' }}</p>
          <RouterLink class="btn block" to="/login">登录后去抢</RouterLink>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page { padding-top: 24px; }
h1 { margin: 0 0 6px; }
.block { margin-top: 22px; }
.head {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: baseline;
  margin-bottom: 12px;
}
.head h2 { margin: 0; font-size: 1.15rem; }
.tag {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: .78rem;
  background: #f3f4f6;
}
.tag.on { background: #ecfdf5; color: #047857; }
.tag.warn { background: #fff7ed; color: #c2410c; }
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.item { padding: 12px; }
.item img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 10px;
  background: #fafafa;
}
h3 {
  margin: 10px 0 6px;
  font-size: .95rem;
  font-weight: 500;
  min-height: 2.6em;
}
.origin { text-decoration: line-through; margin-left: 6px; font-weight: 400; }
@media (max-width: 900px) {
  .grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
