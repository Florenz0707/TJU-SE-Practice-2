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
          <el-image
            :src="business.businessImg"
            fit="cover"
            class="business-image"
          />
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getBusinessById } from '../../api/business'
import { getAllFoods } from '../../api/food'
import type { Business, Food } from '../../api/types'
import MenuItem from '../../components/MenuItem.vue'

const route = useRoute()
const business = ref<Business | null>(null)
const menu = ref<Food[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

onMounted(async () => {
  loading.value = true
  error.value = null
  const businessId = Number(route.params.id)

  if (isNaN(businessId)) {
    error.value = '无效的餐厅ID。';
    loading.value = false;
    return;
  }

  try {
    // Fetch business details and menu in parallel
    const [businessResponse, menuResponse] = await Promise.all([
      getBusinessById(businessId),
      getAllFoods({ business: businessId }),
    ])

    if (businessResponse.success) {
      business.value = businessResponse.data
    } else {
      throw new Error(businessResponse.message || '获取餐厅详情失败');
    }

    if (menuResponse.success) {
      menu.value = menuResponse.data
    } else {
      throw new Error(menuResponse.message || '获取菜单失败');
    }
  } catch (err: any) {
    error.value = err.message || '发生未知错误。';
  } finally {
    loading.value = false
  }
})
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
</style>
