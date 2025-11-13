<template>
  <div class="dashboard-container" v-loading="loading">
    <div class="header">
      <h2>实时订单仪表盘</h2>
    </div>

    <el-row :gutter="20" v-if="!showNoBusinessMessage">
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
              <el-button type="success" @click="handleAccept(order)">接单</el-button>
              <el-button type="danger" @click="handleReject(order)">拒单</el-button>
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
                <el-tag :type="getOrderStatusInfo(order.orderState as OrderStatus).type">
                  {{ getOrderStatusInfo(order.orderState as OrderStatus).text }}
                </el-tag>
              </p>
            </div>
            <div class="order-actions" v-if="order.id">
              <el-button v-if="order.orderState === OrderStatusEnum.ACCEPTED" @click="updateStatus(order, OrderStatusEnum.DELIVERY)">开始配送</el-button>
              <el-button v-if="order.orderState === OrderStatusEnum.DELIVERY" @click="updateStatus(order, OrderStatusEnum.COMPLETE)">订单完成</el-button>
            </div>
          </el-card>
           <el-empty v-if="inProgressOrders.length === 0" description="暂无进行中订单"></el-empty>
        </div>
      </el-col>
    </el-row>
     <el-empty v-if="showNoBusinessMessage" description="您当前未选择任何店铺，或您还未开设店铺。">
      <el-button type="primary" @click="$router.push({ name: 'MyApplications' })">申请开店</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import { getOrdersByBusinessId, updateOrderStatus } from '../../api/order';
import { useBusinessStore } from '../../store/business';
import { storeToRefs } from 'pinia';
import type { Order, OrderStatus } from '../../api/types';
import { OrderStatus as OrderStatusEnum, getOrderStatusInfo } from '../../api/types';
import { ElMessage } from 'element-plus';

const loading = ref(true);
const newOrders = ref<Order[]>([]);
const inProgressOrders = ref<Order[]>([]);

const businessStore = useBusinessStore();
const { selectedBusinessId, businesses } = storeToRefs(businessStore);

const selectedBusiness = computed(() => {
  return businesses.value.find(b => b.id === selectedBusinessId.value);
});

const fetchInitialData = async () => {
  loading.value = true;
  if (!selectedBusiness.value) {
    newOrders.value = [];
    inProgressOrders.value = [];
    loading.value = false;
    return;
  }
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

const showNoBusinessMessage = computed(() => !selectedBusiness.value && !loading.value);

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
.dashboard-container { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.order-list { height: 60vh; overflow-y: auto; padding: 10px; border: 1px solid #eee; border-radius: 4px; }
.order-card { margin-bottom: 15px; }
.order-content { margin-bottom: 10px; }
.order-actions { text-align: right; }
</style>
