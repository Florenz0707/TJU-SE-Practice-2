<template>
  <div class="admin-layout">
    <el-container>
      <!-- Header -->
      <el-header class="app-header">
        <div class="header-left">
          <div class="logo">
            <router-link to="/admin/dashboard" class="logo-link">管理后台</router-link>
          </div>
          <!-- Role-based Navigation -->
          <el-menu
            v-if="authStore.isLoggedIn && authStore.userRoles.length > 1"
            :default-active="activePath"
            mode="horizontal"
            :ellipsis="false"
            router
            class="header-menu"
          >
            <template v-if="authStore.userRoles.includes('CUSTOMER')">
              <el-menu-item index="/">
                <el-icon><HomeFilled /></el-icon>
                <span>顾客主页</span>
              </el-menu-item>
            </template>
            <template v-if="authStore.userRoles.includes('MERCHANT')">
              <el-menu-item index="/merchant/dashboard">
                <el-icon><Shop /></el-icon>
                <span>商家中心</span>
              </el-menu-item>
            </template>
            <template v-if="authStore.userRoles.includes('ADMIN')">
              <el-menu-item index="/admin/dashboard">
                <el-icon><Setting /></el-icon>
                <span>管理后台</span>
              </el-menu-item>
            </template>
          </el-menu>
          <el-menu
            :default-active="$route.path"
            class="header-menu"
            mode="horizontal"
            :ellipsis="false"
            router
          >
            <el-menu-item index="/admin/dashboard">平台总览</el-menu-item>
            <el-menu-item index="/admin/users">用户管理</el-menu-item>
            <el-menu-item index="/admin/businesses">店铺管理</el-menu-item>
            <el-menu-item index="/admin/applications">申请管理</el-menu-item>
          </el-menu>
        </div>

        <div class="header-right">
           <el-button class="mobile-menu-button" @click="drawerVisible = true" text>
            <el-icon><Menu /></el-icon>
          </el-button>
          <div class="user-info">
            <el-dropdown>
              <el-avatar>{{ authStore.user?.username?.charAt(0).toUpperCase() }}</el-avatar>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <!-- Main Content -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>

    <!-- Mobile Navigation Drawer -->
    <el-drawer v-model="drawerVisible" title="导航" direction="ltr" size="250px" class="mobile-drawer">
      <el-menu
        :default-active="$route.path"
        @select="drawerVisible = false"
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
        <el-menu-item index="/admin/applications">
          <el-icon><i-ep-document-checked /></el-icon>
          <span>申请管理</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../../store/auth';
import { Menu } from 'lucide-vue-next';
import { HomeFilled, Shop, Setting } from '@element-plus/icons-vue';

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();
const drawerVisible = ref(false);

const activePath = computed(() => {
  if (route.path.startsWith('/admin')) {
    return '/admin/dashboard';
  } else if (route.path.startsWith('/merchant')) {
    return '/merchant/dashboard';
  }
  return '/';
});

const handleLogout = async () => {
  await authStore.logout();
  router.push({ name: 'Login' });
};
</script>
