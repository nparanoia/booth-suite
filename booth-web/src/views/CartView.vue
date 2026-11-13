<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as tradeApi from '@/api/trade'
import { useCartStore } from '@/stores/cart'
import { useUiStore } from '@/stores/ui'
import { money, placeholderPic, specsText } from '@/utils/format'

const cart = useCartStore()
const ui = useUiStore()
const router = useRouter()
const busy = ref(false)

onMounted(() => cart.refresh())

async function toggle(id: number, checked: boolean) {
  await tradeApi.updateCartChecked(checked, [id])
  await cart.refresh()
}

async function toggleAll(checked: boolean) {
  await tradeApi.updateCartChecked(checked)
  await cart.refresh()
}

async function changeQty(id: number, quantity: number) {
  if (quantity < 1) return
  await tradeApi.updateCartQuantity(id, quantity)
  await cart.refresh()
}

async function remove(id: number) {
  await tradeApi.deleteCartItems([id])
  await cart.refresh()
  ui.toast('已删除')
}

async function checkout() {
  if (!cart.cart?.checkedCount) {
    ui.toast('请先勾选商品')
    return
  }
  busy.value = true
  try {
    await tradeApi.confirmOrder({ source: 'CART' })
    router.push({ name: 'checkout', query: { source: 'CART' } })
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '结算失败')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="container page">
    <h1>购物车</h1>
    <div v-if="cart.loading" class="empty">加载中…</div>
    <div v-else-if="!cart.cart?.items?.length" class="empty">
      购物车是空的，<RouterLink to="/">去首页逛逛</RouterLink>
    </div>
    <template v-else>
      <label class="all">
        <input
          type="checkbox"
          :checked="cart.cart.items.every((i) => i.checked || !i.available)"
          @change="toggleAll(($event.target as HTMLInputElement).checked)"
        />
        全选
      </label>
      <div class="list">
        <article v-for="item in cart.cart.items" :key="item.id" class="row card" :class="{ dead: !item.available }">
          <input type="checkbox" :checked="item.checked" :disabled="!item.available" @change="toggle(item.id, ($event.target as HTMLInputElement).checked)" />
          <img :src="item.pic || placeholderPic(item.productId)" :alt="item.productName" />
          <div class="meta">
            <h3>{{ item.productName }}</h3>
            <p class="muted">{{ specsText(item.specs) || '默认规格' }}</p>
            <p v-if="!item.available" class="warn">已失效</p>
            <div class="ops">
              <span class="price"><span class="yen">¥</span>{{ money(item.price) }}</span>
              <div class="stepper">
                <button type="button" @click="changeQty(item.id, item.quantity - 1)">−</button>
                <span>{{ item.quantity }}</span>
                <button type="button" @click="changeQty(item.id, item.quantity + 1)">+</button>
              </div>
              <button type="button" class="link" @click="remove(item.id)">删除</button>
            </div>
          </div>
        </article>
      </div>
      <div class="bar card">
        <div>
          已选 {{ cart.cart.checkedCount }} 件，合计
          <span class="price lg"><span class="yen">¥</span>{{ money(cart.cart.checkedAmount) }}</span>
        </div>
        <button class="btn lg" type="button" :disabled="busy" @click="checkout">去结算</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page { padding-top: 24px; }
h1 { margin: 0 0 16px; }
.all { display: inline-flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.list { display: grid; gap: 12px; }
.row {
  display: grid;
  grid-template-columns: auto 96px 1fr;
  gap: 14px;
  align-items: center;
  padding: 14px;
}
.row.dead { opacity: .55; }
.row img {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: 10px;
  background: #fafafa;
}
.meta h3 { margin: 0 0 6px; font-size: 1rem; font-weight: 500; }
.warn { color: var(--danger); margin: 4px 0 0; }
.ops {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.stepper {
  display: inline-flex;
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
}
.stepper button, .stepper span {
  width: 32px; height: 32px; border: 0; background: #fff;
  display: grid; place-items: center;
}
.link { border: 0; background: none; color: var(--muted); }
.bar {
  position: sticky;
  bottom: 12px;
  margin-top: 18px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
@media (max-width: 640px) {
  .row { grid-template-columns: auto 72px 1fr; }
  .row img { width: 72px; height: 72px; }
}
</style>
