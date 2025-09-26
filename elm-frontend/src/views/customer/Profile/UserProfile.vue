<template>
  <div>
    <h2>我的资料</h2>
    <el-form v-if="user" :model="userForm" label-width="120px" style="max-width: 600px">
      <el-form-item label="用户名">
        <el-input v-model="userForm.username" disabled />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="userForm.email" />
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model="userForm.phone" />
      </el-form-item>
      <!-- Add other Person fields as needed, e.g., firstName, lastName -->
      <el-form-item>
        <el-button type="primary" @click="handleUpdateProfile">保存更改</el-button>
      </el-form-item>
    </el-form>
    <div v-else>正在加载用户数据...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useAuthStore } from '../../../store/auth';
import { updateUser, getUserById } from '../../../api/user';
import { ElMessage } from 'element-plus';
import type { Person } from '../../../api/types';

const authStore = useAuthStore();
const user = authStore.user;

// Use a local ref for the form to avoid directly mutating the store's state
const userForm = ref<Partial<Person>>({});

// Watch for changes in the store's user data and update the form
watch(
  () => authStore.user,
  (newUser) => {
    if (newUser?.id) {
      getUserById(newUser.id).then((res) => {
        if (res.success) {
          userForm.value = res.data;
        } else {
          ElMessage.error('获取用户信息失败: ' + res.message);
        }
      });
    }
  },
  { immediate: true }
);

const handleUpdateProfile = async () => {
  if (!user || !user.id) {
    ElMessage.error('用户数据不可用。');
    return;
  }
  try {
    // The API expects a full Person object, so we merge the original user data
    // with the form data to create a complete payload.
    const payload: Person = {
      ...user, // a base User object
      ...userForm.value, // an object with Person fields
      username: user.username, // ensure username is not lost
    };

    const response = await updateUser(user.id, payload);
    if (response.success) {
      // Update the store with the new user info
      authStore.setUser(response.data);
      ElMessage.success('个人资料更新成功！');
    } else {
      throw new Error(response.message || '更新个人资料失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '发生错误');
  }
};
</script>
