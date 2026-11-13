import { getData, postData } from './http'

export interface CouponItem {
  id: number
  name: string
  amount: number
  minPoint: number
  remainCount: number | null
  perLimit: number | null
  note: string | null
}

export interface SeckillSku {
  id: number
  activityId?: number
  sessionId?: number
  skuId?: number
  productName?: string
  productPic?: string | null
  pic?: string | null
  seckillPrice?: number
  seckillStock?: number
  remainStock?: number
  perLimit?: number
}

export interface SeckillSession {
  id: number
  name: string
  startTime: string
  endTime: string
  ongoing: boolean
  skus: SeckillSku[]
}

export function fetchReceivableCoupons() {
  return getData<CouponItem[]>('/trade/open/coupons')
}

export function receiveCoupon(id: number) {
  return postData<number>(`/trade/app/coupons/${id}/receive`)
}

export function fetchMyCoupons(status?: string) {
  return getData<unknown[]>('/trade/app/coupons', status ? { status } : undefined)
}

export function fetchSeckillSessions() {
  return getData<SeckillSession[]>('/trade/open/seckill/sessions/current')
}
