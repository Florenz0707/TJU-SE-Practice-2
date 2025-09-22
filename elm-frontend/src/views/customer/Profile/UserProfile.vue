<template>
  <div>
    <h2>My Profile</h2>
    <el-form v-if="user" :model="userForm" label-width="120px" style="max-width: 600px">
      <el-form-item label="Username">
        <el-input v-model="userForm.username" disabled />
      </el-form-item>
      <el-form-item label="Email">
        <el-input v-model="userForm.email" />
      </el-form-item>
      <el-form-item label="Phone">
        <el-input v-model="userForm.phone" />
      </el-form-item>
      <!-- Add other Person fields as needed, e.g., firstName, lastName -->
      <el-form-item>
        <el-button type="primary" @click="handleUpdateProfile">Save Changes</el-button>
      </el-form-item>
    </el-form>
    <div v-else>Loading user data...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useAuthStore } from '../../../store/auth';
import { updateUser } from '../../../api/user';
import { ElMessage } from 'element-plus';
import type { Person } from '../../../api/types';

const authStore = useAuthStore();
const user = authStore.user;

// Use a local ref for the form to avoid directly mutating the store's state
const userForm = ref<Partial<Person>>({});

// Watch for changes in the store's user data and update the form
watch(
  () => authStore.user,
  (newUser) => {
    if (newUser) {
      userForm.value = { ...newUser };
    }
  },
  { immediate: true }
);

const handleUpdateProfile = async () => {
  if (!user || !user.id) {
    ElMessage.error('User data not available.');
    return;
  }
  try {
    // The API expects a full Person object, so we merge the original user data
    // with the form data to create a complete payload.
    const payload: Person = {
      ...user, // a base User object
      ...userForm.value, // an object with Person fields
      username: user.username, // ensure username is not lost
    };

    const response = await updateUser(user.id, payload);
    if (response.success) {
      // Update the store with the new user info
      authStore.setUser(response.data);
      ElMessage.success('Profile updated successfully!');
    } else {
      throw new Error(response.message || 'Failed to update profile');
    }
  } catch (error: any) {
    ElMessage.error(error.message || 'An error occurred');
  }
};
</script>
