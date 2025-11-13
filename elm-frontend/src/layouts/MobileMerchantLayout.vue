<template>
  <div class="mobile-merchant-layout">
    <header class="top-bar">
      <h1 class="page-title">{{ $route.meta.title }}</h1>
      <el-select
        v-if="businesses.length > 0"
        v-model="selectedBusinessId"
        placeholder="选择店铺"
        @change="handleBusinessChange"
        filterable
        size="small"
        class="business-selector"
      >
        <el-option
          v-for="business in businesses"
          :key="business.id"
          :label="business.businessName"
          :value="business.id"
        />
      </el-select>
    </header>
    <main class="main-content">
      <router-view :key="selectedBusinessId || 0" />
    </main>
    <nav class="bottom-nav">
      <router-link to="/mobile/merchant/dashboard" class="nav-item">
        <LayoutDashboard :size="24" />
        <span>仪表盘</span>
      </router-link>
      <router-link to="/mobile/merchant/menu" class="nav-item">
        <BookCopy :size="24" />
        <span>菜单</span>
      </router-link>
      <router-link to="/mobile/merchant/orders" class="nav-item">
        <ScrollText :size="24" />
        <span>历史订单</span>
      </router-link>
      <router-link to="/mobile/merchant/user-profile" class="nav-item">
        <User :size="24" />
        <span>我的</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useBusinessStore } from '../store/business';
import { storeToRefs } from 'pinia';
import { LayoutDashboard, BookCopy, ScrollText, User } from 'lucide-vue-next';

const businessStore = useBusinessStore();
const { businesses, selectedBusinessId } = storeToRefs(businessStore);

onMounted(() => {
  businessStore.fetchBusinesses();
});

const handleBusinessChange = (businessId: number) => {
  businessStore.selectBusiness(businessId);
};
</script>

<style scoped>
.mobile-merchant-layout {
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
  justify-content: space-between; /* Adjusted for title and selector */
  align-items: center;
  padding: 0 1rem;
  height: 56px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.business-selector {
  max-width: 150px; /* Adjust as needed */
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
  color: var(--el-color-primary);
}
</style>