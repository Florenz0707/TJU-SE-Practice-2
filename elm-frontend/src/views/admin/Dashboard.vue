<template>
  <div class="admin-dashboard">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Welcome to the Admin Dashboard</span>
        </div>
      </template>
      <p>This is the central place for managing the platform.</p>
      <p>Use the sidebar navigation to manage users and businesses.</p>
      <br />
      <p>
        <strong>Note:</strong> A full platform analytics dashboard is planned
        but currently pending backend API support.
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElCard } from 'element-plus'
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
