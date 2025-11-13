<template>
  <div class="mobile-order-detail-page">
    <el-page-header @back="goBack" content="订单详情"></el-page-header>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="order" class="order-content">
      <el-card class="info-card">
        <template #header>
          <h3>{{ order.business?.businessName }}</h3>
        </template>
        <p><strong>订单状态:</strong> <el-tag :type="getOrderStatusInfo(order.orderState as OrderStatus).type">{{ getOrderStatusInfo(order.orderState as OrderStatus).text }}</el-tag></p>
        <p><strong>订单号:</strong> {{ order.id }}</p>
        <p><strong>下单时间:</strong> {{ new Date(order.orderDate!).toLocaleString() }}</p>
      </el-card>

      <el-card class="items-card">
        <template #header>
          <h4>商品列表</h4>
        </template>
        <div v-for="item in order.orderDetails" :key="item.id" class="order-item">
          <span>{{ item.food?.foodName }} x {{ item.quantity }}</span>
          <span>¥{{ (item.food?.foodPrice! * item.quantity!).toFixed(2) }}</span>
        </div>
        <div class="total-row">
          <strong>总计</strong>
          <strong>¥{{ order.orderTotal!.toFixed(2) }}</strong>
        </div>
      </el-card>

      <el-card class="address-card">
        <template #header>
          <h4>配送信息</h4>
        </template>
        <p><strong>配送地址:</strong> {{ order.deliveryAddress!.address }}</p>
        <p><strong>联系人:</strong> {{ order.deliveryAddress!.contactName }}</p>
        <p><strong>联系电话:</strong> {{ order.deliveryAddress!.contactTel }}</p>
      </el-card>

      <el-card v-if="review" class="review-card">
        <template #header>
          <h4>我的评价</h4>
        </template>
        <el-rate :model-value="review.stars" disabled size="small" />
        <p>{{ review.content }}</p>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getOrderById } from '../../../api/order';
import { getOrderReview } from '../../../api/review';
import type { Order, Review, OrderStatus } from '../../../api/types';
import { getOrderStatusInfo, OrderStatus as OrderStatusEnum } from '../../../api/types';

const route = useRoute();
const router = useRouter();
const order = ref<Order | null>(null);
const review = ref<Review | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

const fetchOrderDetails = async () => {
  loading.value = true;
  error.value = null;
  const orderId = Number(route.params.id);

  if (isNaN(orderId)) {
    error.value = '无效的订单ID。';
    loading.value = false;
    return;
  }

  try {
    const res = await getOrderById(orderId);
    if (res.success) {
      order.value = res.data;
      if (order.value.orderState === OrderStatusEnum.COMMENTED) {
        const reviewRes = await getOrderReview(orderId);
        if (reviewRes.success) {
          review.value = reviewRes.data;
        }
      }
    } else {
      throw new Error(res.message);
    }
  } catch (err: any) {
    error.value = err.message || '获取订单详情失败';
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  router.back();
};

onMounted(fetchOrderDetails);
</script>

<style scoped>
.mobile-order-detail-page { padding: 1rem; }
.el-page-header { margin-bottom: 1.5rem; }
.info-card, .items-card, .address-card, .review-card { margin-bottom: 1rem; }
.order-item, .total-row { display: flex; justify-content: space-between; padding: 0.5rem 0; }
.total-row { font-size: 1.1rem; border-top: 1px solid #eee; margin-top: 0.5rem; }
</style>