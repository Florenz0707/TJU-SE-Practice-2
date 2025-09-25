<template>
  <div class="mobile-restaurant-detail">
    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-if="error" class="error-state">{{ error }}</div>
    <div v-if="business" class="content">
      <div class="header-image" :style="{ backgroundImage: `url(${business.businessImg})` }"></div>
      <div class="info-card">
        <h1 class="name">{{ business.businessName }}</h1>
        <p class="details">
          <span>⭐ 4.5</span>
          <span class="separator">·</span>
          <span>{{ business.businessAddress }}</span>
        </p>
        <div class="price-info">
          <span>起送 ¥{{ business.startPrice }}</span>
          <span>配送 ¥{{ business.deliveryPrice }}</span>
        </div>
      </div>

      <div class="menu-section">
        <h2 class="section-title">菜单</h2>
        <el-input
          v-model="foodSearchQuery"
          placeholder="搜索菜单"
          clearable
          class="search-input"
        >
          <template #prefix>
            <Search :size="16" />
          </template>
        </el-input>
        <div v-if="filteredFoods.length" class="menu-items">
          <div v-for="food in filteredFoods" :key="food.id" class="menu-item">
            <img :src="food.foodImg" alt="" class="food-image" v-if="food.foodImg">
            <div class="food-info">
              <h3 class="food-name">{{ food.foodName }}</h3>
              <p class="food-desc">{{ food.foodExplain }}</p>
              <p class="food-price">¥{{ food.foodPrice }}</p>
            </div>
            <div class="actions">
              <el-button type="primary" circle size="small" @click="handleAddItem(food)">+</el-button>
            </div>
          </div>
        </div>
        <div v-else class="no-menu">暂无菜单信息。</div>
      </div>

      <div class="reviews-section">
        <h2 class="section-title">顾客评价</h2>
        <div v-if="reviews.length" class="review-list">
          <div v-for="review in reviews" :key="review.id" class="review-item">
            <div class="review-author">
              <strong>{{ review.anonymous ? '匿名用户' : review.customer?.username }}</strong>
            </div>
            <el-rate :model-value="review.stars" disabled size="small" />
            <p class="review-content">{{ review.content }}</p>
          </div>
        </div>
        <div v-else class="no-reviews">暂无评价。</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { Search } from 'lucide-vue-next';
import { getBusinessById } from '../../../api/business';
import { getAllFoods } from '../../../api/food';
import { getBusinessReviews } from '../../../api/review';
import type { Business, Food, Review } from '../../../api/types';
import { useCartStore } from '../../../store/cart';

const route = useRoute();
const business = ref<Business | null>(null);
const foods = ref<Food[]>([]);
const reviews = ref<Review[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const foodSearchQuery = ref('');

const businessId = Number(route.params.id);
const cartStore = useCartStore();

const handleAddItem = (food: Food) => {
  cartStore.addItem(food, 1);
};

const filteredFoods = computed(() => {
  if (!foodSearchQuery.value) {
    return foods.value;
  }
  return foods.value.filter(food =>
    food.foodName.toLowerCase().includes(foodSearchQuery.value.toLowerCase())
  );
});

onMounted(async () => {
  loading.value = true;
  error.value = null;
  try {
    const results = await Promise.allSettled([
      getBusinessById(businessId),
      getAllFoods({ business: businessId }),
      getBusinessReviews(businessId),
    ]);

    // Handle business details
    const businessResult = results[0];
    if (businessResult.status === 'fulfilled' && businessResult.value.success) {
      business.value = businessResult.value.data;
    } else {
      const errorMessage = businessResult.status === 'fulfilled'
        ? businessResult.value.message
        : (businessResult.reason as Error).message;
      throw new Error(errorMessage || '获取商家信息失败');
    }

    // Handle menu (non-critical)
    const foodResult = results[1];
    if (foodResult.status === 'fulfilled' && foodResult.value.success) {
      foods.value = foodResult.value.data;
    } else {
      const errorMessage = foodResult.status === 'fulfilled'
        ? foodResult.value.message
        : (foodResult.reason as Error).message;
      console.error('获取菜单失败:', errorMessage);
    }

    // Handle reviews (non-critical)
    const reviewsResult = results[2];
    if (reviewsResult.status === 'fulfilled' && reviewsResult.value.success) {
      reviews.value = reviewsResult.value.data;
    } else {
      const errorMessage = reviewsResult.status === 'fulfilled'
        ? reviewsResult.value.message
        : (reviewsResult.reason as Error).message;
      console.error('获取评价失败:', errorMessage);
    }
  } catch (err: any) {
    error.value = err.message || '发生未知错误';
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.mobile-restaurant-detail {
  background-color: #f8f8f8;
}
.header-image {
  height: 200px;
  background-size: cover;
  background-position: center;
}
.info-card {
  background: #fff;
  padding: 1rem;
  margin: -2rem 1rem 1rem;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  position: relative;
}
.name {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 0.5rem;
}
.details, .price-info {
  font-size: 0.875rem;
  color: #666;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0.5rem;
}
.menu-section {
  background: #fff;
  padding: 1rem;
}
.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1rem;
}
.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid #f0f0f0;
}
.food-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  margin-right: 1rem;
}
.food-name {
  font-size: 1rem;
  font-weight: 600;
}
.food-desc {
  font-size: 0.875rem;
  color: #888;
  margin: 0.25rem 0;
}
.food-price {
  font-size: 1rem;
  font-weight: 700;
  color: var(--el-color-primary);
}
.reviews-section {
  background: #fff;
  padding: 1rem;
  margin-top: 1rem;
}
.review-item {
  padding: 1rem 0;
  border-bottom: 1px solid #f0f0f0;
}
.review-author {
  margin-bottom: 0.5rem;
}
.review-content {
  margin-top: 0.5rem;
  font-size: 0.9rem;
  color: #333;
}
.no-reviews {
  text-align: center;
  color: #888;
  padding: 1rem 0;
}
</style>