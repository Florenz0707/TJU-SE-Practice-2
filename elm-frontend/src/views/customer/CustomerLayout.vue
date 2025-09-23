<template>
  <div class="customer-layout">
    <el-container>
      <!-- Header -->
      <el-header class="app-header">
        <div class="header-left">
          <div class="logo">
            <router-link to="/" class="logo-link">美食速递</router-link>
          </div>
          <!-- Role-based Navigation -->
          <el-menu
            v-if="authStore.isLoggedIn && authStore.userRoles.length > 1"
            :default-active="activePath"
            mode="horizontal"
            :ellipsis="false"
            router
            class="header-menu"
          >
            <template v-if="authStore.userRoles.includes('CUSTOMER')">
              <el-menu-item index="/">
                <el-icon><HomeFilled /></el-icon>
                <span>顾客主页</span>
              </el-menu-item>
            </template>
            <template v-if="authStore.userRoles.includes('MERCHANT')">
              <el-menu-item index="/merchant/dashboard">
                <el-icon><Shop /></el-icon>
                <span>商家中心</span>
              </el-menu-item>
            </template>
            <template v-if="authStore.userRoles.includes('ADMIN')">
              <el-menu-item index="/admin/dashboard">
                <el-icon><Setting /></el-icon>
                <span>管理后台</span>
              </el-menu-item>
            </template>
          </el-menu>
          <!-- Profile Section Navigation -->
          <el-menu
            v-if="isProfileSection"
            :default-active="activeProfileRoute"
            mode="horizontal"
            :ellipsis="false"
            router
            class="header-menu profile-nav-menu"
          >
            <el-menu-item index="/profile/details">
              <el-icon><User /></el-icon>
              <span>我的资料</span>
            </el-menu-item>
            <el-menu-item index="/profile/addresses">
              <el-icon><Location /></el-icon>
              <span>地址管理</span>
            </el-menu-item>
            <el-menu-item index="/profile/orders">
              <el-icon><List /></el-icon>
              <span>订单历史</span>
            </el-menu-item>
          </el-menu>
        </div>

        <!-- User Info / Actions -->
        <div class="user-actions">
          <!-- Logged-in state -->
          <template v-if="authStore.isLoggedIn">
            <div class="user-info">
              <el-button ref="cartButtonRef" @click="cartVisible = true" type="primary" round class="hide-on-mobile">
                <el-icon class="el-icon--left"><ShoppingCartIcon /></el-icon>
                购物车
                <el-badge :value="cartStore.totalItems" :hidden="cartStore.totalItems === 0" class="cart-badge" />
              </el-button>
              <el-button ref="cartButtonRef" @click="cartVisible = true" type="primary" circle class="show-on-mobile">
                <el-icon><ShoppingCartIcon /></el-icon>
              </el-button>
              <el-dropdown>
                <el-avatar>{{ authStore.user?.username?.charAt(0).toUpperCase() }}</el-avatar>
                <template #dropdown>
                  <el-dropdown-menu>
                    <router-link to="/profile/details" class="dropdown-link">
                      <el-dropdown-item>个人资料</el-dropdown-item>
                    </router-link>
                    <router-link to="/profile/orders" class="dropdown-link">
                      <el-dropdown-item>我的订单</el-dropdown-item>
                    </router-link>
                    <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
          <!-- Logged-out state -->
          <template v-else>
            <div class="auth-buttons">
              <el-button @click="goTo('/login')">登录</el-button>
              <el-button @click="goTo('/register')" type="primary">注册</el-button>
            </div>
          </template>
        </div>
      </el-header>

      <!-- Main Content -->
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>

      <!-- Shopping Cart Drawer -->
      <ShoppingCart :visible="cartVisible" @update:visible="cartVisible = $event" />
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../../store/auth';
import { useCartStore, type AnimationOrigin } from '../../store/cart';
import ShoppingCart from '../../components/ShoppingCart.vue';
import { ShoppingCart as ShoppingCartIcon, User, Location, List, HomeFilled, Shop, Setting } from '@element-plus/icons-vue';

const authStore = useAuthStore();
const cartStore = useCartStore();
const router = useRouter();
const route = useRoute();

const cartVisible = ref(false);
const cartButtonRef = ref(null);

// Check if the current route is within the profile section
const isProfileSection = computed(() => route.path.startsWith('/profile'));

// Determine the active route for the profile menu
const activeProfileRoute = computed(() => {
  if (route.path.startsWith('/profile/orders')) {
    return '/profile/orders';
  }
  return route.path;
});

const activePath = computed(() => {
  if (route.path.startsWith('/admin')) {
    return '/admin/dashboard';
  } else if (route.path.startsWith('/merchant')) {
    return '/merchant/dashboard';
  }
  return '/';
});

const handleLogout = async () => {
  await authStore.logout();
  router.push({ name: 'Login' });
};

const goTo = (path: string) => {
  router.push(path);
};

const triggerFlyAnimation = (origin: AnimationOrigin) => {
  if (!origin || !cartButtonRef.value) return;

  const cartButtonEl = (cartButtonRef.value as any).$el;
  const targetRect = cartButtonEl.getBoundingClientRect();
  const targetX = targetRect.left + targetRect.width / 2;
  const targetY = targetRect.top + targetRect.height / 2;

  const flyingEl = document.createElement('img');
  flyingEl.src = origin.imgSrc;
  flyingEl.style.position = 'fixed';
  flyingEl.style.left = `${origin.x}px`;
  flyingEl.style.top = `${origin.y}px`;
  flyingEl.style.width = '50px';
  flyingEl.style.height = '50px';
  flyingEl.style.borderRadius = '50%';
  flyingEl.style.objectFit = 'cover';
  flyingEl.style.zIndex = '9999';
  flyingEl.style.pointerEvents = 'none';
  flyingEl.style.transform = 'translate(-50%, -50%)';

  document.body.appendChild(flyingEl);

  flyingEl.animate([
    { transform: `translate(-50%, -50%) scale(1)`, opacity: 1 },
    { transform: `translate(${(targetX - origin.x)}px, ${(targetY - origin.y)}px) scale(0.1)`, opacity: 0.5 }
  ], {
    duration: 600,
    easing: 'cubic-bezier(0.5, -0.5, 1, 1)',
  }).onfinish = () => {
    document.body.removeChild(flyingEl);
    // Reset the store state after animation
    cartStore.animationOrigin = null;
  };
};

watch(() => cartStore.animationOrigin, (newOrigin) => {
  if (newOrigin) {
    triggerFlyAnimation(newOrigin);
  }
});
</script>

<style lang="scss" scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e5e7eb;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 32px; // Add space between logo and nav
}

.logo-link {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--el-text-color-primary);
  text-decoration: none;
}

.profile-nav-menu {
  .el-menu-item {
    span {
      margin-left: 8px;
    }
  }
}

.user-actions,
.user-info,
.auth-buttons {
  display: flex;
  align-items: center;
  gap: 15px;
}

.cart-badge {
  margin-left: 8px;
  transform: translateY(-2px) translateX(2px);
}

.el-dropdown {
  cursor: pointer;
}

.dropdown-link {
  color: inherit;
  text-decoration: none;
}

@media (max-width: 768px) {
  .profile-nav-menu .el-menu-item span {
    display: none; // Hide text on mobile, keep only icons
  }
}
</style>