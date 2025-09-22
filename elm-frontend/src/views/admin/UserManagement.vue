<template>
  <div class="user-management">
    <h1>User Management</h1>

    <el-input
      v-model="searchQuery"
      placeholder="Search by username"
      clearable
      style="width: 300px; margin-bottom: 20px;"
    />

    <DataTable :columns="columns" :data="filteredUsers">
      <template #actions="{ row }">
        <el-button size="small" @click="handleEdit(row as User)">Edit</el-button>
        <el-button size="small" type="danger" @click="handleDelete(row as User)">Delete</el-button>
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
  { prop: 'username', label: 'Username' },
  // Note: Email is on the Person type, not the base User type, so it's removed for now.
  // { prop: 'email', label: 'Email' },
  { prop: 'authorities', label: 'Roles' },
  { prop: 'createTime', label: 'Created At' },
];

const fetchUsers = async () => {
  try {
    const res = await getAllUsers();
    if (res.success) {
      rawUsers.value = res.data || [];
    } else {
      ElMessage.error(res.message || 'Failed to fetch users.');
    }
  } catch (error) {
    ElMessage.error('Failed to fetch users.');
    console.error(error);
  }
};

onMounted(fetchUsers);

// Create a computed property for display-friendly data
const displayUsers = computed(() => {
  return rawUsers.value.map(user => ({
    ...user,
    authorities: user.authorities ? user.authorities.map((auth: Authority) => auth.name).join(', ') : 'N/A',
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
  ElMessage.info(`Editing user ID: ${user.id}. Implementation pending.`);
};

const handleDelete = (user: User) => {
  if (!user.id) {
    ElMessage.error('Cannot delete user without an ID.');
    return;
  }
  ElMessageBox.confirm(
    `Are you sure you want to delete user "${user.username}"? This action cannot be undone.`,
    'Warning',
    {
      confirmButtonText: 'OK',
      cancelButtonText: 'Cancel',
      type: 'warning',
    }
  ).then(async () => {
    try {
      if (user.id) { // Redundant check, but good for safety
        await deleteUser(user.id);
        ElMessage.success('User deleted successfully.');
        fetchUsers(); // Refresh the list
      }
    } catch (error) {
      ElMessage.error('Failed to delete user.');
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
