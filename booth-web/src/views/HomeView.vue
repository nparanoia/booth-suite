<script setup lang="ts">
import { onMounted, ref } from 'vue'
import ProductCard from '@/components/ProductCard.vue'
import HomeCarousel from '@/components/HomeCarousel.vue'
import CategorySideNav from '@/components/CategorySideNav.vue'
import { fetchCategories, fetchProducts } from '@/api/catalog'
import { fetchReceivableCoupons, fetchSeckillSessions } from '@/api/promo'
import type { CategoryTree, ProductListItem } from '@/types/api'
import { useUiStore } from '@/stores/ui'
import { money } from '@/utils/format'

const ui = useUiStore()
const products = ref<ProductListItem[]>([])
const hot = ref<ProductListItem[]>([])
const categories = ref<CategoryTree[]>([])
const loading = ref(true)
const error = ref('')
const couponHint = ref('')
const seckillHint = ref('')

onMounted(async () => {
  try {
    const [cats, page, coupons, sessions] = await Promise.all([
      fetchCategories(),
      fetchProducts({ pageNum: 1, pageSize: 20 }),
      fetchReceivableCoupons().catch(() => []),
      fetchSeckillSessions().catch(() => []),
    ])
    categories.value = cats || []
    products.value = page.list || []
    hot.value = [...(page.list || [])].sort((a, b) => (b.sale || 0) - (a.sale || 0))
    if (coupons?.length) {
      const c = coupons[0]
      couponHint.value = `${c.name} · 满${money(c.minPoint)}减${money(c.amount)}`
    }
    const ongoing = sessions?.find((s) => s.ongoing) || sessions?.[0]
    if (ongoing) {
      seckillHint.value = `${ongoing.name} · ${ongoing.skus?.length || 0} 款在抢`
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
    ui.toast(error.value)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="hero-band">
    <div class="container-bleed hero-wrap">
      <div class="hero">
        <CategorySideNav :categories="categories" />
        <HomeCarousel
          :products="hot"
          :coupon-hint="couponHint"
          :seckill-hint="seckillHint"
        />
      </div>
      <div v-if="categories.length" class="mobile-cats">
        <RouterLink
          v-for="c in categories.slice(0, 8)"
          :key="c.id"
          class="m-cat"
          :to="{ name: 'category', query: { id: c.id } }"
        >
          {{ c.name }}
        </RouterLink>
        <RouterLink class="m-cat more" to="/category">全部分类</RouterLink>
      </div>
    </div>
  </section>

  <section class="container section">
    <div class="section-head">
      <h2>本店货架</h2>
      <RouterLink to="/search">更多</RouterLink>
    </div>
    <div v-if="loading" class="empty">加载中…</div>
    <div v-else-if="error && !products.length" class="empty">{{ error }}</div>
    <div v-else-if="!products.length" class="empty">暂无上架商品</div>
    <div v-else class="grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" dense />
    </div>
  </section>
</template>

<style scoped>
.hero-band {
  width: 100%;
  padding: 12px 0 0;
  background:
    linear-gradient(180deg, #eceae7 0%, transparent 100%);
}
.hero-wrap {
  padding-top: 0;
}
.hero {
  display: flex;
  height: clamp(380px, 42vw, 480px);
  border-radius: 6px;
  overflow: hidden;
  background: #111;
  box-shadow: var(--shadow);
}
.mobile-cats {
  display: none;
  margin-top: 12px;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: thin;
}
.section {
  padding-top: 22px;
  padding-bottom: 8px;
}
.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}
.section-head h2 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
}
.section-head a { color: var(--muted); }
.section-head a:hover { color: var(--brand); }
.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
@media (max-width: 1100px) {
  .grid { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 900px) {
  .hero { height: auto; min-height: 280px; }
  .mobile-cats { display: flex; }
  .m-cat {
    flex: 0 0 auto;
    padding: 8px 12px;
    border-radius: 999px;
    background: #fff;
    box-shadow: var(--shadow);
    font-size: .82rem;
    max-width: 11em;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .m-cat.more { color: var(--brand); }
  .grid { grid-template-columns: repeat(3, 1fr); gap: 10px; }
}
@media (max-width: 560px) {
  .grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
