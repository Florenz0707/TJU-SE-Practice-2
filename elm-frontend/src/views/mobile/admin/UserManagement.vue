<template>
  <div class="mobile-user-management">
    <div class="search-bar">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="按用户名搜索"
        class="search-input"
      />
    </div>

    <div v-if="filteredUsers.length" class="user-list">
      <div v-for="user in filteredUsers" :key="user.id" class="user-card">
        <div class="user-info">
          <span class="username">{{ user.username }}</span>
          <span class="roles">{{ user.authorities }}</span>
        </div>
        <div class="user-meta">
          <span>ID: {{ user.id }}</span>
          <span>注册于: {{ user.createTime }}</span>
        </div>
      </div>
    </div>
    <div v-else class="no-results">
      <p>没有找到用户。</p>
    </div>

    <div class="floating-notice">
      <p>用户创建和编辑功能请在桌面端进行操作。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { getAllUsers } from '@/api/user';
import type { User, Authority } from '@/api/types';

const rawUsers = ref<User[]>([]);
const searchQuery = ref('');

const fetchUsers = async () => {
  try {
    const res = await getAllUsers();
    if (res.success) {
      rawUsers.value = res.data || [];
    } else {
      ElMessage.error(res.message || '获取用户列表失败。');
    }
  } catch (error) {
    ElMessage.error('获取用户列表失败。');
    console.error(error);
  }
};

onMounted(fetchUsers);

const displayUsers = computed(() => {
  return rawUsers.value.map(user => ({
    ...user,
    authorities: user.authorities ? user.authorities.map((auth: Authority) => auth.name).join(', ') : '暂无',
    createTime: user.createTime ? new Date(user.createTime).toLocaleDateString() : 'N/A',
  }));
});

const filteredUsers = computed(() => {
  if (!searchQuery.value) {
    return displayUsers.value;
  }
  return displayUsers.value.filter(user =>
    user.username.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});
</script>

<style scoped>
.mobile-user-management {
  padding: 1rem;
}

.search-bar {
  margin-bottom: 1rem;
}

.search-input {
  width: 100%;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  box-sizing: border-box;
}

.user-list {
  display: grid;
  gap: 1rem;
}

.user-card {
  background-color: #ffffff;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.user-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.username {
  font-size: 1.125rem;
  font-weight: 600;
}

.roles {
  font-size: 0.875rem;
  color: #6b7280;
  background-color: #f0f2f5;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.user-meta {
  font-size: 0.875rem;
  color: #909399;
  display: flex;
  justify-content: space-between;
}

.no-results {
  text-align: center;
  padding: 2rem;
  color: #909399;
}

.floating-notice {
  position: fixed;
  bottom: 70px; /* Above the bottom nav */
  left: 1rem;
  right: 1rem;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  text-align: center;
  padding: 0.75rem;
  border-radius: 8px;
  font-size: 0.875rem;
}
</style>