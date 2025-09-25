<template>
  <div class="mobile-admin-layout">
    <header class="top-bar">
      <h1 class="page-title">{{ $route.meta.title }}</h1>
    </header>
    <main class="main-content">
      <router-view />
    </main>
    <nav class="bottom-nav">
      <router-link to="/mobile/admin/dashboard" class="nav-item">
        <LayoutDashboard :size="24" />
        <span>仪表盘</span>
      </router-link>
      <router-link to="/mobile/admin/users" class="nav-item">
        <Users :size="24" />
        <span>用户</span>
      </router-link>
      <router-link to="/mobile/admin/businesses" class="nav-item">
        <Store :size="24" />
        <span>店铺</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { LayoutDashboard, Users, Store } from 'lucide-vue-next';
</script>

<style scoped>
.mobile-admin-layout {
  height: 100vh;
  background-color: #f4f4f5;
}

.top-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  display: flex;
  justify-content: center; /* Center the title */
  align-items: center;
  padding: 0 1rem;
  height: 56px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.page-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #111827;
}

.main-content {
  padding-top: 56px;
  padding-bottom: 60px;
  height: 100%;
  box-sizing: border-box;
  overflow-y: auto;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 10;
  height: 60px;
  background-color: #ffffff;
  display: flex;
  justify-content: space-around;
  align-items: center;
  border-top: 1px solid #e5e7eb;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: #6b7280;
  font-size: 12px;
  transition: color 0.2s;
}

.nav-item.router-link-active {
  color: #409eff; /* Element Plus primary color */
}
</style>