<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <span>管理后台</span>
      </div>
      <el-menu
        :default-active="$route.path"
        class="el-menu-vertical-demo"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#F97316"
        router
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><i-ep-data-analysis /></el-icon>
          <span>平台总览</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><i-ep-user /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/businesses">
          <el-icon><i-ep-shop /></el-icon>
          <span>店铺管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 页头 -->
      <el-header class="header">
        <div class="header-left">
          <!-- Breadcrumb etc. -->
        </div>
        <div class="header-right">
          <span>欢迎, {{ authStore.user?.username }} (Admin)</span>
          <el-dropdown>
            <el-avatar src="https://placehold.co/40x40/f56c6c/ffffff?text=A" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区域 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../store/auth';

const authStore = useAuthStore();
const router = useRouter();

const handleLogout = async () => {
  await authStore.logout();
  router.push({ name: 'Login' });
};
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.sidebar {
  background-color: #304156;
  color: white;
}
.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  font-size: 20px;
  font-weight: bold;
  background-color: #2b3a4a;
}
.el-menu {
  border-right: none;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}
.main-content {
  background-color: #f5f7fa;
  padding: 20px;
}
</style>
