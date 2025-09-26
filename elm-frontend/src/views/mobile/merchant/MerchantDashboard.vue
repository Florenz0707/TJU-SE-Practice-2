<template>
  <div class="mobile-dashboard-container" v-loading="loading">
    <div class="header">
      <h4>实时订单仪表盘</h4>
      <p>
        <el-tag :type="isConnected ? 'success' : 'danger'" size="small">
          {{ isConnected ? '已连接' : '未连接' }}
        </el-tag>
      </p>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="新进订单" name="new">
        <div class="order-list new-orders">
          <el-card v-for="order in newOrders" :key="order.id" class="order-card">
            <div class="order-content">
              <p><strong>订单 #{{ order.id }}</strong> 来自 {{ order.customer?.username ?? 'N/A' }}</p>
              <p>总价: ¥{{ (order.orderTotal ?? 0).toFixed(2) }}</p>
              <p>下单时间: {{ order.orderDate ? new Date(order.orderDate).toLocaleTimeString() : 'N/A' }}</p>
            </div>
            <div class="order-actions">
              <el-button type="success" size="small" @click="handleAccept(order)">接单</el-button>
              <el-button type="danger" size="small" @click="handleReject(order)">拒单</el-button>
            </div>
          </el-card>
          <el-empty v-if="newOrders.length === 0" description="暂无新订单"></el-empty>
        </div>
      </el-tab-pane>
      <el-tab-pane label="进行中订单" name="in-progress">
        <div class="order-list in-progress-orders">
          <el-card v-for="order in inProgressOrders" :key="order.id" class="order-card">
             <div class="order-content">
              <p><strong>订单 #{{ order.id }}</strong> 来自 {{ order.customer?.username ?? 'N/A' }}</p>
              <p>状态:
                <el-tag :type="getOrderStatusType(order.orderState)" size="small">
                  {{ getOrderStatusText(order.orderState) }}
                </el-tag>
              </p>
            </div>
            <div class="order-actions">
              <el-button v-if="order.orderState === 1" size="small" @click="updateStatus(order, 2)">开始配送</el-button>
              <el-button v-if="order.orderState === 2" size="small" @click="updateStatus(order, 3)">订单完成</el-button>
            </div>
          </el-card>
           <el-empty v-if="inProgressOrders.length === 0" description="暂无进行中订单"></el-empty>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useWebSocket } from '../../../utils/useWebSocket';
import { listOrders, updateOrderStatus } from '../../../api/order';
import { useBusinessStore } from '../../../store/business';
import { storeToRefs } from 'pinia';
import type { Order } from '../../../api/types';
import { ElMessage } from 'element-plus';

const WEBSOCKET_URL = 'ws://localhost:8080/api/ws/orders';

const loading = ref(true);
const newOrders = ref<Order[]>([]);
const inProgressOrders = ref<Order[]>([]);
const activeTab = ref('new');

const businessStore = useBusinessStore();
const { selectedBusinessId, businesses } = storeToRefs(businessStore);

const selectedBusiness = computed(() => {
  return businesses.value.find(b => b.id === selectedBusinessId.value);
});

const { isConnected, message } = useWebSocket(WEBSOCKET_URL);

const fetchInitialData = async () => {
  if (!selectedBusiness.value) {
    newOrders.value = [];
    inProgressOrders.value = [];
    return;
  }
  loading.value = true;
  try {
    const ownerId = selectedBusiness.value.businessOwner?.id;
    if (ownerId) {
      const allFetchedOrdersResponse = await listOrders(ownerId);
      if (allFetchedOrdersResponse.success) {
        const allFetchedOrders = allFetchedOrdersResponse.data;
        newOrders.value = allFetchedOrders.filter((o:Order) => o.orderState === 1 && o.business?.id === selectedBusinessId.value); // unpaid
        inProgressOrders.value = allFetchedOrders.filter((o:Order) => o.orderState === 2 && o.business?.id === selectedBusinessId.value); // delivery
      } else {
        ElMessage.error(allFetchedOrdersResponse.message || '获取订单列表失败');
      }
    }
  } catch (error) {
    ElMessage.error('加载初始订单数据失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchInitialData);

watch(selectedBusinessId, fetchInitialData);

watch(message, (newMessage) => {
  if (newMessage && typeof newMessage === 'object' && 'type' in newMessage && newMessage.type === 'NEW_ORDER') {
    const order = newMessage.payload as Order;
    if (order.business?.id === selectedBusinessId.value && !newOrders.value.some(o => o.id === order.id)) {
      newOrders.value.unshift(order);
      ElMessage.success(`收到新订单 #${order.id}`);
    }
  }
});

const updateStatus = async (order: Order, newStatus: number) => {
  loading.value = true;
  try {
    const updatedOrder = { ...order, orderState: newStatus };
    const res = await updateOrderStatus(updatedOrder);
    if (res.success) {
      ElMessage.success(`订单 #${order.id} 状态已更新`);
      await fetchInitialData(); // Refresh the lists
    } else {
      ElMessage.error(res.message || '更新订单状态失败');
    }
  } catch (error) {
    ElMessage.error('更新订单状态失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handleAccept = (order: Order) => {
  updateStatus(order, 2); // 2: delivery
};

const handleReject = (order: Order) => {
  updateStatus(order, 0); // 0: Cancelled
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
.mobile-dashboard-container { padding: 1rem; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.order-list {
  max-height: 70vh;
  overflow-y: auto;
  padding: 0.5rem;
}
.order-card { margin-bottom: 1rem; }
.order-content { margin-bottom: 0.75rem; font-size: 0.875rem; }
.order-actions { text-align: right; }
p {
  margin: 0.25rem 0;
}
</style>