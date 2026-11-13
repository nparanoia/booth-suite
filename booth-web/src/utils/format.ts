import type { SkuSpec } from '@/types/api'

export function money(value: number | string | null | undefined) {
  const n = Number(value || 0)
  return n.toFixed(2)
}

export function specsText(specs: SkuSpec[] | null | undefined) {
  if (!specs?.length) return ''
  return specs.map((s) => `${s.key}:${s.value}`).join(' / ')
}

export const ORDER_STATUS: Record<string, string> = {
  WAIT_PAY: '待付款',
  WAIT_DELIVER: '待发货',
  DELIVERED: '已发货',
  WAIT_RECEIVE: '待收货',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
}

export function orderStatusLabel(status: string) {
  return ORDER_STATUS[status] || status
}

export function placeholderPic(seed: string | number = 'qinglan') {
  return `https://picsum.photos/seed/${encodeURIComponent(String(seed))}/600/600`
}
