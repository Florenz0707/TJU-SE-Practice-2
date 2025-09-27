<template>
  <div class="desktop-user-profile" v-loading="loading">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="profile-sidebar" shadow="never">
          <div class="user-avatar-section">
            <el-avatar :size="100" :src="userForm.photo" class="user-avatar">
              {{ userInitial }}
            </el-avatar>
            <h3>{{ user?.username }}</h3>
            <p>ID: {{ user?.id }}</p>
          </div>
          <el-menu :default-active="activeMenu" class="profile-menu" @select="handleMenuSelect">
            <el-menu-item index="profile">
              <el-icon><User /></el-icon>
              <span>我的资料</span>
            </el-menu-item>
            <el-menu-item index="password">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
            <el-menu-item v-for="role in availableRoles" :key="role.name" :index="role.path">
              <el-icon><component :is="role.icon" /></el-icon>
              <span>切换到{{ role.title }}</span>
            </el-menu-item>
          </el-menu>
          <div class="logout-section">
            <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card class="profile-content" shadow="never">
          <template #header>
            <div class="card-header">
              <h2>{{ activeMenuTitle }}</h2>
            </div>
          </template>

          <div v-if="activeMenu === 'profile'">
            <el-form :model="userForm" label-width="120px" @submit.prevent="handleUpdateProfile">
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
          </div>

          <div v-if="activeMenu === 'password'">
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="120px" @submit.prevent="handleUpdatePassword">
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
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, reactive, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../store/auth';
import { getUserById, updateUserPassword, updateUser } from '../api/user';
import { ElMessage, type FormInstance } from 'element-plus';
import { User, Lock, UserCog, Home } from 'lucide-vue-next';
import type { Person } from '../api/types';

const loading = ref(false);
const activeMenu = ref('profile');
const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();
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

const allRoles = [
  { name: 'CUSTOMER', title: '顾客', path: '/', icon: Home },
  { name: 'MERCHANT', title: '商家', path: '/merchant/dashboard', icon: UserCog },
  { name: 'ADMIN', title: '管理', path: '/admin/dashboard', icon: UserCog },
];

const getCurrentRole = () => {
    const currentPath = route.path;
    if (currentPath.startsWith('/admin')) return 'ADMIN';
    if (currentPath.startsWith('/merchant')) return 'MERCHANT';
    return 'CUSTOMER';
};

const availableRoles = computed(() => {
  const userRoles = authStore.userRoles;
  const currentRole = getCurrentRole();
  return allRoles.filter(role => userRoles.includes(role.name) && role.name !== currentRole);
});

const userInitial = computed(() => {
  if (userForm.value && userForm.value.username) {
    return userForm.value.username.charAt(0).toUpperCase();
  }
  return '';
});

const activeMenuTitle = computed(() => {
  switch (activeMenu.value) {
    case 'profile':
      return '我的资料';
    case 'password':
      return '修改密码';
    default:
      return '我的资料';
  }
});

const handleMenuSelect = (index: string) => {
  if (index.startsWith('/')) {
    router.push(index);
  } else {
    activeMenu.value = index;
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

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};

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
</script>

<style scoped>
.desktop-user-profile {
  /* The padding and background-color are removed to inherit from the parent layout */
}

.profile-sidebar {
  border-radius: 8px;
}

.user-avatar-section {
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid #e4e7ed;
}

.user-avatar {
  background-color: var(--el-color-primary);
  color: white;
  font-size: 3rem;
  margin-bottom: 10px;
}

.user-avatar-section h3 {
  margin: 10px 0 5px;
  font-size: 1.2rem;
}

.user-avatar-section p {
  margin: 0;
  color: #909399;
  font-size: 0.9rem;
}

.profile-menu {
  border-right: none;
}

.profile-menu .el-menu-item {
  border-radius: 4px;
  margin: 5px 0;
}

.logout-section {
  padding: 20px;
  text-align: center;
  border-top: 1px solid #e4e7ed;
}

.profile-content {
  border-radius: 8px;
}

.card-header {
  font-size: 1.2rem;
  font-weight: 600;
}
</style>