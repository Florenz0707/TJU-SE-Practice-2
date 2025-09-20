import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { getCurrentUserCart, addCartItem, updateCartItem, deleteCartItem } from '../api/cart';
import type { Cart, Food } from '../api/types';

export interface AnimationOrigin {
  x: number;
  y: number;
  imgSrc: string;
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<Cart[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const animationOrigin = ref<AnimationOrigin | null>(null);

  const cartTotal = computed(() => {
    return items.value.reduce((total, item) => {
      const price = item.food?.foodPrice ?? 0;
      const quantity = item.quantity ?? 0;
      return total + price * quantity;
    }, 0);
  });

  const totalItems = computed(() => {
    return items.value.reduce((total, item) => total + (item.quantity ?? 0), 0);
  });

  const fetchCart = async () => {
    loading.value = true;
    error.value = null;
    try {
      const response = await getCurrentUserCart();
      if (response.success) {
        items.value = response.data;
      } else {
        throw new Error(response.message || 'Failed to fetch cart');
      }
    } catch (err: any) {
      error.value = err.message;
    } finally {
      loading.value = false;
    }
  };

  const addItem = async (food: Food, quantity: number, origin?: AnimationOrigin) => {
    // Trigger the animation
    if (origin) {
      animationOrigin.value = origin;
    }

    const existingItem = items.value.find(item => item.food?.id === food.id);
    if (existingItem && existingItem.id && existingItem.quantity) {
      // If item exists, update its quantity
      await updateItemQuantity(existingItem.id, existingItem.quantity + quantity);
    } else {
      // If item doesn't exist, add it as a new item
      loading.value = true;
      error.value = null;
      try {
        const newCartItem: Cart = {
          food: food,
          quantity: quantity,
          business: food.business,
          // Customer will be identified by the backend via JWT
        };
        const response = await addCartItem(newCartItem);
        if (response.success) {
          items.value.push(response.data);
        } else {
          throw new Error(response.message || 'Failed to add item to cart');
        }
      } catch (err: any) {
        error.value = err.message;
      } finally {
        loading.value = false;
      }
    }
  };

  const updateItemQuantity = async (itemId: number, quantity: number) => {
    if (quantity <= 0) {
      await removeItem(itemId);
      return;
    }
    loading.value = true;
    error.value = null;
    try {
      const response = await updateCartItem(itemId, quantity);
      if (response.success) {
        const index = items.value.findIndex(item => item.id === itemId);
        if (index !== -1) {
          items.value[index] = response.data;
        }
      } else {
        throw new Error(response.message || 'Failed to update item quantity');
      }
    } catch (err: any) {
      error.value = err.message;
    } finally {
      loading.value = false;
    }
  };

  const removeItem = async (itemId: number) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await deleteCartItem(itemId);
      if (response.success) {
        items.value = items.value.filter(item => item.id !== itemId);
      } else {
        throw new Error(response.message || 'Failed to remove item from cart');
      }
    } catch (err: any) {
      error.value = err.message;
    } finally {
      loading.value = false;
    }
  };

  return {
    items,
    loading,
    error,
    cartTotal,
    totalItems,
    fetchCart,
    addItem,
    updateItemQuantity,
    removeItem,
    animationOrigin,
  };
});
