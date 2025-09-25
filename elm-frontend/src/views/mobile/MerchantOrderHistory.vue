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
import { ref, onMounted, computed } from 'vue';
import { Search } from 'lucide-vue-next';
import { listOrders } from '../../api/order';
import { getCurrentUserBusinesses } from '../../api/business';
import type { Order, Business, HttpResultListBusiness } from '../../api/types';
import { ElMessage } from 'element-plus';

const loading = ref(true);
const allOrders = ref<Order[]>([]);
const business = ref<Business | null>(null);
const searchQuery = ref('');

const fetchBusinessAndOrders = async () => {
  loading.value = true;
  try {
    const businessResponse: HttpResultListBusiness = await getCurrentUserBusinesses();
    if (businessResponse.success && businessResponse.data && businessResponse.data.length > 0) {
      const currentBusiness = businessResponse.data[0];
      if (currentBusiness) {
        business.value = currentBusiness;
        const ownerId = currentBusiness.businessOwner?.id;
        if (ownerId) {
          const ordersResponse = await listOrders(ownerId);
          if (ordersResponse.success) {
            allOrders.value = ordersResponse.data || [];
          } else {
            ElMessage.error(ordersResponse.message || '获取订单列表失败');
          }
        } else {
          ElMessage.warning('无法确定店铺所有者，无法加载订单');
        }
      } else {
         ElMessage.warning('Could not retrieve business details.');
      }
    } else {
      ElMessage.warning(businessResponse.message || '当前用户没有关联的店铺');
    }
  } catch (error) {
    ElMessage.error('加载订单历史失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchBusinessAndOrders);

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
  margin-bottom: 1rem;
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