<template>
  <div class="mobile-user-profile-page">
    <!-- User Info Header -->
    <div v-if="authStore.user" class="profile-header">
      <div class="avatar">
        {{ authStore.user?.username?.[0]?.toUpperCase() }}
      </div>
      <div class="user-info">
        <h2>{{ authStore.user.username }}</h2>
        <p>ID: {{ authStore.user.id }}</p>
      </div>
    </div>

    <!-- Menu List -->
    <div class="menu-list">
      <el-card class="menu-card">
        <div class="menu-item" @click="showInfo = !showInfo">
          <span>我的信息</span>
          <ChevronRight :size="20" color="#999" />
        </div>
        <el-collapse-transition>
          <div v-show="showInfo" class="info-form">
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
                <el-button
                  type="primary"
                  @click="updateProfile"
                  :loading="isSubmitting"
                  >更新信息</el-button
                >
              </el-form-item>
            </el-form>
          </div>
        </el-collapse-transition>
        <router-link to="/mobile/profile/addresses" class="menu-item">
          <span>地址管理</span>
          <ChevronRight :size="20" color="#999" />
        </router-link>
        <router-link v-if="!isMerchant" to="/mobile/profile/apply-merchant" class="menu-item">
          <span>成为商家</span>
          <ChevronRight :size="20" color="#999" />
        </router-link>
      </el-card>

      <el-card class="menu-card" style="margin-top: 1rem;">
        <div class="menu-item" @click="showRoles = !showRoles">
          <span>切换身份</span>
          <ChevronRight :size="20" color="#999" />
        </div>
        <el-collapse-transition>
          <div v-show="showRoles">
            <router-link to="/mobile/home" class="menu-item sub-item">
              <span>顾客</span>
            </router-link>
            <router-link v-if="isMerchant" to="/mobile/merchant/dashboard" class="menu-item sub-item">
              <span>商家</span>
            </router-link>
            <router-link v-if="isAdmin" to="/mobile/admin/dashboard" class="menu-item sub-item">
              <span>管理</span>
            </router-link>
          </div>
        </el-collapse-transition>
      </el-card>
    </div>

    <!-- Logout Button -->
    <div class="actions">
      <el-button type="danger" @click="logout" size="large">退出登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../../../store/auth';
import { getUserById, updateUser } from '../../../../api/user';
import type { Person } from '../../../../api/types';
import { ElMessage, type FormInstance } from 'element-plus';
import { ChevronRight } from 'lucide-vue-next';

const authStore = useAuthStore();
const router = useRouter();
const profileForm = ref<Partial<Person>>({});
const formRef = ref<FormInstance>();
const isSubmitting = ref(false);
const showInfo = ref(false);
const showRoles = ref(false);

const isMerchant = computed(() => authStore.userRoles.includes('MERCHANT'));
const isAdmin = computed(() => authStore.userRoles.includes('ADMIN'));

watch(() => authStore.user, (newUser) => {
  if (newUser?.id) {
    getUserById(newUser.id).then(res => {
      if (res.success) {
        profileForm.value = res.data;
      }
    });
  }
}, { immediate: true, deep: true });

const updateProfile = async () => {
  if (!formRef.value || !authStore.user?.id) {
    ElMessage.error('用户未登录，无法更新信息。');
    return;
  }
  await formRef.value.validate();
  isSubmitting.value = true;
  try {
    const res = await updateUser(authStore.user.id, profileForm.value as Person);
    if (res.success) {
      ElMessage.success('信息更新成功！');
      authStore.setUser(res.data);
    } else {
      throw new Error(res.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败');
  } finally {
    isSubmitting.value = false;
  }
};

const logout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<style scoped>
.mobile-user-profile-page {
  background-color: #f4f4f5;
  min-height: 100%;
}

.profile-header {
  display: flex;
  align-items: center;
  padding: 2rem 1rem;
  background-color: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background-color: #f97316;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  font-weight: bold;
  margin-right: 1rem;
}

.user-info h2 {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0;
}

.user-info p {
  color: #666;
  margin: 0.25rem 0 0;
}

.menu-list {
  margin: 1rem;
}

.menu-card {
  border-radius: 0.5rem;
}

:deep(.el-card__body) {
  padding: 0;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  cursor: pointer;
  text-decoration: none;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.info-form {
  padding: 1rem;
  border-top: 1px solid #f0f0f0;
}

.actions {
  padding: 0 1rem 1rem;
}

.actions .el-button {
  width: 100%;
}
</style>