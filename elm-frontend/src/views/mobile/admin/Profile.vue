<template>
  <div class="mobile-admin-profile-page">
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
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../../store/auth';
import { ChevronRight } from 'lucide-vue-next';

const authStore = useAuthStore();
const router = useRouter();
const showRoles = ref(false);

const isMerchant = computed(() => authStore.userRoles.includes('MERCHANT'));
const isAdmin = computed(() => authStore.userRoles.includes('ADMIN'));

const logout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<style scoped>
.mobile-admin-profile-page {
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
  background-color: #409eff;
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

.sub-item {
  padding-left: 2rem;
}

.actions {
  padding: 0 1rem 1rem;
}

.actions .el-button {
  width: 100%;
}
</style>