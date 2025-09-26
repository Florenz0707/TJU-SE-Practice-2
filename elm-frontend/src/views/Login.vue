<template>
  <div class="auth-container">
    <div class="auth-wrapper">
      <!-- Left Panel: Welcome Message -->
      <div class="welcome-panel">
        <div class="welcome-content">
          <h1 class="welcome-title">欢迎光临！</h1>
          <p class="welcome-text">
            登录以继续您的美食之旅。发现、订购并享受您最喜爱的菜肴。
          </p>
        </div>
      </div>

      <!-- Right Panel: Login Form -->
      <div class="form-panel">
        <div class="form-content">
          <h2 class="form-title">登录您的帐户</h2>
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            @keyup.enter="handleLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" clearable size="large"></el-input>
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                size="large"
              ></el-input>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                @click="handleLogin"
                :loading="loading"
                class="login-button"
                size="large"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <el-link type="info"></el-link>
            <router-link to="/register">
              <el-link type="primary">还没有账户？去注册</el-link>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
  
<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../store/auth';
import { ElMessage, type FormInstance } from 'element-plus';
import { isMobile } from '../utils/device';

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();

const loginFormRef = ref<FormInstance | null>(null);
const loading = ref(false);

const loginForm = reactive({
  username: 'user',
  password: 'user',
});

const loginRules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
});

const getRedirectPath = (roles: string[]): string => {
  const redirect = route.query.redirect as string | undefined;
  if (redirect) {
    return redirect;
  }

  const mobile = isMobile();

  if (roles.includes('ADMIN')) {
    return mobile ? '/mobile/admin' : '/admin';
  }
  if (roles.includes('MERCHANT')) {
    return mobile ? '/mobile/merchant' : '/merchant';
  }

  return mobile ? '/mobile/home' : '/';
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;

  try {
    await loginFormRef.value.validate();
    loading.value = true;
    const userRoles = await authStore.login(loginForm);
    ElMessage.success('登录成功！');

    const redirectPath = getRedirectPath(userRoles);
    router.push(redirectPath);

  } catch (error) {
    console.error('Login process failed:', error);
  } finally {
    loading.value = false;
  }
};
</script>
  
<style lang="scss" scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f8f9fa;
}

.auth-wrapper {
  display: flex;
  width: 100%;
  max-width: 960px;
  margin: 1.5rem;
  border-radius: 16px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  background-color: #ffffff;
}

.welcome-panel {
  flex: 1;
  background-color: #F97316; // Hardcoded orange
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 3rem;
  text-align: left;
}

.welcome-title {
  font-family: "Poppins", sans-serif;
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

.welcome-text {
  font-family: "Inter", sans-serif;
  font-size: 1.125rem;
  line-height: 1.75;
  opacity: 0.9;
}

.form-panel {
  flex: 1;
  padding: 3rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-title {
  font-family: "Poppins", sans-serif;
  font-size: 1.75rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 2rem;
  text-align: center;
}

.login-button {
  width: 100%;
  font-size: 1rem;
  font-weight: 500;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 1.5rem;
  font-size: 0.875rem;
}

@media (max-width: 768px) {
  .auth-wrapper {
    flex-direction: column;
  }
  .welcome-panel {
    display: none;
  }
  .form-panel {
    padding: 2rem;
  }
}
</style>
  