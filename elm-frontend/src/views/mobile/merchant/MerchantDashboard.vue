<template>
  <div class="mobile-dashboard-container" v-loading="loading">
    <div class="header">
      <h4>实时订单仪表盘</h4>
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
                <el-tag :type="getOrderStatusInfo(order.orderState as OrderStatus).type" size="small">
                  {{ getOrderStatusInfo(order.orderState as OrderStatus).text }}
                </el-tag>
              </p>
            </div>
            <div class="order-actions">
              <el-button v-if="order.orderState === OrderStatusEnum.ACCEPTED" size="small" @click="updateStatus(order, OrderStatusEnum.DELIVERY)">开始配送</el-button>
              <el-button v-if="order.orderState === OrderStatusEnum.DELIVERY" size="small" @click="updateStatus(order, OrderStatusEnum.COMPLETE)">订单完成</el-button>
            </div>
          </el-card>
           <el-empty v-if="inProgressOrders.length === 0" description="暂无进行中订单"></el-empty>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import { getOrdersByBusinessId, updateOrderStatus } from '../../../api/order';
import { useBusinessStore } from '../../../store/business';
import { storeToRefs } from 'pinia';
import type { Order, OrderStatus } from '../../../api/types';
import { OrderStatus as OrderStatusEnum, getOrderStatusInfo } from '../../../api/types';
import { ElMessage } from 'element-plus';

const loading = ref(true);
const newOrders = ref<Order[]>([]);
const inProgressOrders = ref<Order[]>([]);
const activeTab = ref('new');

const businessStore = useBusinessStore();
const { selectedBusinessId, businesses } = storeToRefs(businessStore);

const selectedBusiness = computed(() => {
  return businesses.value.find(b => b.id === selectedBusinessId.value);
});

const fetchInitialData = async () => {
  if (!selectedBusiness.value) {
    newOrders.value = [];
    inProgressOrders.value = [];
    return;
  }
  loading.value = true;
  try {
    const businessId = selectedBusinessId.value;
    if (businessId) {
      const response = await getOrdersByBusinessId(businessId);
      if (response.success) {
        const allOrders = response.data;
        newOrders.value = allOrders.filter((o: Order) => o.orderState === OrderStatusEnum.PAID);
        inProgressOrders.value = allOrders.filter((o: Order) =>
          o.orderState === OrderStatusEnum.ACCEPTED || o.orderState === OrderStatusEnum.DELIVERY
        );
      } else {
        ElMessage.error(response.message || '获取订单列表失败');
      }
    }
  } catch (error) {
    ElMessage.error('加载初始订单数据失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

let pollingInterval: number | null = null;

onMounted(() => {
  fetchInitialData();
  pollingInterval = window.setInterval(fetchInitialData, 15000);
});

onUnmounted(() => {
  if (pollingInterval) {
    clearInterval(pollingInterval);
  }
});

watch(selectedBusinessId, fetchInitialData);

const updateStatus = async (order: Order, newStatus: OrderStatus) => {
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
  updateStatus(order, OrderStatusEnum.ACCEPTED);
};

const handleReject = (order: Order) => {
  updateStatus(order, OrderStatusEnum.CANCELED);
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