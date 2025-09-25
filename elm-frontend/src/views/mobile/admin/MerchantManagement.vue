<template>
  <div class="mobile-merchant-management">
    <div class="filters">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="按店铺名称搜索"
        class="search-input"
      />
      <select v-model="statusFilter" class="status-select">
        <option value="">所有状态</option>
        <option value="待处理">待处理</option>
        <option value="已批准">已批准</option>
        <option value="已拒绝">已拒绝</option>
      </select>
    </div>

    <div v-if="filteredBusinesses.length" class="business-list">
      <div v-for="business in filteredBusinesses" :key="business.id" class="business-card">
        <div class="business-header">
          <span class="business-name">{{ business.businessName }}</span>
          <span :class="['status-badge', `status-${business.status.toLowerCase()}`]">{{ business.status }}</span>
        </div>
        <div class="business-body">
          <p><strong>所有者:</strong> {{ business['businessOwner.username'] }}</p>
          <p><strong>地址:</strong> {{ business.businessAddress }}</p>
        </div>
      </div>
    </div>
    <div v-else class="no-results">
      <p>没有找到符合条件的店铺。</p>
    </div>

    <div class="floating-notice">
      <p>店铺审批和编辑功能请在桌面端进行操作。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { getBusinesses } from '@/api/business';
import type { Business } from '@/api/types';

const rawBusinesses = ref<Business[]>([]);
const searchQuery = ref('');
const statusFilter = ref('');

const getStatus = (business: Business): '待处理' | '已批准' | '已拒绝' => {
  if (business.remarks?.includes('pending')) return '待处理';
  if (business.deleted) return '已拒绝';
  return '已批准';
};

const fetchBusinesses = async () => {
  try {
    const res = await getBusinesses();
    if (res.success) {
      rawBusinesses.value = res.data || [];
    } else {
      ElMessage.error(res.message || '获取店铺列表失败。');
    }
  } catch (error) {
    ElMessage.error('获取店铺列表失败。');
  }
};

onMounted(fetchBusinesses);

const displayBusinesses = computed(() => {
  return rawBusinesses.value.map(b => ({
    ...b,
    status: getStatus(b),
    'businessOwner.username': b.businessOwner?.username || 'N/A',
  }));
});

const filteredBusinesses = computed(() => {
  return displayBusinesses.value.filter(business => {
    const searchMatch = !searchQuery.value || business.businessName.toLowerCase().includes(searchQuery.value.toLowerCase());
    const statusMatch = !statusFilter.value || business.status === statusFilter.value;
    return searchMatch && statusMatch;
  });
});
</script>

<style scoped>
.mobile-merchant-management {
  padding: 1rem;
}

.filters {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.search-input, .status-select {
  width: 100%;
  padding: 0.75rem;
  font-size: 1rem;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  box-sizing: border-box;
}

.business-list {
  display: grid;
  gap: 1rem;
}

.business-card {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.business-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.business-name {
  font-weight: 600;
  font-size: 1.125rem;
}

.status-badge {
  padding: 0.25rem 0.6rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  color: #fff;
}

.status-待处理 { background-color: #e6a23c; }
.status-已批准 { background-color: #67c23a; }
.status-已拒绝 { background-color: #f56c6c; }

.business-body {
  padding: 1rem;
  font-size: 0.875rem;
  color: #495057;
}

.business-body p {
  margin: 0.5rem 0;
}

.no-results, .floating-notice {
  text-align: center;
  color: #909399;
}

.no-results {
  padding: 2rem;
}

.floating-notice {
  position: fixed;
  bottom: 70px;
  left: 1rem;
  right: 1rem;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 0.75rem;
  border-radius: 8px;
  font-size: 0.875rem;
}
</style>