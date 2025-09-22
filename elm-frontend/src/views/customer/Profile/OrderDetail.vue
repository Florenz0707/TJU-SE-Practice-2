<template>
  <div>
    <el-page-header @back="goBack" title="Back to Orders">
      <template #content>
        <span class="text-large font-600 mr-3"> Order Detail </span>
      </template>
    </el-page-header>

    <div v-if="loading" class="loading">Loading...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <el-card v-if="order" class="order-summary-card">
      <template #header>
        <h3>Order #{{ order.id }}</h3>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Restaurant">{{ order.business?.businessName ?? 'N/A' }}</el-descriptions-item>
        <el-descriptions-item label="Status">
          <el-tag>{{ getStatusText(order.orderState) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Order Date">{{ order.orderDate ? new Date(order.orderDate).toLocaleString() : 'N/A' }}</el-descriptions-item>
        <el-descriptions-item label="Total Amount">${{ (order.orderTotal ?? 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="Delivery Address">{{ order.deliveryAddress?.address ?? 'N/A' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="orderItems.length" class="order-items-card">
      <template #header>
        <h4>Items in this Order</h4>
      </template>
      <el-table :data="orderItems" stripe>
        <el-table-column prop="foodName" label="Item" />
        <el-table-column prop="foodPrice" label="Price">
          <template #default="{ row }">${{ row.foodPrice.toFixed(2) }}</template>
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
  if (state === undefined || state === null) return 'Unknown';
  const statuses: { [key: number]: string } = { 1: 'Placed', 2: 'Preparing', 3: 'Delivering', 4: 'Delivered', 5: 'Cancelled' };
  return statuses[state] || 'Unknown';
};

const goBack = () => {
  router.back();
};

onMounted(async () => {
  loading.value = true;
  const orderId = Number(route.params.id);
  if (!orderId) {
    error.value = "Invalid order ID";
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
      throw new Error(orderRes.message || 'Failed to fetch order details');
    }

    if (itemsRes.success) {
      orderItems.value = itemsRes.data;
    } else {
      // It's possible an order has no items or the endpoint fails, don't block for this
      ElMessage.warning('Could not fetch items for this order.');
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
