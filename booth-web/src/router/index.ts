import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAdminStore } from '@/stores/admin'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior() {
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
        { path: 'category', name: 'category', component: () => import('@/views/CategoryView.vue') },
        { path: 'search', name: 'search', component: () => import('@/views/SearchView.vue') },
        { path: 'product/:id', name: 'product', component: () => import('@/views/ProductView.vue') },
        { path: 'cart', name: 'cart', component: () => import('@/views/CartView.vue'), meta: { auth: true } },
        { path: 'checkout', name: 'checkout', component: () => import('@/views/CheckoutView.vue'), meta: { auth: true } },
        { path: 'orders', name: 'orders', component: () => import('@/views/OrdersView.vue'), meta: { auth: true } },
        { path: 'orders/:orderSn', name: 'order-detail', component: () => import('@/views/OrderDetailView.vue'), meta: { auth: true } },
        { path: 'orders/:orderSn/pay', name: 'order-pay', component: () => import('@/views/PayCashierView.vue'), meta: { auth: true } },
        { path: 'me', name: 'me', component: () => import('@/views/MeView.vue') },
        { path: 'login', name: 'login', component: () => import('@/views/LoginView.vue') },
        { path: 'support', name: 'support', component: () => import('@/views/SupportView.vue'), meta: { auth: true } },
        { path: 'coupons', name: 'coupons', component: () => import('@/views/CouponsView.vue') },
        { path: 'seckill', name: 'seckill', component: () => import('@/views/SeckillView.vue') },
      ],
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/AdminLoginView.vue'),
      meta: { adminGuest: true },
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { admin: true },
      children: [
        { path: '', name: 'admin-dashboard', component: () => import('@/views/admin/AdminDashboardView.vue') },
        { path: 'products', name: 'admin-products', component: () => import('@/views/admin/AdminProductsView.vue') },
        { path: 'orders', name: 'admin-orders', component: () => import('@/views/admin/AdminOrdersView.vue') },
        { path: 'members', name: 'admin-members', component: () => import('@/views/admin/AdminMembersView.vue') },
        { path: 'returns', name: 'admin-returns', component: () => import('@/views/admin/AdminReturnsView.vue') },
        { path: 'ops', name: 'admin-ops', component: () => import('@/views/admin/AdminOpsView.vue') },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const user = useUserStore()
  const admin = useAdminStore()

  if (to.meta.admin || to.meta.adminGuest) {
    if (admin.token && !admin.profile) {
      await admin.refreshProfile()
    }
    if (to.meta.admin && !admin.loggedIn) {
      return { name: 'admin-login', query: { redirect: to.fullPath } }
    }
    if (to.meta.adminGuest && admin.loggedIn) {
      return { name: 'admin-dashboard' }
    }
    return true
  }

  if (user.token && !user.profile) {
    await user.refreshProfile()
  }
  if (to.meta.auth && !user.loggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
