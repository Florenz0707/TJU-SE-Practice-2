<template>
  <div class="business-management">
    <h1>商家管理</h1>

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
          <el-option label="待处理" value="待处理"></el-option>
          <el-option label="已批准" value="已批准"></el-option>
          <el-option label="已拒绝" value="已拒绝"></el-option>
        </el-select>
      </el-col>
    </el-row>

    <DataTable :columns="columns" :data="filteredBusinesses">
      <template #actions="{ row }">
        <div v-if="row.status === '待处理'">
          <el-button size="small" type="success" @click="handleApprove(row as Business)">批准</el-button>
          <el-button size="small" type="danger" @click="handleReject(row as Business)">拒绝</el-button>
        </div>
        <el-button v-else size="small" @click="handleEdit(row as Business)">查看/编辑</el-button>
      </template>
    </DataTable>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElButton, ElInput, ElSelect, ElOption, ElRow, ElCol, ElMessage } from 'element-plus';
import DataTable from '@/components/DataTable.vue';
import { getBusinesses, updateBusiness } from '@/api/business';
import type { Business } from '@/api/types';

const rawBusinesses = ref<Business[]>([]);
const searchQuery = ref('');
const statusFilter = ref('');

const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'businessName', label: '店铺名称' },
  { prop: 'businessOwner.username', label: '所有者' },
  { prop: 'businessAddress', label: '地址' },
  { prop: 'status', label: '状态' },
  { prop: 'createTime', label: '创建时间' },
];

const getStatus = (business: Business): '待处理' | '已批准' | '已拒绝' => {
  // Assuming 'pending' status is marked in remarks. This is a fragile contract.
  if (business.remarks?.includes('pending')) {
    return '待处理';
  }
  if (business.deleted) {
    return '已拒绝';
  }
  return '已批准';
};

const fetchBusinesses = async () => {
  try {
    const res = await getBusinesses();
    if (res.success) {
      rawBusinesses.value = (res.data || []).sort((a, b) => {
        if (a.createTime && b.createTime) {
          return new Date(b.createTime).getTime() - new Date(a.createTime).getTime();
        }
        return 0;
      });
    } else {
      ElMessage.error(res.message || '获取店铺列表失败。');
    }
  } catch (error) {
    ElMessage.error('获取店铺列表失败。');
    console.error(error);
  }
};

onMounted(fetchBusinesses);

const displayBusinesses = computed(() => {
  return rawBusinesses.value.map(b => ({
    ...b,
    status: getStatus(b),
    'businessOwner.username': b.businessOwner?.username || 'N/A',
  }));
});

const filteredBusinesses = computed(() => {
  return displayBusinesses.value.filter(business => {
    const searchMatch = !searchQuery.value || business.businessName.toLowerCase().includes(searchQuery.value.toLowerCase());
    const statusMatch = !statusFilter.value || business.status === statusFilter.value;
    return searchMatch && statusMatch;
  });
});

const handleApprove = async (business: Business) => {
  if (business.id === undefined) {
    ElMessage.error('无法批准没有ID的店铺。');
    return;
  }
  try {
    const updateData: Business = { ...business, remarks: 'approved', deleted: false };
    await updateBusiness(business.id, updateData);
    ElMessage.success(`店铺 "${business.businessName}" 已批准。`);
    fetchBusinesses(); // Refresh list
  } catch (error) {
    ElMessage.error('批准店铺失败。');
  }
};

const handleReject = async (business: Business) => {
  if (business.id === undefined) {
    ElMessage.error('无法拒绝没有ID的店铺。');
    return;
  }
   try {
    const updateData: Business = { ...business, deleted: true };
    await updateBusiness(business.id, updateData);
    ElMessage.warning(`店铺 "${business.businessName}" 已拒绝。`);
    fetchBusinesses(); // Refresh list
  } catch (error) {
    ElMessage.error('拒绝店铺失败。');
  }
};

const handleEdit = (business: Business) => {
  console.log('Editing business:', business);
  ElMessage.info(`正在编辑店铺 ID: ${business.id}。功能待实现。`);
};
</script>

<style scoped>
.business-management {
  padding: 20px;
}
</style>
