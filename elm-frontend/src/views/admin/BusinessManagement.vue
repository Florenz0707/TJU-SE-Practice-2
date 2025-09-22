<template>
  <div class="business-management">
    <h1>Business Management</h1>

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="8">
        <el-input
          v-model="searchQuery"
          placeholder="Search by business name"
          clearable
        />
      </el-col>
      <el-col :span="8">
        <el-select v-model="statusFilter" placeholder="Filter by status" clearable>
          <el-option label="Pending" value="pending"></el-option>
          <el-option label="Approved" value="approved"></el-option>
          <el-option label="Rejected" value="rejected"></el-option>
        </el-select>
      </el-col>
    </el-row>

    <DataTable :columns="columns" :data="filteredBusinesses">
      <template #actions="{ row }">
        <div v-if="row.status === 'Pending'">
          <el-button size="small" type="success" @click="handleApprove(row as Business)">Approve</el-button>
          <el-button size="small" type="danger" @click="handleReject(row as Business)">Reject</el-button>
        </div>
        <el-button v-else size="small" @click="handleEdit(row as Business)">View/Edit</el-button>
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
  { prop: 'businessName', label: 'Business Name' },
  { prop: 'businessOwner.username', label: 'Owner' },
  { prop: 'businessAddress', label: 'Address' },
  { prop: 'status', label: 'Status' },
  { prop: 'createTime', label: 'Created At' },
];

const getStatus = (business: Business): 'Pending' | 'Approved' | 'Rejected' => {
  // Assuming 'pending' status is marked in remarks. This is a fragile contract.
  if (business.remarks?.includes('pending')) {
    return 'Pending';
  }
  if (business.deleted) {
    return 'Rejected';
  }
  return 'Approved';
};

const fetchBusinesses = async () => {
  try {
    const res = await getBusinesses();
    if (res.success) {
      rawBusinesses.value = res.data || [];
    } else {
      ElMessage.error(res.message || 'Failed to fetch businesses.');
    }
  } catch (error) {
    ElMessage.error('Failed to fetch businesses.');
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
    const statusMatch = !statusFilter.value || business.status.toLowerCase() === statusFilter.value;
    return searchMatch && statusMatch;
  });
});

const handleApprove = async (business: Business) => {
  try {
    const updateData: Business = { ...business, remarks: 'approved', deleted: false };
    await updateBusiness(business.id, updateData);
    ElMessage.success(`Business "${business.businessName}" approved.`);
    fetchBusinesses(); // Refresh list
  } catch (error) {
    ElMessage.error('Failed to approve business.');
  }
};

const handleReject = async (business: Business) => {
   try {
    const updateData: Business = { ...business, deleted: true };
    await updateBusiness(business.id, updateData);
    ElMessage.warning(`Business "${business.businessName}" rejected.`);
    fetchBusinesses(); // Refresh list
  } catch (error) {
    ElMessage.error('Failed to reject business.');
  }
};

const handleEdit = (business: Business) => {
  console.log('Editing business:', business);
  ElMessage.info(`Editing business ID: ${business.id}. Implementation pending.`);
};
</script>

<style scoped>
.business-management {
  padding: 20px;
}
</style>
