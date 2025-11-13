<template>
  <div class="mobile-admin-profile-page">
    <!-- User Info Header -->
    <div v-if="authStore.user" class="profile-header">
      <div class="avatar">
        {{ authStore.user?.username?.[0]?.toUpperCase() }}
      </div>
      <div class="user-info">
        <h2>{{ authStore.user.username }}</h2>
        <p>ID: {{ authStore.user.id }}</p>
      </div>
    </div>

    <!-- Menu List -->
    <div class="menu-list">
      <el-card class="menu-card" style="margin-bottom: 1rem;">
        <div class="menu-item" @click="showPassword = !showPassword">
          <span>修改密码</span>
          <ChevronRight :size="20" color="#999" />
        </div>
        <el-collapse-transition>
          <div v-show="showPassword" class="info-form">
            <el-form
              :model="passwordForm"
              ref="passwordFormRef"
              :rules="passwordRules"
              label-position="top"
            >
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  show-password
                />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  @click="handleUpdatePassword"
                  :loading="isSubmitting"
                  >更新密码</el-button
                >
              </el-form-item>
            </el-form>
          </div>
        </el-collapse-transition>
      </el-card>

      <el-card class="menu-card">
        <div class="menu-item" @click="showRoles = !showRoles">
          <span>切换身份</span>
          <ChevronRight :size="20" color="#999" />
        </div>
        <el-collapse-transition>
          <div v-show="showRoles">
            <router-link to="/mobile/home" class="menu-item sub-item">
              <span>顾客</span>
            </router-link>
            <router-link v-if="isMerchant" to="/mobile/merchant/dashboard" class="menu-item sub-item">
              <span>商家</span>
            </router-link>
            <router-link v-if="isAdmin" to="/mobile/admin/dashboard" class="menu-item sub-item">
              <span>管理</span>
            </router-link>
          </div>
        </el-collapse-transition>
      </el-card>
    </div>

    <!-- Logout Button -->
    <div class="actions">
      <el-button type="danger" @click="logout" size="large">退出登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../../store/auth';
import { updateUserPassword } from '../../../api/user';
import { ElMessage, type FormInstance } from 'element-plus';
import { ChevronRight } from 'lucide-vue-next';

const authStore = useAuthStore();
const router = useRouter();
const showRoles = ref(false);
const showPassword = ref(false);
const isSubmitting = ref(false);

const passwordFormRef = ref<FormInstance>();
const passwordForm = reactive({
  newPassword: '',
  confirmPassword: '',
});

const passwordRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
};

const isMerchant = computed(() => authStore.userRoles.includes('MERCHANT'));
const isAdmin = computed(() => authStore.userRoles.includes('ADMIN'));

const handleUpdatePassword = async () => {
  if (!passwordFormRef.value || !authStore.user) return;

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;
      try {
        const res = await updateUserPassword({
          username: authStore.user!.username,
          password: passwordForm.newPassword,
        });
        if (res.success) {
          ElMessage.success('密码更新成功');
          passwordForm.newPassword = '';
          passwordForm.confirmPassword = '';
          passwordFormRef.value?.resetFields();
          showPassword.value = false;
        } else {
          ElMessage.error(res.message || '密码更新失败');
        }
      } catch (error) {
        ElMessage.error('密码更新失败');
      } finally {
        isSubmitting.value = false;
      }
    }
  });
};

const logout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<style scoped>
.info-form {
  padding: 1rem;
  border-top: 1px solid #f0f0f0;
}

.mobile-admin-profile-page {
  background-color: #f4f4f5;
  min-height: 100%;
}

.profile-header {
  display: flex;
  align-items: center;
  padding: 2rem 1rem;
  background-color: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background-color: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  font-weight: bold;
  margin-right: 1rem;
}

.user-info h2 {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0;
}

.user-info p {
  color: #666;
  margin: 0.25rem 0 0;
}

.menu-list {
  margin: 1rem;
}

.menu-card {
  border-radius: 0.5rem;
}

:deep(.el-card__body) {
  padding: 0;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  cursor: pointer;
  text-decoration: none;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.sub-item {
  padding-left: 2rem;
}

.actions {
  padding: 0 1rem 1rem;
}

.actions .el-button {
  width: 100%;
}
</style>