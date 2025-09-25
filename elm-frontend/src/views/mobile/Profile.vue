<template>
  <div class="mobile-profile-page">
    <div v-if="authStore.user" class="profile-info">
      <div class="avatar">
        {{ authStore.user?.username?.[0]?.toUpperCase() }}
      </div>
      <h2>{{ authStore.user.username }}</h2>
      <p>ID: {{ authStore.user.id }}</p>
    </div>

    <div class="actions">
      <el-button type="danger" @click="logout" size="large">退出登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../store/auth';

const authStore = useAuthStore();
const router = useRouter();

const logout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<style scoped>
.mobile-profile-page {
  padding: 2rem 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.profile-info {
  margin-bottom: 3rem;
}
.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-color: #f97316; /* Orange color */
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  font-weight: bold;
  margin: 0 auto 1rem;
}
h2 {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
}
p {
  color: #666;
}
.actions .el-button {
  width: 100%;
}
</style>