<template>
  <div>
    <h2>申请成为商家</h2>
    <el-form @submit.prevent="submitApplication">
      <el-form-item label="申请说明">
        <el-input type="textarea" v-model="applicationExplain" />
      </el-form-item>
      <el-button type="primary" native-type="submit">提交申请</el-button>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { applyToBeMerchant } from '../../../api/application';
import { ElMessage } from 'element-plus';

const applicationExplain = ref('');

const submitApplication = async () => {
  try {
    const res = await applyToBeMerchant({ applicationExplain: applicationExplain.value });
    if (res.success) {
      ElMessage.success('申请成功，请等待管理员审核');
    } else {
      ElMessage.error(res.message || '申请失败');
    }
  } catch (error) {
    ElMessage.error('申请失败');
  }
};
</script>