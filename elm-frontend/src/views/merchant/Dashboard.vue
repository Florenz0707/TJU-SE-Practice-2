<template>
  <div class="dashboard-container" v-loading="loading">
    <div class="header">
      <h2>实时订单仪表盘</h2>
      <p>WebSocket 状态:
        <el-tag :type="isConnected ? 'success' : 'danger'">
          {{ isConnected ? '已连接' : '已断开' }}
        </el-tag>
      </p>
    </div>

    <el-row :gutter="20">
      <!-- New Orders Column -->
      <el-col :span="12">
        <h3>新进订单</h3>
        <div class="order-list new-orders">
          <el-card v-for="order in newOrders" :key="order.id" class="order-card">
            <div class="order-content">
              <p><strong>订单 #{{ order.id }}</strong> 来自 {{ order.customer?.username ?? 'N/A' }}</p>
              <p>总价: ¥{{ (order.orderTotal ?? 0).toFixed(2) }}</p>
              <p>下单时间: {{ order.orderDate ? new Date(order.orderDate).toLocaleTimeString() : 'N/A' }}</p>
            </div>
            <div class="order-actions" v-if="order.id">
              <el-button type="success" @click="handleAccept(order.id!)">接单</el-button>
              <el-button type="danger" @click="handleReject(order.id!)">拒单</el-button>
            </div>
          </el-card>
          <el-empty v-if="newOrders.length === 0" description="暂无新订单"></el-empty>
        </div>
      </el-col>

      <!-- In-Progress Orders Column -->
      <el-col :span="12">
        <h3>进行中订单</h3>
        <div class="order-list in-progress-orders">
          <el-card v-for="order in inProgressOrders" :key="order.id" class="order-card">
             <div class="order-content">
              <p><strong>订单 #{{ order.id }}</strong> 来自 {{ order.customer?.username ?? 'N/A' }}</p>
              <p>状态:
                <el-tag :type="getOrderStatusType(order.orderState)">
                  {{ getOrderStatusText(order.orderState) }}
                </el-tag>
              </p>
            </div>
            <div class="order-actions" v-if="order.id">
              <el-button v-if="order.orderState === 1" @click="updateStatus(order.id!, 2)">开始备餐</el-button>
              <el-button v-if="order.orderState === 2" @click="updateStatus(order.id!, 3)">准备就绪</el-button>
              <el-button v-if="order.orderState === 3" @click="updateStatus(order.id!, 5)">完成订单</el-button>
            </div>
          </el-card>
           <el-empty v-if="inProgressOrders.length === 0" description="暂无进行中订单"></el-empty>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useWebSocket } from '../../utils/useWebSocket';
import { listOrdersByUserId, updateOrderStatus } from '../../api/order';
import { getCurrentUserBusinesses } from '../../api/business';
import type { Order, Business, HttpResultListBusiness } from '../../api/types';
import { ElMessage } from 'element-plus';

// Assuming WebSocket URL is configured elsewhere, using a placeholder
const WEBSOCKET_URL = 'ws://localhost:8080/api/ws/orders';

const loading = ref(true);
const business = ref<Business | null>(null);
const newOrders = ref<Order[]>([]);
const inProgressOrders = ref<Order[]>([]);

const { isConnected, message } = useWebSocket(WEBSOCKET_URL);

const fetchInitialData = async () => {
  loading.value = true;
  try {
    const businessResponse: HttpResultListBusiness = await getCurrentUserBusinesses();
    if (businessResponse.success && businessResponse.data && businessResponse.data.length > 0) {
      const currentBusiness = businessResponse.data[0];
      if (!currentBusiness) {
        ElMessage.warning('当前用户没有关联的店铺');
        return;
      }
      business.value = currentBusiness;
      const ownerId = currentBusiness.businessOwner?.id;
      if (ownerId) {
        const allFetchedOrders = await listOrdersByUserId(ownerId);
        newOrders.value = allFetchedOrders.filter(o => o.orderState === 0); // New
        inProgressOrders.value = allFetchedOrders.filter(o => o.orderState !== undefined && o.orderState > 0 && o.orderState < 5);
      }
    } else {
       ElMessage.warning(businessResponse.message || '当前用户没有关联的店铺');
    }
  } catch (error) {
    ElMessage.error('加载初始订单数据失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchInitialData);

watch(message, (newMessage) => {
  if (newMessage && typeof newMessage === 'object' && 'type' in newMessage && newMessage.type === 'NEW_ORDER') {
    const order = newMessage.payload as Order;
    if (order.business?.id === business.value?.id && !newOrders.value.some(o => o.id === order.id)) {
      newOrders.value.unshift(order);
      ElMessage.success(`收到新订单 #${order.id}`);
    }
  }
});

const updateStatus = async (orderId: number, newStatus: number) => {
  loading.value = true;
  try {
    const res = await updateOrderStatus(orderId, newStatus);
    if (res.success) {
      ElMessage.success(`订单 #${orderId} 状态已更新`);
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

const handleAccept = (orderId: number) => {
  updateStatus(orderId, 1); // 1: Accepted
};

const handleReject = (orderId: number) => {
  updateStatus(orderId, 6); // 6: Cancelled
};

const getOrderStatusText = (status?: number): string => {
  if (status === undefined) return '未知状态';
  const statusMap: { [key: number]: string } = {
    0: '已下单', 1: '商家已接单', 2: '准备中', 3: '待取餐', 4: '配送中', 5: '已送达', 6: '已取消',
  };
  return statusMap[status] || '未知状态';
};

const getOrderStatusType = (status?: number): string => {
  if (status === undefined) return 'info';
  const typeMap: { [key: number]: string } = {
    0: 'info', 1: 'primary', 2: 'primary', 3: 'warning', 4: 'warning', 5: 'success', 6: 'danger',
  };
  return typeMap[status] || 'info';
};
</script>

<style scoped>
.dashboard-container { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.order-list { height: 60vh; overflow-y: auto; padding: 10px; border: 1px solid #eee; border-radius: 4px; }
.order-card { margin-bottom: 15px; }
.order-content { margin-bottom: 10px; }
.order-actions { text-align: right; }
</style>
