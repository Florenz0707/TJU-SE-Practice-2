<template>
  <div class="p-8">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>开店申请</span>
        </div>
      </template>
      <el-form :model="business" label-width="120px">
        <el-form-item label="店铺名称">
          <el-input v-model="business.businessName" />
        </el-form-item>
        <el-form-item label="店铺地址">
          <el-input v-model="business.businessAddress" />
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input v-model="business.businessExplain" type="textarea" />
        </el-form-item>
        <el-form-item label="起送价">
          <el-input-number v-model="business.startPrice" :min="0" />
        </el-form-item>
        <el-form-item label="配送费">
          <el-input-number v-model="business.deliveryPrice" :min="0" />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="applicationExplain" type="textarea" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitApplication">提交申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { submitBusinessApplication } from '../../api/applicationService';
import type { Business } from '../../api/types';

const router = useRouter();

const business = ref<Business>({
  businessName: '',
  businessAddress: '',
  businessExplain: '',
  startPrice: 0,
  deliveryPrice: 0,
});

const applicationExplain = ref('');

const submitApplication = async () => {
  try {
    await submitBusinessApplication({
      business: business.value,
      applicationExplain: applicationExplain.value,
    });
    ElMessage.success('申请已提交');
    router.push({ name: 'MyApplications' });
  } catch (error) {
    ElMessage.error('申请提交失败');
    console.error('Failed to submit application:', error);
  }
};
</script>