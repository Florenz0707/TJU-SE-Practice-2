<template>
  <div class="auth-container">
    <div class="auth-wrapper">
      <!-- Left Panel: Welcome Message -->
      <div class="welcome-panel">
        <div class="welcome-content">
          <h1 class="welcome-title">创建您的账户</h1>
          <p class="welcome-text">
            加入我们，开启一段全新的美食探索之旅。只需几步，即可享受全城美味。
          </p>
        </div>
      </div>

      <!-- Right Panel: Register Form -->
      <div class="form-panel">
        <div class="form-content">
          <h2 class="form-title">注册新用户</h2>
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            @keyup.enter="handleRegister"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" clearable size="large"></el-input>
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入至少6位数的密码"
                show-password
                size="large"
              ></el-input>
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password
                size="large"
              ></el-input>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                @click="handleRegister"
                :loading="loading"
                class="register-button"
                size="large"
              >
                立即注册
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <router-link to="/login">
              <el-link type="primary">已有账户？直接登录</el-link>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { createUser } from '../api/user';

const router = useRouter();
const registerFormRef = ref<FormInstance | null>(null);
const loading = ref(false);

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
});

const validatePass = (_rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== registerForm.password) {
    callback(new Error("两次输入的密码不一致!"));
  } else {
    callback();
  }
};

const registerRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validatePass, trigger: 'blur' }
  ],
});

const handleRegister = async () => {
  if (!registerFormRef.value) return;

  try {
    await registerFormRef.value.validate();

    loading.value = true;
    const res = await createUser({
      username: registerForm.username,
      password: registerForm.password
    });

    if (res.success) {
      ElMessage.success('注册成功！将跳转到登录页...');
      setTimeout(() => {
        router.push({ name: 'Login' });
      }, 1500);
    } else {
      ElMessage.error(res.message || '注册失败，请稍后重试');
    }
  } catch (error) {
    ElMessage.error('注册失败，请稍后重试');
    console.error('Registration failed:', error);
  } finally {
    loading.value = false;
  }
};
</script>

<style lang="scss" scoped>
// Using the same styles as Login.vue for consistency
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

.register-button {
  width: 100%;
  font-size: 1rem;
  font-weight: 500;
}

.form-footer {
  text-align: center;
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
