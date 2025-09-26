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

    <h2 style="margin-top: 2rem;">修改密码</h2>
    <el-form
      ref="passwordFormRef"
      :model="passwordForm"
      :rules="passwordRules"
      label-width="120px"
      style="max-width: 600px"
      @submit.prevent="handleUpdatePassword"
    >
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleUpdatePassword">更新密码</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue';
import { useAuthStore } from '../../../store/auth';
import { updateUser, getUserById, updateUserPassword }from '../../../api/user';
import { ElMessage, ElForm, ElFormItem, ElInput, ElButton, type FormInstance } from 'element-plus';
import type { Person } from '../../../api/types';

const authStore = useAuthStore();
const user = authStore.user;

const passwordFormRef = ref<FormInstance>();
const passwordForm = reactive({
  newPassword: '',
  confirmPassword: '',
});

const passwordRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
};

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

const handleUpdatePassword = async () => {
  if (!passwordFormRef.value || !user) return;

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await updateUserPassword({
          username: user.username,
          password: passwordForm.newPassword,
        });
        if (res.success) {
          ElMessage.success('密码更新成功');
          passwordForm.newPassword = '';
          passwordForm.confirmPassword = '';
          passwordFormRef.value?.resetFields();
        } else {
          ElMessage.error(res.message || '密码更新失败');
        }
      } catch (error) {
        ElMessage.error('密码更新失败');
      }
    }
  });
};
</script>
