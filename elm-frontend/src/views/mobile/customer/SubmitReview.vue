<template>
  <div class="mobile-submit-review-page">
    <el-page-header @back="goBack" content="评价订单"></el-page-header>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="order" class="review-content">
      <el-card>
        <template #header>
          <h3>评价 {{ order.business?.businessName }}</h3>
        </template>
        <el-form :model="reviewForm" ref="formRef" label-position="top">
          <el-form-item label="评分" prop="stars" :rules="{ required: true, message: '请给出一个评分' }">
            <el-rate v-model="reviewForm.stars" :max="5" show-text />
          </el-form-item>
          <el-form-item label="评价内容" prop="content" :rules="{ required: true, message: '评价内容不能为空' }">
            <el-input type="textarea" v-model="reviewForm.content" rows="4" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitReview" :loading="isSubmitting">提交评价</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getOrderById } from '../../../api/order';
import { addReview } from '../../../api/review';
import type { Order, Review } from '../../../api/types';
import { ElMessage, type FormInstance } from 'element-plus';
import { useAuthStore } from '../../../store/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const order = ref<Order | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);
const isSubmitting = ref(false);
const formRef = ref<FormInstance>();
const reviewForm = ref({
  stars: 0,
  content: '',
});

onMounted(async () => {
  loading.value = true;
  const orderId = Number(route.params.orderId);
  if (isNaN(orderId)) {
    error.value = '无效的订单ID';
    loading.value = false;
    return;
  }
  try {
    const res = await getOrderById(orderId);
    if (res.success) {
      order.value = res.data;
    } else {
      throw new Error(res.message);
    }
  } catch (err: any) {
    error.value = err.message || '获取订单失败';
  } finally {
    loading.value = false;
  }
});

const submitReview = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();

  if (!authStore.user || !order.value) {
    ElMessage.error('无法提交评价，缺少必要信息。');
    return;
  }

  isSubmitting.value = true;
  try {
    const reviewData: Partial<Review> = {
      order: order.value,
      customer: authStore.user,
      business: order.value.business,
      stars: reviewForm.value.stars,
      content: reviewForm.value.content,
    };
    const res = await addReview(order.value.id!, reviewData);
    if (res.success) {
      ElMessage.success('评价成功！');
      router.push('/mobile/orders');
    } else {
      throw new Error(res.message);
    }
  } catch (err: any) {
    ElMessage.error(err.message || '提交评价失败');
  } finally {
    isSubmitting.value = false;
  }
};

const goBack = () => {
  router.back();
};
</script>

<style scoped>
.mobile-submit-review-page { padding: 1rem; }
.el-page-header { margin-bottom: 1.5rem; }
.el-rate { height: auto; }
</style>