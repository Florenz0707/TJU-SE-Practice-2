import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { getCurrentUserCart, addCartItem, updateCartItem, deleteCartItem } from '../api/cart';
import type { Cart, Food } from '../api/types';
import { useAuthStore } from './auth';

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
  const currentBusinessId = ref<number | null>(null);
  const cartIconElement = ref<HTMLElement | null>(null);
  const deliveryPrice = ref<number>(0);
  const startPrice = ref<number>(0);

  const itemsForCurrentBusiness = computed(() => {
    if (!currentBusinessId.value) {
      return [];
    }
    return items.value.filter(item => item.business?.id === currentBusinessId.value);
  });

  const cartTotal = computed(() => {
    return itemsForCurrentBusiness.value.reduce((total, item) => {
      const price = item.food?.foodPrice ?? 0;
      const quantity = item.quantity ?? 0;
      return total + price * quantity;
    }, 0);
  });

  const totalItems = computed(() => {
    return itemsForCurrentBusiness.value.reduce((total, item) => total + (item.quantity ?? 0), 0);
  });

  const finalOrderTotal = computed(() => {
    return cartTotal.value + deliveryPrice.value;
  });

  const setCurrentBusinessId = (id: number | null) => {
    currentBusinessId.value = id;
  };

  const setCartIconElement = (el: HTMLElement) => {
    cartIconElement.value = el;
  };

  const setBusinessFees = (delivery: number, start: number) => {
    deliveryPrice.value = delivery;
    startPrice.value = start;
  };

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
      // animationOrigin.value = origin; // Animation disabled due to reported issues
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
        const authStore = useAuthStore();
        if (!authStore.user) {
          throw new Error('User not logged in. Cannot add items to cart.');
        }
        const newCartItem: Cart = {
          food: food,
          quantity: quantity,
          business: food.business,
          customer: authStore.user,
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
    itemsForCurrentBusiness,
    setCurrentBusinessId,
    currentBusinessId,
    cartIconElement,
    setCartIconElement,
    deliveryPrice,
    startPrice,
    setBusinessFees,
    finalOrderTotal,
  };
});
