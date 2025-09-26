<template>
  <div class="user-profile-container" v-loading="loading">
    <el-card>
      <div class="user-info-header">
        <el-avatar :size="100" :src="userForm.photo" icon="el-icon-user-solid" />
        <div class="user-details">
          <h2>{{ user?.username }}</h2>
          <p>ID: {{ user?.id }}</p>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 2rem;">
      <h3>我的资料</h3>
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
        <el-form-item>
          <el-button type="primary" @click="handleUpdateProfile">保存更改</el-button>
        </el-form-item>
      </el-form>
      <div v-else>正在加载用户数据...</div>
    </el-card>

    <el-card style="margin-top: 2rem;">
      <h3>修改密码</h3>
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue';
import { useAuthStore } from '../store/auth';
import { updateUser, getUserById, updateUserPassword } from '../api/user';
import { ElMessage, type FormInstance } from 'element-plus';
import type { Person } from '../api/types';

const loading = ref(false);
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

const userForm = ref<Partial<Person>>({});

watch(
  () => authStore.user,
  (newUser) => {
    if (newUser?.id) {
      loading.value = true;
      getUserById(newUser.id).then((res) => {
        if (res.success) {
          userForm.value = res.data;
        } else {
          ElMessage.error('获取用户信息失败: ' + res.message);
        }
      }).finally(() => {
        loading.value = false;
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
  loading.value = true;
  try {
    const payload: Person = {
      ...user,
      ...userForm.value,
      username: user.username,
    };

    const response = await updateUser(user.id, payload);
    if (response.success) {
      authStore.setUser(response.data);
      ElMessage.success('个人资料更新成功！');
    } else {
      throw new Error(response.message || '更新个人资料失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '发生错误');
  } finally {
    loading.value = false;
  }
};

const handleUpdatePassword = async () => {
  if (!passwordFormRef.value || !user) return;

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
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
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style scoped>
.user-profile-container {
  padding: 20px;
}
.user-info-header {
  display: flex;
  align-items: center;
  gap: 20px;
}
</style>