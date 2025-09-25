<template>
  <div class="mobile-cart-page">
    <div v-if="cartStore.loading" class="loading">加载中...</div>
    <div v-if="cartStore.error" class="error">{{ cartStore.error }}</div>

    <div v-if="cartStore.items.length > 0" class="cart-content">
      <div class="cart-items">
        <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
          <div class="item-info">
            <h3 class="item-name">{{ item.food?.foodName }}</h3>
            <p class="item-price">¥{{ item.food?.foodPrice }}</p>
          </div>
          <div class="item-actions">
            <el-button-group>
              <el-button @click="updateQuantity(item, item.quantity! - 1)">-</el-button>
              <el-button text>{{ item.quantity }}</el-button>
              <el-button @click="updateQuantity(item, item.quantity! + 1)">+</el-button>
            </el-button-group>
            <el-button type="danger" plain @click="removeItem(item.id!)" class="remove-btn">移除</el-button>
          </div>
        </div>
      </div>

      <div class="cart-summary">
        <div class="summary-row">
          <span>商品总计</span>
          <span class="total-price">¥{{ cartStore.cartTotal.toFixed(2) }}</span>
        </div>
        <el-button type="primary" size="large" class="checkout-btn" @click="goToCheckout">去结算</el-button>
      </div>
    </div>

    <div v-else class="empty-cart">
      <h2>购物车是空的</h2>
      <p>去添加一些美味的食物吧！</p>
      <el-button type="primary" @click="$router.push('/mobile/home')">去逛逛</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../../../store/cart';
import type { Cart } from '../../../api/types';

const cartStore = useCartStore();
const router = useRouter();

onMounted(() => {
  if (cartStore.items.length === 0) {
    cartStore.fetchCart();
  }
});

const updateQuantity = (item: Cart, newQuantity: number) => {
  if (item.id) {
    if (newQuantity > 0) {
      cartStore.updateItemQuantity(item.id, newQuantity);
    } else {
      cartStore.removeItem(item.id);
    }
  }
};

const removeItem = (itemId: number) => {
  cartStore.removeItem(itemId);
};

const goToCheckout = () => {
  router.push('/mobile/checkout');
};
</script>

<style scoped>
.mobile-cart-page { padding: 1rem; }
.cart-item { display: flex; justify-content: space-between; align-items: center; padding: 1rem 0; border-bottom: 1px solid #eee; }
.item-name { font-size: 1rem; font-weight: 600; }
.item-price { font-size: 0.9rem; color: #888; }
.remove-btn { margin-left: 1rem; }
.cart-summary { margin-top: 2rem; padding-top: 1rem; border-top: 2px solid #ddd; }
.summary-row { display: flex; justify-content: space-between; font-size: 1.1rem; font-weight: 600; margin-bottom: 1rem; }
.checkout-btn { width: 100%; }
.empty-cart { text-align: center; padding: 4rem 0; }
.empty-cart h2 { font-size: 1.5rem; margin-bottom: 1rem; }
</style>