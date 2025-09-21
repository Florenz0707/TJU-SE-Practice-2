<template>
  <div class="merchant-layout">
    <el-container>
      <!-- Header -->
      <el-header class="app-header">
        <div class="header-left">
          <div class="logo">
            <router-link to="/merchant/dashboard" class="logo-link">商家中心</router-link>
          </div>
          <el-menu
            :default-active="$route.path"
            class="header-menu"
            mode="horizontal"
            :ellipsis="false"
            router
          >
            <el-menu-item index="/merchant/dashboard">仪表盘</el-menu-item>
            <el-menu-item index="/merchant/orders">历史订单</el-menu-item>
            <el-menu-item index="/merchant/menu">菜单管理</el-menu-item>
            <el-menu-item index="/merchant/profile">店铺信息</el-menu-item>
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
        <el-menu-item index="/merchant/dashboard">
          <el-icon><i-ep-data-board /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/merchant/orders">
          <el-icon><i-ep-document /></el-icon>
          <span>历史订单</span>
        </el-menu-item>
        <el-menu-item index="/merchant/menu">
          <el-icon><i-ep-notebook /></el-icon>
          <span>菜单管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/profile">
          <el-icon><i-ep-menu /></el-icon>
          <span>店铺信息</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../store/auth';
import { Menu } from 'lucide-vue-next';

const authStore = useAuthStore();
const router = useRouter();
const drawerVisible = ref(false);

const handleLogout = async () => {
  await authStore.logout();
  router.push({ name: 'Login' });
};
</script>
