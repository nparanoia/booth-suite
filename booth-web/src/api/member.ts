import { deleteData, getData, postData, putData } from './http'
import type { Address, MemberProfile, TokenVO } from '@/types/api'

export function login(username: string, password: string) {
  return postData<TokenVO>('/user/open/members/login', { username, password })
}

export function register(payload: {
  username: string
  password: string
  phone: string
  nickname?: string
}) {
  return postData<number>('/user/open/members/register', payload)
}

export function fetchMe() {
  return getData<MemberProfile>('/user/app/members/me')
}

export function logout() {
  return postData<void>('/user/app/members/logout')
}

export function fetchAddresses() {
  return getData<Address[]>('/user/app/addresses')
}

export function createAddress(payload: {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  region: string
  detailAddress: string
  postCode?: string
  isDefault?: boolean
}) {
  return postData<number>('/user/app/addresses', payload)
}

export function setDefaultAddress(id: number) {
  return putData<void>(`/user/app/addresses/${id}/default`)
}

export function removeAddress(id: number) {
  return deleteData<void>(`/user/app/addresses/${id}`)
}
