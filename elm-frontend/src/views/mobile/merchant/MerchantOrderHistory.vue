<template>
  <div class="mobile-order-history" v-loading="loading">
    <div class="header">
      <h4>历史订单查询</h4>
    </div>
    <el-input
      v-model="searchQuery"
      placeholder="搜索订单ID或顾客信息"
      class="search-input"
      @keyup.enter="handleSearch"
      clearable
      @clear="handleSearch"
      size="large"
    >
      <template #prefix>
        <Search :size="18" />
      </template>
    </el-input>

    <div v-if="filteredOrders.length" class="order-list">
      <el-card v-for="order in filteredOrders" :key="order.id" class="order-card">
        <div class="order-info">
          <p class="order-id"><strong>订单 #{{ order.id }}</strong></p>
          <p class="customer-name">顾客: {{ order.customer?.username ?? 'N/A' }}</p>
          <p class="order-time">{{ order.orderDate ? new Date(order.orderDate).toLocaleString() : 'N/A' }}</p>
        </div>
        <div class="order-status">
          <p class="order-total">¥{{ (order.orderTotal ?? 0).toFixed(2) }}</p>
          <el-tag :type="getOrderStatusType(order.orderState)" size="small">
            {{ getOrderStatusText(order.orderState) }}
          </el-tag>
        </div>
      </el-card>
    </div>
    <el-empty v-else description="没有找到相关订单"></el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { Search } from 'lucide-vue-next';
import { getOrdersByBusinessId } from '../../../api/order';
import { useBusinessStore } from '../../../store/business';
import type { Order } from '../../../api/types';
import { ElMessage } from 'element-plus';
import { storeToRefs } from 'pinia';

const loading = ref(true);
const allOrders = ref<Order[]>([]);
const searchQuery = ref('');

const businessStore = useBusinessStore();
const { selectedBusinessId } = storeToRefs(businessStore);

const fetchOrdersForBusiness = async (businessId: number) => {
  if (!businessId) {
    allOrders.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getOrdersByBusinessId(businessId);
    if (res.success) {
      allOrders.value = res.data || [];
    } else {
      ElMessage.error(res.message || '获取订单列表失败');
    }
  } catch (error) {
    ElMessage.error('加载订单历史失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

watch(selectedBusinessId, (newId) => {
  if (newId) {
    fetchOrdersForBusiness(newId);
  }
}, { immediate: true });

const filteredOrders = computed(() => {
  if (!searchQuery.value) {
    return allOrders.value;
  }
  const query = searchQuery.value.toLowerCase();
  return allOrders.value.filter(order =>
    (order.id?.toString() ?? '').includes(query) ||
    (order.customer?.username?.toLowerCase() ?? '').includes(query)
  );
});

const handleSearch = () => {
  // The computed property already handles filtering
};

const getOrderStatusText = (status?: number): string => {
  if (status === undefined) return '未知状态';
  const statusMap: { [key: number]: string } = {
    0: '已取消', 1: '未支付', 2: '配送中', 3: '已完成', 4: '已评价',
  };
  return statusMap[status] || '未知状态';
};

const getOrderStatusType = (status?: number): string => {
  if (status === undefined) return 'info';
  const typeMap: { [key: number]: string } = {
    0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info',
  };
  return typeMap[status] || 'info';
};
</script>

<style scoped>
.mobile-order-history {
  padding: 1rem;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.business-selector {
  flex-grow: 1;
  max-width: 200px;
}
.search-input {
  margin-bottom: 1.5rem;
}
.order-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.order-card {
  width: 100%;
}
.order-card :deep(.el-card__body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-info p, .order-status p {
  margin: 0.25rem 0;
  font-size: 0.875rem;
}
.order-id {
  font-weight: bold;
}
.order-time {
  font-size: 0.75rem;
  color: #909399;
}
.order-status {
  text-align: right;
}
.order-total {
  font-weight: bold;
  font-size: 1rem;
  color: #f56c6c;
}
</style>