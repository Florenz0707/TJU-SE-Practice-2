<template>
  <div class="shop-management">
    <h1>店铺管理</h1>

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="8">
        <el-input
          v-model="searchQuery"
          placeholder="按店铺名称搜索"
          clearable
        />
      </el-col>
      <el-col :span="8">
        <el-select v-model="statusFilter" placeholder="按状态筛选" clearable>
          <el-option label="待处理" value="0"></el-option>
          <el-option label="已批准" value="1"></el-option>
          <el-option label="已拒绝" value="2"></el-option>
        </el-select>
      </el-col>
    </el-row>

    <DataTable :columns="columns" :data="filteredApplications">
      <template #status="{ row }">
        <el-tag :type="statusType(row.applicationState)">
          {{ statusText(row.applicationState) }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button-group>
          <el-button
            v-if="row.applicationState === ApplicationState.UNDISPOSED"
            type="success"
            size="small"
            @click="updateApplicationStatus(row.id, ApplicationState.APPROVED)"
          >
            批准
          </el-button>
          <el-button
            v-if="row.applicationState === ApplicationState.UNDISPOSED"
            type="danger"
            size="small"
            @click="updateApplicationStatus(row.id, ApplicationState.REJECTED)"
          >
            拒绝
          </el-button>
          <el-button
            type="primary"
            size="small"
            @click="viewApplicationDetails(row)"
          >
            详情
          </el-button>
        </el-button-group>
      </template>
    </DataTable>

    <el-dialog v-model="detailsVisible" title="申请详情" width="50%">
      <div v-if="selectedApplication">
        <p><strong>店铺名称:</strong> {{ selectedApplication.business.businessName }}</p>
        <p><strong>店铺地址:</strong> {{ selectedApplication.business.businessAddress }}</p>
        <p><strong>店铺简介:</strong> {{ selectedApplication.business.businessExplain }}</p>
        <p><strong>起送价:</strong> {{ selectedApplication.business.startPrice }}</p>
        <p><strong>配送费:</strong> {{ selectedApplication.business.deliveryPrice }}</p>
        <p><strong>申请说明:</strong> {{ selectedApplication.applicationExplain }}</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailsVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElButton, ElInput, ElSelect, ElOption, ElRow, ElCol, ElDialog, ElTag } from 'element-plus';
import { getBusinessApplications, updateBusinessApplication, ApplicationState } from '../../api/applicationService';
import type { BusinessApplication } from '../../api/types';
import DataTable from '@/components/DataTable.vue';

const applications = ref<BusinessApplication[]>([]);
const detailsVisible = ref(false);
const selectedApplication = ref<BusinessApplication | null>(null);
const searchQuery = ref('');
const statusFilter = ref('');

const columns = [
  { prop: 'business.businessName', label: '店铺名称' },
  { prop: 'business.businessOwner.username', label: '申请人' },
  { prop: 'applicationState', label: '状态', slot: 'status' },
  { prop: 'createTime', label: '申请时间', formatter: (row: any) => new Date(row.createTime || '').toLocaleString() },
  { prop: 'actions', label: '操作', slot: 'actions' },
];

const fetchApplications = async () => {
  try {
    const response = await getBusinessApplications();
    applications.value = (response || []).sort((a, b) => {
      if (a.createTime && b.createTime) {
        return new Date(b.createTime).getTime() - new Date(a.createTime).getTime();
      }
      return 0;
    });
  } catch (error) {
    ElMessage.error('获取申请列表失败');
    console.error('获取申请列表失败:', error);
  }
};

onMounted(fetchApplications);

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

const filteredApplications = computed(() => {
  return applications.value.filter(app => {
    const searchMatch = !searchQuery.value || app.business.businessName.toLowerCase().includes(searchQuery.value.toLowerCase());
    const statusMatch = !statusFilter.value || app.applicationState === Number(statusFilter.value);
    return searchMatch && statusMatch;
  });
});

const updateApplicationStatus = async (id: number, state: number) => {
  try {
    await updateBusinessApplication(id, { applicationState: state as ApplicationState });
    ElMessage.success('操作成功');
    fetchApplications();
  } catch (error) {
    ElMessage.error('操作失败');
    console.error('操作失败:', error);
  }
};

const viewApplicationDetails = (app: BusinessApplication) => {
  selectedApplication.value = app;
  detailsVisible.value = true;
};
</script>

<style scoped>
.shop-management {
  padding: 20px;
}
</style>