<template>
  <div class="mobile-apply-merchant-page">
    <el-card>
      <template #header>
        <h3>申请成为商家</h3>
      </template>
      <el-form :model="merchantForm" ref="formRef" label-position="top">
        <el-form-item label="店铺名称" prop="businessName" :rules="{ required: true, message: '店铺名称不能为空' }">
          <el-input v-model="merchantForm.businessName" />
        </el-form-item>
        <el-form-item label="店铺地址" prop="businessAddress" :rules="{ required: true, message: '店铺地址不能为空' }">
          <el-input v-model="merchantForm.businessAddress" />
        </el-form-item>
        <el-form-item label="店铺介绍" prop="businessExplain">
          <el-input type="textarea" v-model="merchantForm.businessExplain" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitApplication" :loading="isSubmitting">提交申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAuthStore } from '../../../../store/auth';
import { addBusiness } from '../../../../api/business';
import type { Business } from '../../../../api/types';
import { ElMessage, type FormInstance } from 'element-plus';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();
const merchantForm = ref<Partial<Business>>({});
const formRef = ref<FormInstance>();
const isSubmitting = ref(false);

const submitApplication = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  if (!authStore.user) {
    ElMessage.error('用户未登录，无法申请。');
    return;
  }
  isSubmitting.value = true;
  try {
    const payload: Partial<Business> = {
      ...merchantForm.value,
      businessOwner: authStore.user,
    };
    const res = await addBusiness(payload);
    if (res.success) {
      ElMessage.success('申请成功！请等待审核。');
      router.push('/mobile/profile');
    } else {
      throw new Error(res.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '申请失败');
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
.mobile-apply-merchant-page {
  padding: 1rem;
}
</style>