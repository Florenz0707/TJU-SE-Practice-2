<template>
  <div class="order-history-container" v-loading="loading">
    <el-card>
      <template #header>
        <div class="header">
          <h2>历史订单查询</h2>
          <div class="actions">
            <el-input
              v-model="searchQuery"
              placeholder="搜索订单ID或顾客信息"
              class="search-input"
              @keyup.enter="handleSearch"
              clearable
              @clear="handleSearch"
            >
              <template #append>
                <el-button @click="handleSearch">搜索</el-button>
              </template>
            </el-input>
          </div>
        </div>
      </template>

      <el-table :data="filteredOrders" stripe style="width: 100%">
        <el-table-column prop="id" label="订单ID" width="100" />
        <el-table-column prop="orderDate" label="下单时间" width="200">
          <template #default="{ row }">
            <span>{{ row.orderDate ? new Date(row.orderDate).toLocaleString() : 'N/A' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="customer.username" label="顾客用户名" />
        <el-table-column prop="orderTotal" label="订单总额">
          <template #default="{ row }">
            <span>¥{{ (row.orderTotal ?? 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderState" label="订单状态">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusType(row.orderState)">
              {{ getOrderStatusText(row.orderState) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { getOrdersByBusinessId } from '../../api/order';
import { useBusinessStore } from '../../store/business';
import type { Order } from '../../api/types';
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
.order-history-container {
  padding: 20px;
  background-color: #f5f7fa;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.actions {
  display: flex;
  gap: 1rem;
}
.search-input {
  width: 300px;
}
</style>
