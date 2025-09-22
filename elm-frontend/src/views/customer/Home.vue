<template>
  <div class="home-container">
    <div class="search-bar-wrapper">
      <el-input
        v-model="searchQuery"
        placeholder="Search for restaurants, cuisines, or dishes..."
        @keyup.enter="fetchBusinesses"
        clearable
        size="large"
        class="main-search-input"
      >
        <template #prefix>
          <Search :size="20" :stroke-width="2.5" class="search-icon" />
        </template>
      </el-input>
    </div>

    <div v-if="loading" class="loading">Loading...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="businesses.length" class="restaurant-grid">
      <RestaurantCard v-for="business in businesses" :key="business.id" :business="business" />
    </div>
    <div v-else-if="!loading" class="no-results">
      No restaurants found.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Search } from 'lucide-vue-next';
import { getBusinesses } from '../../api/business';
import type { Business } from '../../api/types';
import RestaurantCard from '../../components/RestaurantCard.vue';

const businesses = ref<Business[]>([]);
const searchQuery = ref('');
const loading = ref(false);
const error = ref<string | null>(null);

const fetchBusinesses = async () => {
  loading.value = true;
  error.value = null;
  try {
    // In a real app, you'd pass search/filter params here
    const params = searchQuery.value ? { name: searchQuery.value } : {};
    const response = await getBusinesses(params);
    if (response.success) {
      businesses.value = response.data;
    } else {
      throw new Error(response.message || 'Failed to fetch businesses');
    }
  } catch (err: any) {
    error.value = err.message || 'An unexpected error occurred.';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchBusinesses();
});
</script>

<style lang="scss" scoped>
.home-container {
  padding: 2rem 1.5rem;
  max-width: 1200px;
  margin: 0 auto;
}

.search-bar-wrapper {
  margin-bottom: 2.5rem;
}

.main-search-input {
  // The global style overrides for .el-input__wrapper handle most of the styling.
  // We can add specific tweaks here if needed.
  .search-icon {
    margin-left: 0.5rem;
    color: #9ca3af; // text-gray-400
  }
}

.restaurant-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.75rem;
}

.loading, .error, .no-results {
  margin-top: 2.5rem;
  text-align: center;
  font-size: 1.125rem; // text-lg
  color: #6b7280; // text-gray-500
}
</style>
