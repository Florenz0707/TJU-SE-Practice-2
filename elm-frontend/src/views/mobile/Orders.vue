<template>
  <div class="mobile-orders-page">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="orders.length > 0" class="order-list">
      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="card-header">
          <h4>{{ order.business?.businessName }}</h4>
          <span class="status">{{ getOrderStatusText(order.orderState) }}</span>
        </div>
        <div class="card-body">
          <p>总价: ¥{{ order.orderTotal!.toFixed(2) }}</p>
          <p>日期: {{ new Date(order.createTime!).toLocaleDateString() }}</p>
        </div>
        <div class="card-footer">
          <el-button size="small" @click="viewOrderDetails(order.id!)">查看详情</el-button>
          <el-button v-if="order.orderState === 3" size="small" type="primary" @click="goToReview(order.id!)">评价</el-button>
        </div>
      </div>
    </div>
    <div v-else-if="!loading" class="empty-orders">
      <h3>没有历史订单</h3>
      <p>去下一单吧！</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { listOrders } from '../../api/order';
import { getActualUser } from '../../api/user';
import type { Order } from '../../api/types';

const loading = ref(false);
const error = ref<string | null>(null);
const orders = ref<Order[]>([]);
const router = useRouter();

const getOrderStatusText = (state?: number) => {
  switch (state) {
    case 0: return '待支付';
    case 1: return '已支付';
    case 2: return '配送中';
    case 3: return '已送达';
    case 4: return '已取消';
    default: return '未知状态';
  }
};

onMounted(async () => {
  loading.value = true;
  try {
    const userRes = await getActualUser();
    if (!userRes.success || !userRes.data.id) {
      throw new Error(userRes.message || '无法获取当前用户信息');
    }
    const res = await listOrders(userRes.data.id);
    if (res.success) {
      orders.value = res.data;
    } else {
      throw new Error(res.message || '获取订单失败');
    }
  } catch (err: any) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
});

const viewOrderDetails = (id: number) => {
  router.push({ name: 'MobileOrderDetail', params: { id } });
};

const goToReview = (id: number) => {
  router.push({ name: 'MobileSubmitReview', params: { orderId: id } });
};
</script>

<style scoped>
.mobile-orders-page { padding: 1rem; }
.order-card { background: #fff; border-radius: 8px; padding: 1rem; margin-bottom: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; padding-bottom: 0.5rem; margin-bottom: 0.5rem; }
.card-header h4 { font-size: 1.1rem; margin: 0; }
.status { font-weight: bold; color: var(--el-color-primary); }
.card-footer { text-align: right; margin-top: 1rem; }
.empty-orders { text-align: center; padding: 4rem 0; }
</style>