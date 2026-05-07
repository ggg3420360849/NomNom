import { createRouter, createWebHistory } from 'vue-router'
import AuthView from '../views/AuthView.vue'
import CustomerPortalView from '../views/CustomerPortalView.vue'
import MerchantPortalView from '../views/MerchantPortalView.vue'
import { resolvePortalPathForUser } from '../utils/useNomNomApp'

function readStoredUser() {
  try {
    const raw = localStorage.getItem('nomnom-auth-user')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const routes = [
  {
    path: '/',
    redirect: () => resolvePortalPathForUser(readStoredUser()),
  },
  {
    path: '/login',
    name: 'login',
    component: AuthView,
    meta: {
      guestOnly: true,
    },
  },
  {
    path: '/customer',
    name: 'customer',
    component: CustomerPortalView,
    meta: {
      requiresAuth: true,
      role: 'CUSTOMER',
    },
  },
  {
    path: '/merchant',
    name: 'merchant',
    component: MerchantPortalView,
    meta: {
      requiresAuth: true,
      role: 'MERCHANT',
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  // Check permissions before the target page is rendered.
  const user = readStoredUser()

  if (to.meta.guestOnly && user) {
    return resolvePortalPathForUser(user)
  }

  if (to.meta.requiresAuth && !user) {
    return '/login'
  }

  if (to.meta.role && user?.roleCode !== to.meta.role) {
    return resolvePortalPathForUser(user)
  }

  return true
})

export default router
