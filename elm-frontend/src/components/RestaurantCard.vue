<template>
  <div class="restaurant-card" @click="goToRestaurant">
    <div class="card-image-wrapper">
      <img v-if="business.businessImg" :src="business.businessImg" alt="餐厅图片" class="restaurant-image" />
      <div v-else class="placeholder-image">
        <span class="placeholder-text">{{ business.businessName }}</span>
      </div>
    </div>
    <div class="card-info">
      <h3 class="name">{{ business.businessName }}</h3>
      <p class="details">
        <span>⭐ 4.5</span>
        <span class="separator">·</span>
        <span>{{ business.businessAddress }}</span>
        <span class="separator">·</span>
        <span>25-35 分钟</span>
      </p>
      <div class="price-info">
        <span>起送价: ¥{{ business.startPrice }}</span>
        <span>配送费: ¥{{ business.deliveryPrice }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { Business } from '../api/types'

const props = defineProps<{
  business: Business
}>()

const router = useRouter()

const goToRestaurant = () => {
  router.push({ name: 'RestaurantDetail', params: { id: props.business.id } })
}
</script>

<style scoped>
.restaurant-card {
  background-color: #ffffff;
  border-radius: 16px; /* $border-radius-lg */
  box-shadow:
    0 10px 15px -3px rgba(0, 0, 0, 0.07),
    0 4px 6px -2px rgba(0, 0, 0, 0.05); /* $shadow-medium */
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease-out;
}

.restaurant-card:hover {
  transform: translateY(-8px);
  box-shadow:
    0 20px 25px -5px rgba(0, 0, 0, 0.1),
    0 10px 10px -5px rgba(0, 0, 0, 0.04); /* $shadow-large */
}

.card-image-wrapper {
  width: 100%;
  height: 160px;
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
  font-size: 20px;
  font-weight: 600;
  color: #6c757d;
  padding: 10px;
}

.restaurant-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease-out;
}

.restaurant-card:hover .restaurant-image {
  transform: scale(1.05);
}

.card-info {
  padding: 16px;
}

.name {
  font-family: 'Poppins', sans-serif;
  font-weight: 600; /* $fw-semibold */
  font-size: 20px; /* A bit smaller than H3 for cards */
  color: #111827; /* $text-primary */
  margin: 0 0 8px 0;
}

.details {
  font-family: 'Inter', sans-serif;
  font-size: 14px; /* $fs-label */
  color: #6b7280; /* $text-secondary */
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.separator {
  color: #e5e7eb; /* $border-color */
}

.price-info {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}
</style>
