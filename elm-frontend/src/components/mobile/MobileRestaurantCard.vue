<template>
  <div class="mobile-restaurant-card" @click="goToRestaurant">
    <div class="card-image-wrapper">
      <img v-if="business.businessImg" :src="business.businessImg" alt="餐厅图片" class="restaurant-image" />
      <div v-else class="placeholder-image">
        <span>{{ business.businessName[0] }}</span>
      </div>
    </div>
    <div class="card-info">
      <h3 class="name">{{ business.businessName }}</h3>
      <p class="details">
        <span>⭐ 4.5</span>
        <span class="separator">·</span>
        <span>{{ business.businessAddress }}</span>
      </p>
      <p class="delivery-info">25-35 分钟</p>
      <div class="price-info">
        <span>起送 ¥{{ business.startPrice }}</span>
        <span>配送 ¥{{ business.deliveryPrice }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import type { Business } from '../../api/types';

const props = defineProps<{
  business: Business;
}>();

const router = useRouter();

const goToRestaurant = () => {
  router.push({ name: 'MobileRestaurantDetail', params: { id: props.business.id } });
};
</script>

<style scoped>
.mobile-restaurant-card {
  display: flex;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: transform 0.2s ease-in-out;
}

.mobile-restaurant-card:active {
  transform: scale(0.98);
}

.card-image-wrapper {
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}

.restaurant-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  font-size: 24px;
  font-weight: 600;
  color: #a0a0a0;
}

.card-info {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex-grow: 1;
}

.name {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 0.25rem;
}

.details, .delivery-info {
  font-size: 0.75rem;
  color: #666;
  margin: 0 0 0.25rem;
  display: flex;
  align-items: center;
  gap: 4px;
}

.separator {
  color: #ddd;
}

.price-info {
  font-size: 0.75rem;
  color: #333;
  margin-top: 0.5rem;
  display: flex;
  gap: 1rem;
}
</style>