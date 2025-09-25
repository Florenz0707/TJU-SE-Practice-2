<template>
  <div class="mobile-user-profile-page">
    <el-card>
      <template #header>
        <h3>我的信息</h3>
      </template>
      <el-form :model="profileForm" ref="formRef" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="firstName">
          <el-input v-model="profileForm.firstName" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="profileForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="updateProfile" :loading="isSubmitting">更新信息</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useAuthStore } from '../../../store/auth';
import { getPersonById, updatePerson } from '../../../api/person';
import type { Person } from '../../../api/types';
import { ElMessage, type FormInstance } from 'element-plus';

const authStore = useAuthStore();
const profileForm = ref<Partial<Person>>({});
const formRef = ref<FormInstance>();
const isSubmitting = ref(false);

onMounted(async () => {
  if (authStore.user?.id) {
    const res = await getPersonById(authStore.user.id);
    if (res.success) {
      profileForm.value = res.data;
    }
  }
});

const updateProfile = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  isSubmitting.value = true;
  try {
    const res = await updatePerson(profileForm.value);
    if (res.success) {
      ElMessage.success('信息更新成功！');
    } else {
      throw new Error(res.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败');
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
.mobile-user-profile-page {
  padding: 1rem;
}
</style>