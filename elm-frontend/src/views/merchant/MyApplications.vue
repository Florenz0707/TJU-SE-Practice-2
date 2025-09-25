<template>
  <div class="p-8">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的店铺申请</span>
        </div>
      </template>
      <div v-if="applications.length === 0" class="text-gray-500">
        您还没有提交任何申请。
      </div>
      <el-timeline v-else>
        <el-timeline-item
          v-for="app in applications"
          :key="app.id"
          :timestamp="`提交于 ${new Date(app.createTime || '').toLocaleDateString()}`"
          :type="statusType(app.applicationState)"
        >
          <el-card>
            <h4>{{ app.business.businessName }}</h4>
            <p>{{ app.business.businessAddress }}</p>
            <p class="mt-2"><strong>申请说明:</strong> {{ app.applicationExplain }}</p>
            <p class="mt-2"><strong>状态:</strong> <el-tag :type="statusType(app.applicationState)">{{ statusText(app.applicationState) }}</el-tag></p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getMyBusinessApplications, ApplicationState } from '../../api/applicationService';
import type { BusinessApplication } from '../../api/types';

const applications = ref<BusinessApplication[]>([]);

onMounted(async () => {
  try {
    const response = await getMyBusinessApplications();
    applications.value = response;
  } catch (error) {
    console.error('获取申请列表失败:', error);
  }
});

const statusText = (state?: number) => {
  switch (state) {
    case ApplicationState.UNDISPOSED:
      return '待处理';
    case ApplicationState.APPROVED:
      return '已批准';
    case ApplicationState.REJECTED:
      return '已拒绝';
    default:
      return '未知';
  }
};

const statusType = (state?: number) => {
  switch (state) {
    case ApplicationState.UNDISPOSED:
      return 'warning';
    case ApplicationState.APPROVED:
      return 'success';
    case ApplicationState.REJECTED:
      return 'danger';
    default:
      return 'info';
  }
};
</script>