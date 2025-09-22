<template>
  <el-drawer
    v-model="isCartVisible"
    title="My Cart"
    size="400px"
    custom-class="glass-cart"
  >
    <div v-if="cartStore.items.length > 0" class="cart-content">
      <!-- Cart Items -->
      <div class="cart-items-list">
        <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
          <img :src="item.food?.foodImg || 'https://placehold.co/80x80/f8f9fa/ccc?text=Item'" alt="item image" class="item-image"/>
          <div class="item-details">
            <span class="item-name">{{ item.food?.foodName ?? 'Unknown Item' }}</span>
            <span class="item-price">${{ (item.food?.foodPrice ?? 0).toFixed(2) }}</span>
          </div>
          <div class="item-controls" v-if="item.id">
            <el-input-number
              :model-value="item.quantity"
              @change="(quantity: number) => cartStore.updateItemQuantity(item.id!, quantity)"
              :min="1"
              size="small"
            />
            <el-button type="danger" :icon="Delete" circle plain @click="cartStore.removeItem(item.id!)" />
          </div>
        </div>
      </div>

      <!-- Cart Footer -->
      <div class="cart-footer">
        <div class="summary-line">
          <span>Subtotal</span>
          <span>${{ cartStore.cartTotal.toFixed(2) }}</span>
        </div>
         <div class="summary-line">
          <span>Delivery Fee</span>
          <span>$5.00</span>
        </div>
        <el-divider />
        <div class="summary-line total">
          <span>Total</span>
          <span>${{ (cartStore.cartTotal + 5).toFixed(2) }}</span>
        </div>
        <el-button type="primary" class="checkout-btn" @click="goToCheckout" round>
          Continue to Checkout
        </el-button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-cart-container">
      <el-empty description="Your cart is empty. Add some delicious food!" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../store/cart';
import { Delete } from '@element-plus/icons-vue';

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits(['update:visible']);

const isCartVisible = ref(props.visible);
watch(() => props.visible, (newValue) => {
  isCartVisible.value = newValue;
});
watch(isCartVisible, (newValue) => {
  emit('update:visible', newValue);
});

const cartStore = useCartStore();
const router = useRouter();

const goToCheckout = () => {
  isCartVisible.value = false;
  router.push({ name: 'Checkout' });
};

// Fetch cart when the component is first used
cartStore.fetchCart();
</script>

<style>
/* We need a global style to target the drawer's wrapper for the glass effect */
.el-drawer.glass-cart {
  background-color: rgba(255, 255, 255, 0.7) !important;
  backdrop-filter: blur(10px) !important;
}
</style>

<style scoped>
.cart-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.cart-items-list {
  flex-grow: 1;
  overflow-y: auto;
  padding-right: 10px; /* for scrollbar */
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #E5E7EB; /* $border-color */
}
.item-image {
  width: 64px;
  height: 64px;
  border-radius: 8px; /* $border-radius-sm */
  object-fit: cover;
}
.item-details {
  flex-grow: 1;
}
.item-name {
  font-family: "Poppins", sans-serif;
  font-weight: 500;
  color: #111827;
}
.item-price {
  color: #6B7280;
  font-size: 14px;
}
.item-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cart-footer {
  padding: 20px 0;
  border-top: 1px solid #E5E7EB;
}
.summary-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-family: "Inter", sans-serif;
  color: #6B7280;
}
.summary-line.total {
  font-family: "Poppins", sans-serif;
  font-weight: 600;
  font-size: 1.2em;
  color: #111827;
}
.checkout-btn {
  width: 100%;
  height: 48px;
  margin-top: 16px;
  font-size: 16px;
}
.empty-cart-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
