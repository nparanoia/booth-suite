import { deleteData, getData, postData, putData } from './http'
import type { Cart, OrderConfirm, OrderDetail, OrderSummary, PageResult } from '@/types/api'

export function fetchCart() {
  return getData<Cart>('/trade/app/cart')
}

export function addCartItem(skuId: number, quantity: number) {
  return postData<void>('/trade/app/cart/items', { skuId, quantity })
}

export function updateCartQuantity(id: number, quantity: number) {
  return putData<void>(`/trade/app/cart/items/${id}/quantity`, { quantity })
}

export function updateCartChecked(checked: boolean, itemIds?: number[]) {
  return putData<void>('/trade/app/cart/items/checked', { checked, itemIds })
}

export function deleteCartItems(ids: number[]) {
  return deleteData<void>('/trade/app/cart/items', { ids })
}

export function confirmOrder(payload: {
  source: 'CART' | 'DIRECT'
  items?: { skuId: number; quantity: number }[]
  memberCouponId?: number
  useIntegration?: number
}) {
  return postData<OrderConfirm>('/trade/app/orders/confirm', payload)
}

export function submitOrder(payload: {
  source: 'CART' | 'DIRECT'
  items?: { skuId: number; quantity: number }[]
  addressId: number
  memberCouponId?: number
  useIntegration?: number
  note?: string
  submitToken: string
}) {
  return postData<string>('/trade/app/orders', payload)
}

export function fetchOrders(params?: { pageNum?: number; pageSize?: number; status?: string }) {
  return getData<PageResult<OrderSummary>>('/trade/app/orders', params)
}

export function fetchOrder(orderSn: string) {
  return getData<OrderDetail>(`/trade/app/orders/${orderSn}`)
}

export function cancelOrder(orderSn: string) {
  return postData<void>(`/trade/app/orders/${orderSn}/cancel`)
}

/** 发起支付：返回 mock://pay/{sn} 或支付宝表单 HTML，订单状态仍待付款 */
export function payOrder(orderSn: string) {
  return postData<string>(`/trade/app/orders/${orderSn}/pay`)
}

/** 模拟收银台确认（仅后端开启 mock 支付时） */
export function confirmMockPay(orderSn: string) {
  return postData<void>(`/trade/app/orders/${orderSn}/pay/mock-confirm`)
}

export function receiveOrder(orderSn: string) {
  return postData<void>(`/trade/app/orders/${orderSn}/receive`)
}
