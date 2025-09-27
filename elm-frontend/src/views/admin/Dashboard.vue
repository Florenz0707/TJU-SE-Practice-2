<template>
  <div class="admin-dashboard">
    <div class="plain-card">
      <div class="card-header">
        <span>欢迎来到管理后台</span>
      </div>
      <p>这里是管理平台的中心位置。</p>
      <p>请使用侧边栏导航来管理用户和店铺。</p>
      <br>
      <p>
        <strong>注意:</strong> 一个完整的平台分析仪表盘正在计划中，但目前尚待后端API支持。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElCard } from 'element-plus';
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}
.card-header {
  font-size: 1.2em;
  font-weight: bold;
}
</style>
