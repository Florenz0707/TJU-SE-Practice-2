<template>
  <div class="my-applications-mobile">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的开店申请</span>
          <el-button type="primary" size="small" @click="openApplicationDialog">申请开店</el-button>
        </div>
      </template>

      <div v-if="applications.length === 0" class="empty-state">
        <p>您还没有提交任何开店申请。</p>
      </div>

      <el-timeline v-else>
        <el-timeline-item
          v-for="app in sortedApplications"
          :key="app.id"
          :timestamp="`提交于 ${new Date(app.createTime || '').toLocaleDateString()}`"
          :type="statusType(app.applicationState)"
        >
          <el-card class="application-card">
            <h4>{{ app.business.businessName }}</h4>
            <p class="address">{{ app.business.businessAddress }}</p>
            <p class="mt-2"><strong>状态:</strong> <el-tag :type="statusType(app.applicationState)" size="small">{{ statusText(app.applicationState) }}</el-tag></p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-dialog v-model="dialogVisible" title="申请开店" fullscreen>
      <el-form :model="applicationData" ref="applicationFormRef" label-position="top">
        <el-form-item label="店铺名称" prop="business.businessName" :rules="[{ required: true, message: '请输入店铺名称' }]">
          <el-input v-model="applicationData.business.businessName" />
        </el-form-item>
        <el-form-item label="店铺地址" prop="business.businessAddress" :rules="[{ required: true, message: '请输入店铺地址' }]">
          <el-input v-model="applicationData.business.businessAddress" />
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input v-model="applicationData.business.businessExplain" type="textarea" />
        </el-form-item>
        <el-form-item label="起送价">
          <el-input-number v-model="applicationData.business.startPrice" :min="0" />
        </el-form-item>
        <el-form-item label="配送费">
          <el-input-number v-model="applicationData.business.deliveryPrice" :min="0" />
        </el-form-item>
        <el-form-item label="申请说明" prop="applicationExplain" :rules="[{ required: true, message: '请输入申请说明' }]">
          <el-input v-model="applicationData.applicationExplain" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitApplication">提交</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, nextTick } from 'vue';
import { ElMessage, ElButton, ElCard, ElTimeline, ElTimelineItem, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber } from 'element-plus';
import type { FormInstance } from 'element-plus';
import { getMyBusinessApplications, submitBusinessApplication, ApplicationState } from '@/api/applicationService';
import type { BusinessApplication, Business } from '@/api/types';

const applications = ref<BusinessApplication[]>([]);
const dialogVisible = ref(false);
const applicationFormRef = ref<FormInstance>();

const applicationData = reactive({
  business: {
    businessName: '',
    businessAddress: '',
    businessExplain: '',
    startPrice: 0,
    deliveryPrice: 0,
  } as Business,
  applicationExplain: ''
});

const sortedApplications = computed(() => {
  return [...applications.value].sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
});

const fetchApplications = async () => {
  try {
    applications.value = await getMyBusinessApplications();
  } catch (error) {
    console.error('获取申请列表失败:', error);
    ElMessage.error('获取申请列表失败');
  }
};

onMounted(fetchApplications);

const openApplicationDialog = () => {
  Object.assign(applicationData, {
    business: {
      businessName: '',
      businessAddress: '',
      businessExplain: '',
      startPrice: 0,
      deliveryPrice: 0,
    },
    applicationExplain: ''
  });
  nextTick(() => {
    applicationFormRef.value?.clearValidate();
  });
  dialogVisible.value = true;
};

const submitApplication = async () => {
  if (!applicationFormRef.value) return;

  await applicationFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await submitBusinessApplication(applicationData);
        ElMessage.success('申请已提交');
        dialogVisible.value = false;
        fetchApplications();
      } catch (error) {
        ElMessage.error('申请提交失败');
        console.error('Failed to submit application:', error);
      }
    }
  });
};

const statusText = (state?: number) => {
  switch (state) {
    case ApplicationState.UNDISPOSED: return '待处理';
    case ApplicationState.APPROVED: return '已批准';
    case ApplicationState.REJECTED: return '已拒绝';
    default: return '未知';
  }
};

const statusType = (state?: number) => {
  switch (state) {
    case ApplicationState.UNDISPOSED: return 'warning';
    case ApplicationState.APPROVED: return 'success';
    case ApplicationState.REJECTED: return 'danger';
    default: return 'info';
  }
};
</script>

<style scoped>
.my-applications-mobile {
  padding: 1rem;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.empty-state {
  text-align: center;
  padding: 2rem;
  color: #909399;
}
.application-card {
  margin-top: 1rem;
}
.application-card h4 {
  margin: 0;
  font-size: 1.1rem;
}
.address {
  font-size: 0.9rem;
  color: #606266;
}
.mt-2 {
  margin-top: 0.5rem;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  padding: 1rem;
}
</style>