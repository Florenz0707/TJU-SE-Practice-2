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
          <el-tag :type="getOrderStatusInfo(order.orderState as OrderStatus).type">{{ getOrderStatusInfo(order.orderState as OrderStatus).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单日期">{{ order.orderDate ? new Date(order.orderDate).toLocaleString() : '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="配送地址" :span="2">{{ order.deliveryAddress?.address ?? '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="订单金额" :span="2">¥{{ (order.orderTotal ?? 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item v-if="order.voucherDiscount && order.voucherDiscount > 0" label="优惠券折扣" :span="2">
          -¥{{ order.voucherDiscount.toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.pointsUsed && order.pointsUsed > 0" label="积分抵扣" :span="2">
          {{ order.pointsUsed }}积分 (-¥{{ (order.pointsDiscount ?? 0).toFixed(2) }})
        </el-descriptions-item>
        <el-descriptions-item v-if="order.walletPaid && order.walletPaid > 0" label="钱包支付" :span="2">
          -¥{{ order.walletPaid.toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="hasDiscounts" label="实际支付" :span="2">
          <strong>¥{{ finalPayment.toFixed(2) }}</strong>
        </el-descriptions-item>
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

    <el-card v-if="review" class="review-card">
      <template #header>
        <h4>我的评价</h4>
      </template>
      <el-rate :model-value="review.stars" disabled />
      <p>{{ review.content }}</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getOrderById } from '../../../api/order';
import { getOrderReview } from '../../../api/review';
import { getAllFoods } from '../../../api/food';
import type { Order, Food, Review, OrderStatus } from '../../../api/types';
import { getOrderStatusInfo, OrderStatus as OrderStatusEnum } from '../../../api/types';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const order = ref<Order | null>(null);
const orderItems = ref<Food[]>([]);
const review = ref<Review | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

const goBack = () => {
  router.back();
};

// Check if order has any discounts or wallet payment
const hasDiscounts = computed(() => {
  if (!order.value) return false;
  return (
    (order.value.voucherDiscount && order.value.voucherDiscount > 0) ||
    (order.value.pointsUsed && order.value.pointsUsed > 0) ||
    (order.value.walletPaid && order.value.walletPaid > 0)
  );
});

// Calculate final payment amount
const finalPayment = computed(() => {
  if (!order.value) return 0;
  let total = order.value.orderTotal ?? 0;
  if (order.value.voucherDiscount) total -= order.value.voucherDiscount;
  if (order.value.pointsDiscount) total -= order.value.pointsDiscount;
  if (order.value.walletPaid) total -= order.value.walletPaid;
  return Math.max(0, total);
});

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
      // If order is reviewed, fetch the review
      if (order.value.orderState === OrderStatusEnum.COMMENTED) {
        const reviewRes = await getOrderReview(orderId);
        if (reviewRes.success) {
          review.value = reviewRes.data;
        } else {
          console.error('获取评价失败:', reviewRes.message);
        }
      }
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
.order-summary-card, .order-items-card, .review-card {
  margin-top: 20px;
}
</style>
