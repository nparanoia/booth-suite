<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchProduct } from '@/api/catalog'
import type { ProductDetail, Sku } from '@/types/api'
import { money, placeholderPic, specsText } from '@/utils/format'
import { sanitizeHtml } from '@/utils/sanitize'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { useUiStore } from '@/stores/ui'
import { confirmOrder } from '@/api/trade'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const user = useUserStore()
const ui = useUiStore()

const product = ref<ProductDetail | null>(null)
const sku = ref<Sku | null>(null)
const qty = ref(1)
const busy = ref(false)

const pics = computed(() => {
  if (!product.value) return []
  const list = [product.value.pic, ...(product.value.albumPics || [])].filter(Boolean) as string[]
  return list.length ? list : [placeholderPic(product.value.id)]
})
const mainPic = computed(() => sku.value?.pic || pics.value[0])
const safeDetailHtml = computed(() => sanitizeHtml(product.value?.detailHtml))

async function load() {
  const id = Number(route.params.id)
  product.value = await fetchProduct(id)
  sku.value = product.value.skus?.[0] || null
  qty.value = 1
}

async function ensureLogin() {
  if (!user.loggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

async function addToCart() {
  if (!sku.value || !(await ensureLogin())) return
  busy.value = true
  try {
    await cart.add(sku.value.id, qty.value)
    ui.toast('已加入购物车')
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '加购失败')
  } finally {
    busy.value = false
  }
}

async function buyNow() {
  if (!sku.value || !(await ensureLogin())) return
  busy.value = true
  try {
    await confirmOrder({
      source: 'DIRECT',
      items: [{ skuId: sku.value.id, quantity: qty.value }],
    })
    router.push({
      name: 'checkout',
      query: { source: 'DIRECT', skuId: String(sku.value.id), quantity: String(qty.value) },
    })
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '无法结算')
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  try {
    await load()
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '商品不存在')
  }
})
watch(() => route.params.id, load)
</script>

<template>
  <div v-if="product" class="container page">
    <div class="gallery card">
      <img :src="mainPic || placeholderPic(product.id)" :alt="product.name" />
    </div>
    <div class="info">
      <p class="muted">{{ product.brandName || '本店自营' }} · {{ product.categoryName || '商品' }}</p>
      <h1>{{ product.name }}</h1>
      <p v-if="product.subTitle" class="sub">{{ product.subTitle }}</p>
      <div class="price-box">
        <span class="price lg"><span class="yen">¥</span>{{ money(sku?.price ?? product.price) }}</span>
        <span v-if="product.originalPrice" class="origin muted">¥{{ money(product.originalPrice) }}</span>
      </div>

      <div v-if="product.skus?.length" class="skus">
        <p>规格</p>
        <div class="sku-list">
          <button
            v-for="s in product.skus"
            :key="s.id"
            type="button"
            :class="{ on: sku?.id === s.id }"
            @click="sku = s"
          >
            {{ specsText(s.specs) || s.skuCode }}
          </button>
        </div>
      </div>

      <div class="qty">
        <p>数量</p>
        <div class="stepper">
          <button type="button" @click="qty = Math.max(1, qty - 1)">−</button>
          <span>{{ qty }}</span>
          <button type="button" @click="qty = Math.min(99, qty + 1)">+</button>
        </div>
        <span class="muted">库存 {{ sku?.availableStock ?? '—' }}</span>
      </div>

      <div class="actions">
        <button class="btn lg" type="button" :disabled="busy || !sku" @click="buyNow">立即购买</button>
        <button class="btn ghost lg" type="button" :disabled="busy || !sku" @click="addToCart">加入购物车</button>
      </div>
    </div>

    <section class="detail card">
      <h2>商品详情</h2>
      <div v-if="safeDetailHtml" class="html" v-html="safeDetailHtml" />
      <p v-else class="muted">暂无图文详情</p>
      <ul v-if="product.params?.length" class="params">
        <li v-for="p in product.params" :key="p.attrId">
          <span>{{ p.name }}</span>
          <strong>{{ p.value }}</strong>
        </li>
      </ul>
    </section>
  </div>
  <div v-else class="empty">加载商品中…</div>
</template>

<style scoped>
.page {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
  padding-top: 28px;
}
.gallery {
  overflow: hidden;
  aspect-ratio: 1;
}
.gallery img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
h1 { margin: 8px 0; font-size: clamp(1.5rem, 3vw, 2rem); }
.sub { color: var(--muted); }
.price-box { display: flex; align-items: baseline; gap: 12px; margin: 18px 0; }
.origin { text-decoration: line-through; }
.skus, .qty { margin: 18px 0; }
.skus p, .qty p { margin: 0 0 8px; color: var(--muted); }
.sku-list { display: flex; flex-wrap: wrap; gap: 8px; }
.sku-list button {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 8px;
  padding: 8px 12px;
}
.sku-list button.on {
  border-color: var(--brand);
  color: var(--brand);
  background: #fff7f0;
}
.stepper {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
  margin-right: 12px;
}
.stepper button, .stepper span {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 0;
  background: #fff;
}
.actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 24px; }
.detail {
  grid-column: 1 / -1;
  padding: 24px;
}
.detail h2 { margin: 0 0 12px; }
.html :deep(img) { max-width: 100%; }
.params { list-style: none; padding: 0; margin: 16px 0 0; }
.params li {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}
@media (max-width: 860px) {
  .page { grid-template-columns: 1fr; }
}
</style>
