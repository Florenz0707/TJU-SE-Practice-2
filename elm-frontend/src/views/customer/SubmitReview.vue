<template>
  <div class="submit-review-container">
    <h2>评价订单</h2>
    <el-card v-if="order">
      <p>订单号: #{{ order.id }}</p>
      <p>商家: {{ order.business?.businessName }}</p>
      <el-form @submit.prevent="submitReview">
        <el-form-item label="评分">
  <el-rate v-model="rating" :max="5" allow-half />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input type="textarea" v-model="comment" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="isAnonymous">匿名评价</el-checkbox>
        </el-form-item>
        <el-button type="primary" native-type="submit">提交评价</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../../store/auth';
import { getOrderById } from '../../api/order';
import { addReview } from '../../api/review';
import type { Order } from '../../api/types';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const order = ref<Order | null>(null);
const rating = ref(0);
const comment = ref('');
const isAnonymous = ref(false);

onMounted(async () => {
  const orderId = Number(route.params.orderId);
  if (orderId) {
    const res = await getOrderById(orderId);
    if (res.success) {
      order.value = res.data;
    } else {
      ElMessage.error('加载订单信息失败');
    }
  }
});

const submitReview = async () => {
  if (!order.value || !order.value.id || !order.value.business) return;

  const reviewData = {
    stars: rating.value * 2,
    content: comment.value,
    anonymous: isAnonymous.value,
    order: { id: order.value.id },
    business: order.value.business,
  };

  try {
    const res = await addReview(order.value.id, reviewData);
    if (res.success && res.data?.id) {
      // Points will be awarded automatically by the backend
      ElMessage.success('评价成功！积分已发放');
      router.push({ name: 'OrderHistory' });
    } else {
      ElMessage.error(res.message || '提交评价失败');
    }
  } catch (error) {
    ElMessage.error('提交评价失败');
  }
};
</script>

<style scoped>
.submit-review-container {
  padding: 20px;
}
</style>