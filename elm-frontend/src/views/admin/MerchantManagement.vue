<template>
  <div class="merchant-application-management">
    <h1>商家资格申请管理</h1>

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="8">
        <el-input
          v-model="searchQuery"
          placeholder="按申请人用户名搜索"
          clearable
        />
      </el-col>
      <el-col :span="8">
        <el-select v-model="statusFilter" placeholder="按状态筛选" clearable>
          <el-option label="待处理" :value="ApplicationState.UNDISPOSED"></el-option>
          <el-option label="已批准" :value="ApplicationState.APPROVED"></el-option>
          <el-option label="已拒绝" :value="ApplicationState.REJECTED"></el-option>
        </el-select>
      </el-col>
    </el-row>

    <DataTable :columns="columns" :data="filteredApplications">
      <template #applicationState="{ row }">
        <el-tag :type="statusType(row.applicationState)">
          {{ statusText(row.applicationState) }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <div v-if="row.applicationState === ApplicationState.UNDISPOSED">
          <el-button size="small" type="success" @click="handleUpdate(row, ApplicationState.APPROVED)">批准</el-button>
          <el-button size="small" type="danger" @click="handleUpdate(row, ApplicationState.REJECTED)">拒绝</el-button>
        </div>
        <span v-else>已处理</span>
      </template>
    </DataTable>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElButton, ElInput, ElSelect, ElOption, ElRow, ElCol, ElMessage, ElTag } from 'element-plus';
import DataTable from '@/components/DataTable.vue';
import { getMerchantApplications, approveMerchantApplication } from '@/api/application';
import { ApplicationState } from '@/api/applicationService';
import type { MerchantApplication } from '@/api/types';

const applications = ref<MerchantApplication[]>([]);
const searchQuery = ref('');
const statusFilter = ref<number | null>(null);

const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'applicant.username', label: '申请人' },
  { prop: 'applicationExplain', label: '申请说明' },
  { prop: 'applicationState', label: '状态', slot: 'applicationState' },
  { prop: 'createTime', label: '申请时间', formatter: (row: any) => new Date(row.createTime).toLocaleString() },
  { prop: 'actions', label: '操作', slot: 'actions' },
];

const statusText = (state: number): string => {
  const map: { [key: number]: string } = {
    [ApplicationState.UNDISPOSED]: '待处理',
    [ApplicationState.APPROVED]: '已批准',
    [ApplicationState.REJECTED]: '已拒绝',
  };
  return map[state] ?? '未知';
};

const statusType = (state: number): "warning" | "success" | "danger" | "info" => {
  const map: { [key: number]: "warning" | "success" | "danger" | "info" } = {
    [ApplicationState.UNDISPOSED]: 'warning',
    [ApplicationState.APPROVED]: 'success',
    [ApplicationState.REJECTED]: 'danger',
  };
  return map[state] ?? 'info';
};

const fetchApplications = async () => {
  try {
    const res = await getMerchantApplications();
    if (res.success) {
      applications.value = (res.data || []).sort((a, b) => new Date(b.createTime!).getTime() - new Date(a.createTime!).getTime());
    } else {
      ElMessage.error(res.message || '获取申请列表失败。');
    }
  } catch (error) {
    ElMessage.error('获取申请列表失败。');
  }
};

onMounted(fetchApplications);

const filteredApplications = computed(() => {
  return applications.value.filter(app => {
    const searchMatch = !searchQuery.value || app.applicant.username.toLowerCase().includes(searchQuery.value.toLowerCase());
    const statusMatch = statusFilter.value === null || app.applicationState === statusFilter.value;
    return searchMatch && statusMatch;
  });
});

const handleUpdate = async (application: MerchantApplication, newState: number) => {
  try {
    await approveMerchantApplication(application.id, { applicationState: newState });
    ElMessage.success('操作成功。');
    fetchApplications(); // Refresh list
  } catch (error) {
    ElMessage.error('操作失败。');
  }
};
</script>

<style scoped>
.merchant-application-management {
  padding: 20px;
}
</style>