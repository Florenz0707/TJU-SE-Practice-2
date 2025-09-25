<template>
  <div class="restaurant-detail-container">
    <div v-if="loading" class="loading">正在加载餐厅详情...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="business" class="restaurant-content">
      <el-card class="business-header">
        <template #header>
          <div class="header-content">
            <h1>{{ business.businessName }}</h1>
          </div>
        </template>
        <div class="business-info">
          <el-image :src="business.businessImg" fit="cover" class="business-image" />
          <div class="info-text">
            <p><strong>地址:</strong> {{ business.businessAddress }}</p>
            <p><strong>介绍:</strong> {{ business.businessExplain }}</p>
          </div>
        </div>
      </el-card>

      <h2 class="menu-title">菜单</h2>
      <div class="menu-grid">
        <MenuItem v-for="food in menu" :key="food.id" :food="food" />
      </div>

      <div class="reviews-section">
        <h2 class="reviews-title">顾客评价</h2>
        <div v-if="reviews.length > 0" class="reviews-list">
          <el-card v-for="review in reviews" :key="review.id" class="review-card">
            <div class="review-header">
              <strong>{{ review.anonymous ? '匿名用户' : review.customer?.username }}</strong>
              <el-rate :model-value="review.stars" disabled />
            </div>
            <p>{{ review.content }}</p>
          </el-card>
        </div>
        <div v-else>
          <p>暂无评价。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { getBusinessById } from '../../api/business';
import { getAllFoods } from '../../api/food';
import { getBusinessReviews } from '../../api/review';
import type { Business, Food, Review } from '../../api/types';
import MenuItem from '../../components/MenuItem.vue';

const route = useRoute();
const business = ref<Business | null>(null);
const menu = ref<Food[]>([]);
const reviews = ref<Review[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

onMounted(async () => {
  loading.value = true;
  error.value = null;
  const businessId = Number(route.params.id);

  if (isNaN(businessId)) {
    error.value = '无效的餐厅ID。';
    loading.value = false;
    return;
  }

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
      throw new Error(errorMessage || '获取餐厅详情失败');
    }

    // Handle menu
    const menuResult = results[1];
    if (menuResult.status === 'fulfilled' && menuResult.value.success) {
      menu.value = menuResult.value.data;
    } else {
      const errorMessage = menuResult.status === 'fulfilled'
        ? menuResult.value.message
        : (menuResult.reason as Error).message;
      throw new Error(errorMessage || '获取菜单失败');
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
      // Not throwing an error here as it's non-critical
    }
  } catch (err: any) {
    error.value = err.message || '发生未知错误。';
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.restaurant-detail-container {
  padding: 20px;
}
.business-header {
  margin-bottom: 30px;
}
.business-info {
  display: flex;
  gap: 20px;
}
.business-image {
  width: 200px;
  height: 200px;
  border-radius: 8px;
}
.info-text {
  flex-grow: 1;
}
.menu-title {
  margin-bottom: 20px;
  font-size: 1.8rem;
  border-bottom: 2px solid #eee;
  padding-bottom: 10px;
}
.menu-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

@media (min-width: 768px) {
  .menu-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.reviews-section {
  margin-top: 30px;
}
.reviews-title {
  margin-bottom: 20px;
  font-size: 1.8rem;
  border-bottom: 2px solid #eee;
  padding-bottom: 10px;
}
.review-card {
  margin-bottom: 15px;
}
.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
</style>
