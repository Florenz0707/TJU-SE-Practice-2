<template>
  <el-row :gutter="20" class="profile-layout">
    <el-col :span="6">
      <el-card class="navigation-card">
        <el-menu :default-active="activeRoute" router>
          <el-menu-item index="/profile/details">
            <el-icon>
              <User />
            </el-icon>
            <span>My Profile</span>
          </el-menu-item>
          <el-menu-item index="/profile/addresses">
            <el-icon>
              <Location />
            </el-icon>
            <span>Address Management</span>
          </el-menu-item>
          <el-menu-item index="/profile/orders">
            <el-icon>
              <List />
            </el-icon>
            <span>Order History</span>
          </el-menu-item>
        </el-menu>
      </el-card>
    </el-col>
    <el-col :span="18">
      <el-card class="content-card">
        <router-view />
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { User, Location, List } from '@element-plus/icons-vue'

const route = useRoute()
const activeRoute = computed(() => route.path)
</script>

<style scoped>
.profile-layout {
  padding: 20px;
}

.navigation-card .el-menu {
  border-right: none;
}

.content-card {
  min-height: 500px;
}
</style>
