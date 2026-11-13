<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as tradeApi from '@/api/trade'
import * as memberApi from '@/api/member'
import type { Address, OrderConfirm } from '@/types/api'
import { money, placeholderPic, specsText } from '@/utils/format'
import { useUiStore } from '@/stores/ui'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const cart = useCartStore()

const confirm = ref<OrderConfirm | null>(null)
const addresses = ref<Address[]>([])
const addressId = ref<number | null>(null)
const note = ref('')
const busy = ref(false)
const showAddrForm = ref(false)
const form = ref({
  receiverName: '',
  receiverPhone: '',
  province: '北京市',
  city: '北京市',
  region: '朝阳区',
  detailAddress: '',
  isDefault: true,
})

const source = (String(route.query.source || 'CART').toUpperCase() === 'DIRECT' ? 'DIRECT' : 'CART') as 'CART' | 'DIRECT'

function directItems() {
  if (source !== 'DIRECT') return undefined
  const skuId = Number(route.query.skuId)
  const quantity = Number(route.query.quantity || 1)
  return [{ skuId, quantity }]
}

async function load() {
  const [c, a] = await Promise.all([
    tradeApi.confirmOrder({ source, items: directItems() }),
    memberApi.fetchAddresses(),
  ])
  confirm.value = c
  addresses.value = a
  addressId.value = a.find((x) => x.isDefault)?.id || a[0]?.id || null
  if (!a.length) showAddrForm.value = true
}

async function saveAddress() {
  busy.value = true
  try {
    const id = await memberApi.createAddress(form.value)
    addresses.value = await memberApi.fetchAddresses()
    addressId.value = id
    showAddrForm.value = false
    ui.toast('地址已保存')
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '保存失败')
  } finally {
    busy.value = false
  }
}

async function submit() {
  if (!confirm.value || !addressId.value) {
    ui.toast('请选择收货地址')
    return
  }
  busy.value = true
  try {
    const orderSn = await tradeApi.submitOrder({
      source,
      items: directItems(),
      addressId: addressId.value,
      note: note.value || undefined,
      submitToken: confirm.value.submitToken,
    })
    await cart.refresh()
    ui.toast('下单成功')
    router.replace({ name: 'order-detail', params: { orderSn } })
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '提交失败')
    try { await load() } catch { /* ignore */ }
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  try {
    await load()
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '确认订单失败')
  }
})
</script>

<template>
  <div class="container page">
    <h1>确认订单</h1>
    <section class="card block">
      <div class="head">
        <h2>收货地址</h2>
        <button type="button" class="link" @click="showAddrForm = !showAddrForm">
          {{ showAddrForm ? '收起' : '新增地址' }}
        </button>
      </div>
      <div v-if="showAddrForm" class="addr-form">
        <input v-model="form.receiverName" placeholder="收货人" />
        <input v-model="form.receiverPhone" placeholder="手机号" />
        <input v-model="form.province" placeholder="省" />
        <input v-model="form.city" placeholder="市" />
        <input v-model="form.region" placeholder="区" />
        <input v-model="form.detailAddress" placeholder="详细地址" />
        <button class="btn" type="button" :disabled="busy" @click="saveAddress">保存地址</button>
      </div>
      <div v-else-if="!addresses.length" class="muted">还没有地址，请先新增。</div>
      <label v-for="a in addresses" :key="a.id" class="addr">
        <input v-model="addressId" type="radio" :value="a.id" />
        <div>
          <strong>{{ a.receiverName }} {{ a.receiverPhone }}</strong>
          <p class="muted">{{ a.province }}{{ a.city }}{{ a.region }}{{ a.detailAddress }}</p>
        </div>
      </label>
    </section>

    <section v-if="confirm" class="card block">
      <h2>商品清单</h2>
      <article v-for="(item, idx) in confirm.items" :key="idx" class="goods">
        <img :src="item.pic || placeholderPic(item.skuId)" alt="" />
        <div>
          <h3>{{ item.productName }}</h3>
          <p class="muted">{{ specsText(item.specs) }} × {{ item.quantity }}</p>
          <p class="price">¥{{ money(item.price) }}</p>
        </div>
      </article>
      <textarea v-model="note" rows="2" placeholder="订单备注（可选）" />
      <div class="sum">
        <div><span>商品金额</span><span>¥{{ money(confirm.goodsAmount) }}</span></div>
        <div><span>运费</span><span>¥{{ money(confirm.freightAmount) }}</span></div>
        <div class="pay"><span>应付</span><span class="price lg">¥{{ money(confirm.payAmount) }}</span></div>
      </div>
      <button class="btn lg block" type="button" :disabled="busy" @click="submit">提交订单</button>
    </section>
  </div>
</template>

<style scoped>
.page { padding-top: 24px; max-width: 860px; }
.block { padding: 18px; margin-bottom: 16px; }
.head { display: flex; justify-content: space-between; align-items: center; }
h1 { margin: 0 0 16px; }
h2 { margin: 0 0 12px; font-size: 1.05rem; }
.link { border: 0; background: none; color: var(--brand); }
.addr {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}
.addr-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 12px;
}
.addr-form input, textarea {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 10px 12px;
  width: 100%;
}
.addr-form .btn { grid-column: 1 / -1; }
.goods {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}
.goods img {
  width: 72px; height: 72px; object-fit: cover; border-radius: 8px;
}
.goods h3 { margin: 0; font-size: .95rem; font-weight: 500; }
textarea { margin-top: 12px; }
.sum { margin: 16px 0; display: grid; gap: 8px; }
.sum div { display: flex; justify-content: space-between; }
.pay { align-items: baseline; }
@media (max-width: 640px) {
  .addr-form { grid-template-columns: 1fr; }
}
</style>
