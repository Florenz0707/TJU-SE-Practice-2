<template>
  <div>
    <div v-if="loading" class="loading-state">
      <p>正在加载您的申请信息...</p>
    </div>

    <!-- Status View for Pending or Approved Applications -->
    <div v-else-if="latestApplication && latestApplication.applicationState !== ApplicationState.REJECTED">
      <h2>申请状态</h2>
      <el-card>
        <div v-if="latestApplication.applicationState === ApplicationState.UNDISPOSED">
          <el-result
            icon="info"
            title="申请待处理"
            sub-title="您的申请已提交，正在等待管理员审核，请耐心等待。"
          />
        </div>
        <div v-else-if="latestApplication.applicationState === ApplicationState.APPROVED">
          <el-result
            icon="success"
            title="申请已批准"
            sub-title="恭喜！您现在已经是商家了。"
          >
            <template #extra>
              <el-button type="primary" @click="$router.push('/merchant/dashboard')">前往商家中心</el-button>
            </template>
          </el-result>
        </div>
      </el-card>
    </div>

    <!-- Form View for New or Rejected Applications -->
    <div v-else>
      <h2>申请成为商家</h2>
      <el-card>
        <div v-if="latestApplication && latestApplication.applicationState === ApplicationState.REJECTED" class="rejected-notice">
          <el-alert
            title="您之前的申请已被拒绝"
            type="warning"
            description="您可以修改申请说明后重新提交。"
            show-icon
            :closable="false"
          />
        </div>
        <el-form @submit.prevent="submitApplication" ref="formRef" :model="formData" :rules="rules" class="form-container">
          <el-form-item label="申请说明" prop="applicationExplain">
            <el-input
              type="textarea"
              v-model="formData.applicationExplain"
              placeholder="请详细说明您的申请理由，例如您的店铺类型、经验等。"
              :rows="4"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="isSubmitting">提交申请</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue';
import { getMyMerchantApplications, applyToBeMerchant } from '../../../api/application';
import { ApplicationState } from '../../../api/applicationService';
import type { MerchantApplication } from '../../../api/types';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';

const applications = ref<MerchantApplication[]>([]);
const loading = ref(true);
const isSubmitting = ref(false);
const formRef = ref<FormInstance>();

const formData = reactive({
  applicationExplain: '',
});

const rules = reactive<FormRules>({
  applicationExplain: [{ required: true, message: '请输入申请说明', trigger: 'blur' }],
});

const latestApplication = computed(() => {
  if (!applications.value || applications.value.length === 0) {
    return null;
  }
  return [...applications.value].sort((a, b) => new Date(b.createTime!).getTime() - new Date(a.createTime!).getTime())[0];
});

const fetchApplications = async () => {
  loading.value = true;
  try {
    const res = await getMyMerchantApplications();
    if (res.success) {
      applications.value = res.data || [];
    } else {
      ElMessage.error('获取申请历史失败。');
    }
  } catch (error) {
    ElMessage.error('加载申请数据时出错。');
  } finally {
    loading.value = false;
  }
};

onMounted(fetchApplications);

const submitApplication = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;
      try {
        const res = await applyToBeMerchant({ applicationExplain: formData.applicationExplain });
        if (res.success) {
          ElMessage.success('申请成功，请等待管理员审核');
          await fetchApplications(); // Refresh the data to show the new status
        } else {
          ElMessage.error(res.message || '申请失败');
        }
      } catch (error) {
        ElMessage.error('申请失败');
      } finally {
        isSubmitting.value = false;
      }
    }
  });
};
</script>

<style scoped>
.loading-state {
  text-align: center;
  padding: 2rem;
  color: #666;
}
.rejected-notice {
  margin-bottom: 1.5rem;
}
.form-container {
  margin-top: 1rem;
}
</style>