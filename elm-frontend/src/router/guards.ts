import router from './index'
import { useAuthStore } from '../store/auth'
import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'

/**
 * Global Before Guard
 */
router.beforeEach(async (
  to: RouteLocationNormalized,
  _from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  // Set page title
  if (to.meta.title && typeof to.meta.title === 'string') {
    document.title = `${to.meta.title} - 美食速递`;
  }

    const authStore = useAuthStore()
    const requiresAuth = to.meta.requiresAuth

    if (requiresAuth) {
      if (authStore.isLoggedIn) {
        if (!authStore.user) {
          try {
            await authStore.fetchUserInfo()
          } catch (error) {
            await authStore.logout()
            next({ name: 'Login', query: { redirect: to.fullPath } })
            return
          }
        }

        const requiredRoles = to.meta.roles as string[] | undefined
        // Check if user has at least one of the required roles
        const hasRequiredRole = requiredRoles
          ? requiredRoles.some(role => authStore.userRoles.includes(role))
          : true // If no roles are required, access is granted

        if (requiredRoles && !hasRequiredRole) {
          next({ name: 'Forbidden' })
        } else {
          next()
        }
      } else {
        next({ name: 'Login', query: { redirect: to.fullPath } })
      }
    } else {
      next()
    }
  }
)
