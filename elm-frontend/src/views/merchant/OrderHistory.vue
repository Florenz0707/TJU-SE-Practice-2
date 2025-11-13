<template>
  <div class="order-history-container" v-loading="loading">
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

    <div v-if="!showNoBusinessMessage">
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
            <el-tag :type="getOrderStatusInfo(row.orderState as OrderStatus).type">
              {{ getOrderStatusInfo(row.orderState as OrderStatus).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button
              v-if="row.orderState === 1"
              type="primary"
              size="small"
              @click="handleUpdateStatus(row, 2)"
            >
              接单
            </el-button>
            <el-button
              v-if="row.orderState === 2"
              type="warning"
              size="small"
              @click="handleUpdateStatus(row, 3)"
            >
              开始配送
            </el-button>
            <el-button
              v-if="row.orderState === 3"
              type="success"
              size="small"
              @click="handleUpdateStatus(row, 4)"
            >
              完成订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-empty v-if="showNoBusinessMessage" description="您当前未选择任何店铺，或您还未开设店铺。">
      <el-button type="primary" @click="$router.push({ name: 'MyApplications' })">申请开店</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { getOrdersByBusinessId, updateOrderStatus } from '../../api/order';
import { useBusinessStore } from '../../store/business';
import type { Order, OrderStatus } from '../../api/types';
import { getOrderStatusInfo } from '../../api/types';
import { ElMessage } from 'element-plus';
import { storeToRefs } from 'pinia';

const loading = ref(true);
const allOrders = ref<Order[]>([]);
const searchQuery = ref('');

const businessStore = useBusinessStore();
const { selectedBusinessId } = storeToRefs(businessStore);

const fetchOrdersForBusiness = async (businessId: number | null) => {
  loading.value = true;
  if (!businessId) {
    allOrders.value = [];
    loading.value = false;
    return;
  }
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
  fetchOrdersForBusiness(newId);
}, { immediate: true });

const showNoBusinessMessage = computed(() => !selectedBusinessId.value && !loading.value);

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

const handleUpdateStatus = async (order: Order, newStatus: OrderStatus) => {
  const orderId = order.id;
  if (!orderId) {
    ElMessage.error('无法更新没有ID的订单');
    return;
  }
  loading.value = true;
  try {
    const response = await updateOrderStatus({ id: orderId, orderState: newStatus });
    if (response.success) {
      ElMessage.success('订单状态更新成功！');
      await fetchOrdersForBusiness(selectedBusinessId.value); // Re-fetch orders
    } else {
      ElMessage.error(response.message || '更新订单状态失败');
    }
  } catch (error) {
    console.error('Failed to update order status:', error);
    ElMessage.error('更新订单状态失败');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.order-history-container {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.actions {
  display: flex;
  gap: 1rem;
}
.search-input {
  width: 300px;
}
</style>
