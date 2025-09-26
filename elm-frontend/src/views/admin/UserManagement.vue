<template>
  <div class="user-management">
    <h1>用户管理</h1>

    <div class="toolbar">
      <el-input
        v-model="searchQuery"
        placeholder="按用户名搜索"
        clearable
        style="width: 300px; margin-right: 10px;"
      />
      <el-button type="primary" @click="showCreateDialog = true">创建用户</el-button>
    </div>

    <DataTable :columns="columns" :data="filteredUsers">
      <template #actions="{ row }">
        <el-button size="small" @click="handleEdit(row as User)">编辑</el-button>
        <el-button size="small" type="warning" @click="handlePasswordChange(row as User)">修改密码</el-button>
        <el-button size="small" type="danger" @click="handleDelete(row as User)">删除</el-button>
      </template>
    </DataTable>

    <!-- Change Password Dialog -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" @closed="resetPasswordForm">
      <el-form ref="passwordFormRef" :model="passwordData" :rules="passwordRules" label-position="top">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordData.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordData.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="updatePassword">更新密码</el-button>
      </template>
    </el-dialog>

    <!-- Create User Dialog -->
    <el-dialog v-model="showCreateDialog" title="创建新用户" @closed="resetForm">
      <el-form ref="createUserFormRef" :model="newUser" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="newUser.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="newUser.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="authorities">
          <el-select v-model="newUser.authorities" multiple placeholder="选择角色">
            <el-option label="顾客" value="CUSTOMER"></el-option>
            <el-option label="商家" value="MERCHANT"></el-option>
            <el-option label="管理员" value="ADMIN"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateUser">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue';
import { ElButton, ElInput, ElMessage, ElMessageBox, ElDialog, ElForm, ElFormItem, ElSelect, ElOption, type FormInstance, type FormRules } from 'element-plus';
import DataTable from '@/components/DataTable.vue';
import { getAllUsers, deleteUser, addPerson, updateUserPassword } from '@/api/user';
import type { User, Authority, Person } from '@/api/types';

const rawUsers = ref<User[]>([]);
const searchQuery = ref('');
const showCreateDialog = ref(false);
const createUserFormRef = ref<FormInstance>();

const showPasswordDialog = ref(false);
const passwordFormRef = ref<FormInstance>();
const selectedUser = ref<User | null>(null);
const passwordData = reactive({
  newPassword: '',
  confirmPassword: '',
});

const passwordRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (value !== passwordData.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
};

const newUser = reactive<{
  username: string;
  password?: string;
  authorities: string[];
}>({
  username: '',
  password: '',
  authorities: ['CUSTOMER'],
});

const rules = reactive<FormRules>({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  authorities: [{ required: true, message: '请至少选择一个角色', trigger: 'change' }],
});

const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'username', label: '用户名' },
  // Note: Email is on the Person type, not the base User type, so it's removed for now.
  // { prop: 'email', label: 'Email' },
  { prop: 'authorities', label: '角色' },
  { prop: 'createTime', label: '创建时间' },
  { prop: 'actions', label: '操作', slot: 'actions' },
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

const handlePasswordChange = (user: User) => {
  selectedUser.value = user;
  showPasswordDialog.value = true;
};

const updatePassword = async () => {
  if (!passwordFormRef.value || !selectedUser.value) return;

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await updateUserPassword({
          username: selectedUser.value!.username,
          password: passwordData.newPassword,
        });
        if (res.success) {
          ElMessage.success('密码更新成功');
          showPasswordDialog.value = false;
        } else {
          ElMessage.error(res.message || '密码更新失败');
        }
      } catch (error) {
        ElMessage.error('密码更新失败');
      }
    }
  });
};

const resetPasswordForm = () => {
  if (!passwordFormRef.value) return;
  passwordFormRef.value.resetFields();
  passwordData.newPassword = '';
  passwordData.confirmPassword = '';
  selectedUser.value = null;
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

const handleCreateUser = async () => {
  if (!createUserFormRef.value) return;
  await createUserFormRef.value.validate(async (valid) => {
    if (valid) {
      const personData: Person = {
        username: newUser.username,
        password: newUser.password,
        authorities: newUser.authorities.map(name => ({ name })),
      };
      try {
        const res = await addPerson(personData);
        if (res.success) {
          ElMessage.success('用户创建成功');
          showCreateDialog.value = false;
          fetchUsers();
        } else {
          ElMessage.error(res.message || '创建用户失败');
        }
      } catch (error) {
        ElMessage.error('创建用户失败');
      }
    }
  });
};

const resetForm = () => {
  if (!createUserFormRef.value) return;
  createUserFormRef.value.resetFields();
  Object.assign(newUser, {
    username: '',
    password: '',
    authorities: ['CUSTOMER'],
  });
};
</script>

<style scoped>
.user-management {
  padding: 20px;
}
.toolbar {
  display: flex;
  margin-bottom: 20px;
}
</style>
