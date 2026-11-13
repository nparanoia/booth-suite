import { adminDelete, adminGet, adminPost, adminPut } from './adminHttp'
import type {
  CategoryTree,
  OrderDetail,
  OrderSummary,
  PageResult,
  ProductDetail,
  ProductListItem,
  TokenVO,
} from '@/types/api'

export interface AdminProfile {
  id: number
  username: string
  nickname: string | null
  avatar: string | null
  roles: string[]
  permissions: string[]
}

export interface MemberAdmin {
  id: number
  username: string
  nickname: string | null
  phone: string
  gender: string | null
  integration: number
  status: 'ENABLED' | 'DISABLED'
  createTime: string
}

export interface ReturnApply {
  id: number
  orderSn: string
  orderItemId: number
  returnQuantity: number
  returnAmount: number
  reason: string
  description: string | null
  status: string
  handleNote: string | null
  createTime: string
}

export function adminLogin(username: string, password: string) {
  return adminPost<TokenVO>('/user/open/admin-auth/login', { username, password })
}

export function adminMe() {
  return adminGet<AdminProfile>('/user/admin/auth/me')
}

export function adminLogout() {
  return adminPost<void>('/user/admin/auth/logout')
}

export function adminProducts(params?: {
  pageNum?: number
  pageSize?: number
  keyword?: string
  categoryId?: number
  publishStatus?: string
}) {
  return adminGet<PageResult<ProductListItem>>('/product/admin/products', params)
}

export function adminProductDetail(id: number) {
  return adminGet<ProductDetail>(`/product/admin/products/${id}`)
}

export function adminPublishProducts(ids: number[], status: 'ON_SHELF' | 'OFF_SHELF') {
  return adminPut<void>('/product/admin/products/publish', { ids, status })
}

export function adminDeleteProduct(id: number) {
  return adminDelete<void>(`/product/admin/products/${id}`)
}

export function adminCategoryTree() {
  return adminGet<CategoryTree[]>('/product/admin/categories/tree')
}

export function adminOrders(params?: {
  pageNum?: number
  pageSize?: number
  status?: string
  orderSn?: string
}) {
  return adminGet<PageResult<OrderSummary>>('/trade/admin/orders', params)
}

export function adminOrderDetail(orderSn: string) {
  return adminGet<OrderDetail>(`/trade/admin/orders/${orderSn}`)
}

export function adminShipOrder(orderSn: string, deliveryCompany: string, deliverySn: string) {
  return adminPost<void>(`/trade/admin/orders/${orderSn}/ship`, { deliveryCompany, deliverySn })
}

export function adminCloseOrder(orderSn: string) {
  return adminPost<void>(`/trade/admin/orders/${orderSn}/close`)
}

export function adminMembers(params?: {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: string
}) {
  return adminGet<PageResult<MemberAdmin>>('/user/admin/members', params)
}

export function adminUpdateMemberStatus(id: number, status: 'ENABLED' | 'DISABLED') {
  return adminPut<void>(`/user/admin/members/${id}/status`, { status })
}

export function adminReturns(status?: string) {
  return adminGet<ReturnApply[]>('/trade/admin/returns', status ? { status } : undefined)
}

export function adminApproveReturn(id: number, note?: string) {
  return adminPost<void>(`/trade/admin/returns/${id}/approve`, { note })
}

export function adminRejectReturn(id: number, note?: string) {
  return adminPost<void>(`/trade/admin/returns/${id}/reject`, { note })
}

export function adminReceiveReturn(id: number, note?: string) {
  return adminPost<void>(`/trade/admin/returns/${id}/receive`, { note })
}
