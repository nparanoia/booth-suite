import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as adminApi from '@/api/admin'
import type { AdminProfile } from '@/api/admin'

const TOKEN_KEY = 'ql_admin_token'

export const useAdminStore = defineStore('admin', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const profile = ref<AdminProfile | null>(null)
  const loading = ref(false)

  const loggedIn = computed(() => Boolean(token.value))
  const displayName = computed(
    () => profile.value?.nickname || profile.value?.username || '管理员',
  )

  function setToken(value: string) {
    token.value = value
    if (value) localStorage.setItem(TOKEN_KEY, value)
    else localStorage.removeItem(TOKEN_KEY)
  }

  async function login(username: string, password: string) {
    const data = await adminApi.adminLogin(username, password)
    setToken(data.token)
    await refreshProfile()
  }

  async function refreshProfile() {
    if (!token.value) {
      profile.value = null
      return
    }
    loading.value = true
    try {
      profile.value = await adminApi.adminMe()
    } catch {
      setToken('')
      profile.value = null
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      if (token.value) await adminApi.adminLogout()
    } catch {
      /* ignore */
    }
    setToken('')
    profile.value = null
  }

  function can(code: string) {
    const perms = profile.value?.permissions || []
    return perms.includes('*') || perms.includes(code)
  }

  return {
    token,
    profile,
    loading,
    loggedIn,
    displayName,
    login,
    refreshProfile,
    logout,
    can,
  }
})
