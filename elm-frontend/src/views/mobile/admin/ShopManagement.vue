<template>
  <div class="mobile-shop-management">
    <div class="filters">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="按店铺名称搜索"
        class="search-input"
      />
      <select v-model="statusFilter" class="status-select">
        <option value="">所有状态</option>
        <option value="0">待处理</option>
        <option value="1">已批准</option>
        <option value="2">已拒绝</option>
      </select>
    </div>

    <div v-if="filteredApplications.length" class="application-list">
      <div v-for="app in filteredApplications" :key="app.id" class="application-card">
        <div class="application-header">
          <span class="business-name">{{ app.business.businessName }}</span>
          <span :class="['status-badge', `status-${app.applicationState}`]">{{ statusText(app.applicationState) }}</span>
        </div>
        <div class="application-body">
          <p><strong>申请人:</strong> {{ app.business.businessOwner?.username || 'N/A' }}</p>
          <p><strong>申请时间:</strong> {{ new Date(app.createTime || '').toLocaleString() }}</p>
        </div>
      </div>
    </div>
    <div v-else class="no-results">
      <p>没有找到符合条件的申请。</p>
    </div>

    <div class="floating-notice">
      <p>店铺审批和编辑功能请在桌面端进行操作。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { getBusinessApplications, ApplicationState } from '@/api/applicationService';
import type { BusinessApplication } from '@/api/types';

const applications = ref<BusinessApplication[]>([]);
const searchQuery = ref('');
const statusFilter = ref('');

const fetchApplications = async () => {
  try {
    const response = await getBusinessApplications();
    applications.value = response;
  } catch (error) {
    ElMessage.error('获取申请列表失败');
    console.error('获取申请列表失败:', error);
  }
};

onMounted(fetchApplications);

const statusText = (state?: number) => {
  switch (state) {
    case ApplicationState.UNDISPOSED:
      return '待处理';
    case ApplicationState.APPROVED:
      return '已批准';
    case ApplicationState.REJECTED:
      return '已拒绝';
    default:
      return '未知';
  }
};

const filteredApplications = computed(() => {
  return applications.value.filter(app => {
    const searchMatch = !searchQuery.value || app.business.businessName.toLowerCase().includes(searchQuery.value.toLowerCase());
    const statusMatch = !statusFilter.value || app.applicationState === Number(statusFilter.value);
    return searchMatch && statusMatch;
  });
});
</script>

<style scoped>
.mobile-shop-management {
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

.application-list {
  display: grid;
  gap: 1rem;
}

.application-card {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.application-header {
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

.status-0 { background-color: #e6a23c; }
.status-1 { background-color: #67c23a; }
.status-2 { background-color: #f56c6c; }

.application-body {
  padding: 1rem;
  font-size: 0.875rem;
  color: #495057;
}

.application-body p {
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