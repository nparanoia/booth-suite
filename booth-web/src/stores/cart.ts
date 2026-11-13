import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as tradeApi from '@/api/trade'
import type { Cart } from '@/types/api'
import { useUserStore } from './user'

export const useCartStore = defineStore('cart', () => {
  const cart = ref<Cart | null>(null)
  const loading = ref(false)

  const count = computed(() =>
    (cart.value?.items || []).reduce((sum, item) => sum + item.quantity, 0),
  )

  async function refresh() {
    const user = useUserStore()
    if (!user.loggedIn) {
      cart.value = null
      return
    }
    loading.value = true
    try {
      cart.value = await tradeApi.fetchCart()
    } finally {
      loading.value = false
    }
  }

  async function add(skuId: number, quantity = 1) {
    await tradeApi.addCartItem(skuId, quantity)
    await refresh()
  }

  return { cart, loading, count, refresh, add }
})
