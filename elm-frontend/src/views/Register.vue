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

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="姓" prop="lastName">
                  <el-input v-model="registerForm.lastName" placeholder="您的姓氏 (可选)" clearable size="large"></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="名" prop="firstName">
                  <el-input v-model="registerForm.firstName" placeholder="您的名字 (可选)" clearable size="large"></el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱地址 (可选)" clearable size="large"></el-input>
            </el-form-item>

            <el-form-item label="电话" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="手机号码 (可选)" clearable size="large"></el-input>
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
import { addPerson, updateUserPassword } from '../api/user';
import { useAuthStore } from '../store/auth';

const router = useRouter();
const authStore = useAuthStore();
const registerFormRef = ref<FormInstance | null>(null);
const loading = ref(false);

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
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

  await registerFormRef.value.validate();
  loading.value = true;

  try {
    // Step 1: Create user with addPerson, including optional fields.
    const personData = {
      username: registerForm.username,
      firstName: registerForm.firstName || undefined,
      lastName: registerForm.lastName || undefined,
      email: registerForm.email || undefined,
      phone: registerForm.phone || undefined,
    };

    const createRes = await addPerson(personData);
    if (!createRes.success) {
      throw new Error(createRes.message || '创建用户失败');
    }
    ElMessage.success('用户创建成功，正在为您登录...');

    // Step 2: Log in with the default password to establish the session.
    await authStore.login({
      username: registerForm.username,
      password: 'password', // Use the default password.
    });

    // Immediately navigate to the homepage for a better user experience.
    ElMessage.success('登录成功！正在跳转到主页...');
    router.push('/');

    // Step 3: Silently update the password in the background after a short delay.
    // This ensures the auth token from the recent login is attached to the request.
    setTimeout(async () => {
      try {
        const passwordUpdateRes = await updateUserPassword({
          username: registerForm.username,
          password: registerForm.password,
        });

        if (passwordUpdateRes.success) {
          console.log('Password successfully updated in the background.');
          // To ensure the session token is consistent with the new password,
          // silently re-authenticate.
          await authStore.login({
            username: registerForm.username,
            password: registerForm.password,
          });
          console.log('Token refreshed with new password in the background.');
        } else {
          console.error('Background password update failed:', passwordUpdateRes.message);
        }
      } catch (error) {
        console.error('An error occurred during the background password update:', error);
      }
    }, 2000); // 1-second delay.

  } catch (error: any) {
    ElMessage.error(error.message || '注册流程失败，请稍后重试');
    console.error('Registration process failed:', error);
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
