<template>
  <div class="mobile-home">
    <div class="search-bar-container">
      <el-input
        v-model="searchQuery"
        placeholder="搜索餐厅或美食"
        clearable
        size="large"
        class="search-input"
      >
        <template #prefix>
          <Search :size="18" :stroke-width="2.5" />
        </template>
      </el-input>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-if="error" class="error-state">{{ error }}</div>

    <div v-if="filteredBusinesses.length" class="restaurant-list">
      <MobileRestaurantCard
        v-for="business in filteredBusinesses"
        :key="business.id"
        :business="business"
        class="restaurant-list-item"
      />
    </div>
    <div v-else-if="!loading" class="no-results-state">
      没有找到相关餐厅。
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { Search } from 'lucide-vue-next';
import { getBusinesses } from '../../../api/business';
import type { Business } from '../../../api/types';
import MobileRestaurantCard from '../../../components/mobile/MobileRestaurantCard.vue';

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
  return allBusinesses.value.filter(business =>
    business.businessName.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

onMounted(() => {
  fetchBusinesses();
});
</script>

<style scoped>
.mobile-home {
  padding: 0.75rem;
}

.search-bar-container {
  padding: 0.5rem 0.25rem 1rem;
  background-color: #ffffff;
  border-radius: 8px;
  margin: -0.75rem -0.75rem 1rem -0.75rem;
  padding: 1rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.restaurant-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.loading-state, .error-state, .no-results-state {
  text-align: center;
  margin-top: 3rem;
  color: #6b7280;
}
</style>