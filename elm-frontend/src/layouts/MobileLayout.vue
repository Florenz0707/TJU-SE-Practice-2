<template>
  <div class="mobile-layout">
    <header class="top-bar">
      <h1 class="page-title">{{ $route.meta.title }}</h1>
      <router-link to="/mobile/cart" class="cart-icon-wrapper" v-if="route.name === 'MobileRestaurantDetail'">
        <ShoppingCart :size="26" />
        <span v-if="cartStore.totalItems > 0" class="cart-badge">{{ cartStore.totalItems }}</span>
      </router-link>
    </header>
    <main class="main-content">
      <router-view />
    </main>
    <nav class="bottom-nav">
      <router-link to="/mobile/home" class="nav-item">
        <Home :size="24" />
        <span>首页</span>
      </router-link>
      <router-link to="/mobile/orders" class="nav-item">
        <ScrollText :size="24" />
        <span>订单</span>
      </router-link>
      <router-link to="/mobile/profile/wallet" class="nav-item">
        <Wallet :size="24" />
        <span>钱包</span>
      </router-link>
      <router-link to="/mobile/profile" class="nav-item">
        <User :size="24" />
        <span>我的</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { Home, ScrollText, User, ShoppingCart, Wallet } from 'lucide-vue-next';
import { useCartStore } from '../store/cart';
import { useRoute } from 'vue-router';

const cartStore = useCartStore();
const route = useRoute();
</script>

<style scoped>
.mobile-layout {
  height: 100vh;
  background-color: #f4f4f5;
}

.top-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 1rem;
  height: 56px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.page-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #111827;
}

.cart-icon-wrapper {
  position: relative;
  color: #333;
}

.cart-badge {
  position: absolute;
  top: -4px;
  right: -8px;
  background-color: #f97316; /* Orange color */
  color: white;
  border-radius: 9999px;
  padding: 2px 5px;
  font-size: 10px;
  font-weight: bold;
  line-height: 1;
}

.main-content {
  padding-top: 56px;
  padding-bottom: 60px;
  height: 100%;
  box-sizing: border-box;
  overflow-y: auto;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 10;
  height: 60px;
  background-color: #ffffff;
  display: flex;
  justify-content: space-around;
  align-items: center;
  border-top: 1px solid #e5e7eb;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: #6b7280;
  font-size: 12px;
  transition: color 0.2s;
}

.nav-item.router-link-active {
  color: #f97316; /* Orange color */
}
</style>