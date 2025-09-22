import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router' // Import router instance
import './router/guards' // Import to activate the route guards
import './styles/main.scss' // Import our new global stylesheet
import App from './App.vue'
import { useAuthStore } from './store/auth'
import { setRequestToken } from './utils/request'

// --- Application Initialization ---

// 1. Create Vue and Pinia instances
const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)

// 2. Initialize Auth Store and Request Token
const authStore = useAuthStore()
const initialToken = authStore.token

if (initialToken) {
  // Set the token for all subsequent API requests
  setRequestToken(initialToken)

  // Fetch user info to complete the login process
  authStore.fetchUserInfo().catch(() => {
    // If the token is invalid (e.g., expired), log the user out
    authStore.logout()
  })
}

// 3. Set up cross-tab state synchronization
window.addEventListener('storage', event => {
  // The key for the auth token is 'authToken' as defined in the auth store
  if (event.key === 'authToken') {
    if (event.newValue) {
      // A token was set or updated in another tab
      authStore.setToken(event.newValue)
      // Re-fetch user info with the new token
      authStore.fetchUserInfo().catch(() => authStore.logout())
    } else {
      // The token was removed in another tab
      authStore.logout()
    }
  }
})

// 4. Mount the application
app.mount('#app')
