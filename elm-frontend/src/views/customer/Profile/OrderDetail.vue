<template>
  <div>
    <el-page-header @back="goBack" title="返回订单列表">
      <template #content>
        <span class="text-large font-600 mr-3"> 订单详情 </span>
      </template>
    </el-page-header>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <el-card v-if="order" class="order-summary-card">
      <template #header>
        <h3>订单 #{{ order.id }}</h3>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="餐厅">{{ order.business?.businessName ?? '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag>{{ getStatusText(order.orderState) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单日期">{{ order.orderDate ? new Date(order.orderDate).toLocaleString() : '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ (order.orderTotal ?? 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="配送地址">{{ order.deliveryAddress?.address ?? '暂无' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="orderItems.length" class="order-items-card">
      <template #header>
        <h4>订单商品</h4>
      </template>
      <el-table :data="orderItems" stripe>
        <el-table-column prop="foodName" label="商品" />
        <el-table-column prop="foodPrice" label="价格">
          <template #default="{ row }">¥{{ row.foodPrice.toFixed(2) }}</template>
        </el-table-column>
        <!-- Quantity is not available on the Food object, so we can't display it yet -->
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getOrderById } from '../../../api/order';
import { getAllFoods } from '../../../api/food';
import type { Order, Food } from '../../../api/types';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const order = ref<Order | null>(null);
const orderItems = ref<Food[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

const getStatusText = (state?: number) => {
  if (state === undefined || state === null) return '未知';
  const statuses: { [key: number]: string } = { 1: '已下单', 2: '准备中', 3: '配送中', 4: '已送达', 5: '已取消' };
  return statuses[state] || '未知';
};

const goBack = () => {
  router.back();
};

onMounted(async () => {
  loading.value = true;
  const orderId = Number(route.params.id);
  if (!orderId) {
    error.value = "无效的订单ID";
    loading.value = false;
    return;
  }

  try {
    const [orderRes, itemsRes] = await Promise.all([
      getOrderById(orderId),
      getAllFoods({ order: orderId })
    ]);

    if (orderRes.success) {
      order.value = orderRes.data;
    } else {
      throw new Error(orderRes.message || '获取订单详情失败');
    }

    if (itemsRes.success) {
      orderItems.value = itemsRes.data;
    } else {
      // It's possible an order has no items or the endpoint fails, don't block for this
      ElMessage.warning('无法获取此订单的商品列表。');
    }

  } catch (err: any) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.order-summary-card, .order-items-card {
  margin-top: 20px;
}
</style>
