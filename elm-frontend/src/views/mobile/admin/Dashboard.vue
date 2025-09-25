<template>
  <div class="mobile-admin-dashboard">
    <div class="welcome-card">
      <h2>欢迎来到管理后台</h2>
      <p>这里是管理平台的中心位置。</p>
      <p>请使用底部导航来管理用户和店铺。</p>
      <p class="notice">
        <strong>注意:</strong> 一个完整的平台分析仪表盘正在计划中，但目前尚待后端API支持。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
// No script needed for this simple component
</script>

<style scoped>
.mobile-admin-dashboard {
  padding: 1rem;
}

.welcome-card {
  background-color: #ffffff;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  text-align: center;
}

h2 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1rem;
}

p {
  font-size: 1rem;
  color: #4b5563;
  margin-bottom: 0.5rem;
}

.notice {
  margin-top: 1.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}
</style>