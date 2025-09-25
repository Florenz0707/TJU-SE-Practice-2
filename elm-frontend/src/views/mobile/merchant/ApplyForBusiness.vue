<template>
  <div class="apply-for-business-mobile">
    <div class="form-container">
      <h2>开店申请</h2>
      <form @submit.prevent="submitApplication">
        <div class="form-group">
          <label for="businessName">店铺名称</label>
          <input type="text" id="businessName" v-model="application.businessName" required>
        </div>
        <div class="form-group">
          <label for="businessAddress">店铺地址</label>
          <input type="text" id="businessAddress" v-model="application.businessAddress" required>
        </div>
        <div class="form-group">
          <label for="businessExplain">店铺简介</label>
          <textarea id="businessExplain" v-model="application.businessExplain" rows="3"></textarea>
        </div>
        <div class="form-group">
          <label for="startPrice">起送价</label>
          <input type="number" id="startPrice" v-model.number="application.startPrice" required>
        </div>
        <div class="form-group">
          <label for="deliveryPrice">配送费</label>
          <input type="number" id="deliveryPrice" v-model.number="application.deliveryPrice" required>
        </div>
        <div class="form-group">
          <label for="applicationExplain">申请说明</label>
          <textarea id="applicationExplain" v-model="application.applicationExplain" rows="3"></textarea>
        </div>
        <button type="submit" class="submit-btn">提交申请</button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { submitBusinessApplication } from '@/api/applicationService';
import { useAuthStore } from '@/store/auth';

const authStore = useAuthStore();
const application = ref({
  businessName: '',
  businessAddress: '',
  businessExplain: '',
  startPrice: 0,
  deliveryPrice: 0,
  applicationExplain: '',
});

const submitApplication = async () => {
  if (!authStore.user) {
    ElMessage.error('请先登录');
    return;
  }
  try {
    await submitBusinessApplication({
      business: {
        businessName: application.value.businessName,
        businessAddress: application.value.businessAddress,
        businessExplain: application.value.businessExplain,
        startPrice: application.value.startPrice,
        deliveryPrice: application.value.deliveryPrice,
        businessOwner: { id: authStore.user.id, username: authStore.user.username }
      },
      applicationExplain: application.value.applicationExplain,
    });
    ElMessage.success('申请提交成功，请等待审核');
  } catch (error) {
    console.error('申请提交失败:', error);
    ElMessage.error('申请提交失败');
  }
};
</script>

<style scoped>
.apply-for-business-mobile {
  padding: 1rem;
  background-color: #f4f4f5;
  min-height: 100vh;
}

.form-container {
  background-color: #fff;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

h2 {
  text-align: center;
  margin-bottom: 1.5rem;
  font-size: 1.5rem;
  color: #303133;
}

.form-group {
  margin-bottom: 1rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #606266;
}

input[type="text"],
input[type="number"],
textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 1rem;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

input[type="text"]:focus,
input[type="number"]:focus,
textarea:focus {
  border-color: var(--el-color-primary);
  outline: none;
}

textarea {
  resize: vertical;
}

.submit-btn {
  width: 100%;
  padding: 0.875rem;
  border: none;
  border-radius: 8px;
  background-color: var(--el-color-primary);
  color: #fff;
  font-size: 1.125rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.submit-btn:hover {
  background-color: var(--el-color-primary-dark-2);
}
</style>