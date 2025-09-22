<template>
  <div class="customer-layout">
    <el-container>
      <!-- Header -->
      <el-header class="app-header">
        <div class="logo">
          <router-link to="/" class="logo-link">FoodFleet</router-link>
        </div>
        <div class="user-info">
          <el-button ref="cartButtonRef" @click="cartVisible = true" type="primary" round>
            <el-icon class="el-icon--left"><ShoppingCartIcon /></el-icon>
            Cart
            <el-badge :value="cartStore.totalItems" :hidden="cartStore.totalItems === 0" class="cart-badge" />
          </el-button>
          <el-dropdown>
            <el-avatar>{{ authStore.user?.username?.charAt(0).toUpperCase() }}</el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <router-link to="/profile/details" class="dropdown-link">
                  <el-dropdown-item>Profile</el-dropdown-item>
                </router-link>
                <router-link to="/profile/orders" class="dropdown-link">
                  <el-dropdown-item>My Orders</el-dropdown-item>
                </router-link>
                <el-dropdown-item divided @click="handleLogout">Logout</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../store/auth';
import { useCartStore, type AnimationOrigin } from '../../store/cart';
import ShoppingCart from '../../components/ShoppingCart.vue';
import { ShoppingCart as ShoppingCartIcon } from '@element-plus/icons-vue';

const authStore = useAuthStore();
const cartStore = useCartStore();
const router = useRouter();

const cartVisible = ref(false);
const cartButtonRef = ref(null);

const handleLogout = async () => {
  await authStore.logout();
  router.push({ name: 'Login' });
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

.logo-link {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--el-text-color-primary);
  text-decoration: none;
}

.user-info {
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
</style>