<template>
  <div class="user-management">
    <h1>用户管理</h1>

    <el-input
      v-model="searchQuery"
      placeholder="按用户名搜索"
      clearable
      style="width: 300px; margin-bottom: 20px;"
    />

    <DataTable :columns="columns" :data="filteredUsers">
      <template #actions="{ row }">
        <el-button size="small" @click="handleEdit(row as User)">编辑</el-button>
        <el-button size="small" type="danger" @click="handleDelete(row as User)">删除</el-button>
      </template>
    </DataTable>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElButton, ElInput, ElMessage, ElMessageBox } from 'element-plus';
import DataTable from '@/components/DataTable.vue';
import { getAllUsers, deleteUser } from '@/api/user';
import type { User, Authority } from '@/api/types';

const rawUsers = ref<User[]>([]);
const searchQuery = ref('');

const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'username', label: '用户名' },
  // Note: Email is on the Person type, not the base User type, so it's removed for now.
  // { prop: 'email', label: 'Email' },
  { prop: 'authorities', label: '角色' },
  { prop: 'createTime', label: '创建时间' },
];

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

// Create a computed property for display-friendly data
const displayUsers = computed(() => {
  return rawUsers.value.map(user => ({
    ...user,
    authorities: user.authorities ? user.authorities.map((auth: Authority) => auth.name).join(', ') : '暂无',
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

const handleEdit = (user: User) => {
  console.log('Editing user:', user);
  ElMessage.info(`正在编辑用户 ID: ${user.id}。功能待实现。`);
};

const handleDelete = (user: User) => {
  if (!user.id) {
    ElMessage.error('无法删除没有ID的用户。');
    return;
  }
  ElMessageBox.confirm(
    `您确定要删除用户 "${user.username}" 吗？此操作无法撤销。`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      if (user.id) { // Redundant check, but good for safety
        await deleteUser(user.id);
        ElMessage.success('用户删除成功。');
        fetchUsers(); // Refresh the list
      }
    } catch (error) {
      ElMessage.error('删除用户失败。');
      console.error(error);
    }
  }).catch(() => {
    // User cancelled the action
  });
};
</script>

<style scoped>
.user-management {
  padding: 20px;
}
</style>
