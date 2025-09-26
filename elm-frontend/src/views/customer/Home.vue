<template>
  <div class="home-container">
    <div class="search-bar-wrapper">
      <el-input
        v-model="searchQuery"
        placeholder="搜索餐厅、菜系或菜品..."
        clearable
        size="large"
        class="main-search-input"
      >
        <template #prefix>
          <Search :size="20" :stroke-width="2.5" class="search-icon" />
        </template>
      </el-input>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="filteredBusinesses.length" class="restaurant-grid">
      <RestaurantCard v-for="business in filteredBusinesses" :key="business.id" :business="business" />
    </div>
    <div v-else-if="!loading" class="no-results">
      没有找到符合条件的餐厅。
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { Search } from 'lucide-vue-next';
import { getBusinesses } from '../../api/business';
import type { Business } from '../../api/types';
import RestaurantCard from '../../components/RestaurantCard.vue';

const allBusinesses = ref<Business[]>([]);
const searchQuery = ref('');
const loading = ref(false);
const error = ref<string | null>(null);

const fetchBusinesses = async () => {
  loading.value = true;
  error.value = null;
  try {
    const response = await getBusinesses();
    if (response.success) {
      allBusinesses.value = response.data;
    } else {
      throw new Error(response.message || '获取商家列表失败');
    }
  } catch (err: any) {
    error.value = err.message || '发生未知错误';
  } finally {
    loading.value = false;
  }
};

const filteredBusinesses = computed(() => {
  if (!searchQuery.value) {
    return allBusinesses.value;
  }
  const lowerCaseQuery = searchQuery.value.toLowerCase();
  return allBusinesses.value.filter(business =>
    business.businessName.toLowerCase().includes(lowerCaseQuery) ||
    business.businessAddress?.toLowerCase().includes(lowerCaseQuery) ||
    business.businessExplain?.toLowerCase().includes(lowerCaseQuery)
  );
});

onMounted(() => {
  fetchBusinesses();
});
</script>

<style lang="scss" scoped>
.home-container {
  padding: 1rem;
  max-width: 1200px;
  margin: 0 auto;
}

.search-bar-wrapper {
  margin-bottom: 2rem;
}

.main-search-input {
  .search-icon {
    margin-left: 0.5rem;
    color: #9ca3af;
  }
}

.restaurant-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
}

.loading, .error, .no-results {
  margin-top: 2rem;
  text-align: center;
  font-size: 1rem;
  color: #6b7280;
}

@media (min-width: 768px) {
  .home-container {
    padding: 2rem 1.5rem;
  }
  .search-bar-wrapper {
    margin-bottom: 2.5rem;
  }
  .restaurant-grid {
    gap: 1.75rem;
  }
  .loading, .error, .no-results {
    font-size: 1.125rem;
  }
}
</style>
