<template>
  <div class="admin-layout">
    <el-container>
      <!-- Header -->
      <el-header class="app-header">
        <div class="header-left">
          <div class="logo">
            <router-link to="/admin/dashboard" class="logo-link">管理后台</router-link>
          </div>
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
