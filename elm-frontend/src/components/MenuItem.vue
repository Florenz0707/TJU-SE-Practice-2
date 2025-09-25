<template>
  <div class="menu-item-card">
    <div class="item-image-wrapper">
      <img
        v-if="food.foodImg"
        ref="imageElement"
        :src="formatBase64Image(food.foodImg)"
        alt="食物图片"
        class="food-image"
      />
      <div v-else class="placeholder-image">
        <span class="placeholder-text">{{ food.foodName }}</span>
      </div>
    </div>
    <div class="food-details">
      <h4 class="food-name">{{ food.foodName }}</h4>
      <p class="food-explain">{{ food.foodExplain }}</p>
      <div class="food-actions">
        <span class="food-price">¥{{ food.foodPrice.toFixed(2) }}</span>
        <el-button type="primary" @click="handleAddToCart" round>添加</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { Food } from '../api/types';
import { useCartStore } from '../store/cart';
import { ElMessage } from 'element-plus';
import { formatBase64Image } from '../utils/image';

const props = defineProps<{
  food: Food;
}>();

const cartStore = useCartStore();
const imageElement = ref<HTMLImageElement | null>(null);

const handleAddToCart = () => {
  let origin: { x: number; y: number; imgSrc: string } | undefined;

  if (imageElement.value && imageElement.value.src) {
    const rect = imageElement.value.getBoundingClientRect();
    origin = {
      x: rect.left + rect.width / 2,
      y: rect.top + rect.height / 2,
      imgSrc: imageElement.value.src,
    };
  }

  // The animation is a bonus, but adding to cart should always work.
  cartStore.addItem(props.food, 1, origin);

  // Show a confirmation message
  ElMessage({
    message: `${props.food.foodName} 已添加到购物车！`,
    type: 'success',
  });
};
</script>

<style scoped>
.menu-item-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.04);
}
.item-image-wrapper {
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
}

.placeholder-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.placeholder-text {
  font-size: 14px;
  font-weight: 600;
  color: #6c757d;
  padding: 5px;
}
.food-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.food-details {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  text-align: left;
  gap: 8px;
}
.food-name {
  font-family: "Poppins", sans-serif;
  font-weight: 600;
  font-size: 18px;
  margin: 0 0 8px 0;
  text-align: left;
}
.food-explain {
  font-family: "Inter", sans-serif;
  color: #6B7280;
  font-size: 14px;
  flex-grow: 1;
  margin-bottom: 10px;
  text-align: left;
}
.food-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.food-price {
  font-family: "Poppins", sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}
</style>
