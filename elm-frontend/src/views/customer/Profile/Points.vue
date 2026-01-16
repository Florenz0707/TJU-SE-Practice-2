<template>
  <div class="points-view">
    <el-card class="points-summary">
      <template #header>
        <div class="card-header">
          <span>我的积分</span>
        </div>
      </template>
      <div v-if="loading" class="loading-container">
         <el-skeleton :rows="3" animated />
      </div>
      <div v-else class="points-info">
        <div class="point-item">
            <div class="label">总积分</div>
            <div class="value total">{{ account?.totalPoints || 0 }}</div>
        </div>
        <div class="point-item">
            <div class="label">可用积分</div>
            <div class="value available">{{ account?.availablePoints || 0 }}</div>
        </div>
        <div class="point-item">
            <div class="label">冻结积分</div>
            <div class="value frozen">{{ account?.frozenPoints || 0 }}</div>
        </div>
      </div>
    </el-card>

    <el-card class="points-records">
        <template #header>
            <div class="card-header">
            <span>积分明细</span>
            </div>
        </template>
         <el-table :data="records" style="width: 100%">
            <el-table-column prop="recordTime" label="时间" width="180">
                 <template #default="scope">
                    {{ new Date(scope.row.recordTime).toLocaleString() }}
                 </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="100">
                <template #default="scope">
                     <el-tag :type="getRecordTypeTag(scope.row.type)">
                        {{ getRecordTypeLabel(scope.row.type) }}
                     </el-tag>
                </template>
            </el-table-column>
             <el-table-column prop="points" label="变动数量">
                 <template #default="scope">
                    <span :class="scope.row.type === PointsRecordType.EARN ? 'plus' : 'minus'">
                        {{ scope.row.type === PointsRecordType.EARN ? '+' : '-' }}{{ scope.row.points }}
                    </span>
                 </template>
            </el-table-column>
            <el-table-column prop="description" label="原因" />
         </el-table>
         <el-pagination
            v-if="total > 0"
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="handlePageChange"
            class="pagination"
         />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getMyPointsAccount, getMyPointsRecords } from '../../../api/points';
import { type PointsAccount, type PointsRecord, PointsRecordType } from '../../../api/types';

const account = ref<PointsAccount | null>(null);
const records = ref<PointsRecord[]>([]);
const loading = ref(true);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

async function fetchData() {
    loading.value = true;
    try {
        const accRes = await getMyPointsAccount();
        account.value = accRes.data;

        // Ensure page index is valid (at least 1 for UI, 0 for backend)
        const pageIndex = Math.max(1, currentPage.value - 1);
        const recRes = await getMyPointsRecords(pageIndex, pageSize.value);
        records.value = recRes.data.records;
        total.value = recRes.data.total;
    } catch (e) {
        // It's possible account doesn't exist yet, usually backend handles it or returns empty
        console.error(e);
    } finally {
        loading.value = false;
    }
}

function handlePageChange(page: number) {
    currentPage.value = page;
    fetchData();
}

function getRecordTypeLabel(type: any) {
    if (type === PointsRecordType.EARN) return '获取';
    if (type === PointsRecordType.CONSUME) return '消费';
    if (type === PointsRecordType.EXPIRE) return '过期';
    if (type === PointsRecordType.FREEZE) return '冻结';
    if (type === PointsRecordType.UNFREEZE) return '解冻';
    return '未知';
}

function getRecordTypeTag(type: any) {
     if (type === PointsRecordType.EARN) return 'success';
    if (type === PointsRecordType.CONSUME) return 'warning';
    if (type === PointsRecordType.EXPIRE) return 'danger';
    return 'info';
}

onMounted(() => {
    fetchData();
});
</script>

<style scoped>
.points-view {
    padding: 20px;
}
.points-summary {
    margin-bottom: 20px;
}
.points-info {
    display: flex;
    justify-content: space-around;
    text-align: center;
}
.point-item .label {
    color: #909399;
    margin-bottom: 8px;
}
.point-item .value {
    font-size: 24px;
    font-weight: bold;
}
.value.total { color: #409EFF; }
.value.available { color: #67C23A; }
.value.frozen { color: #F56C6C; }

.plus { color: #67C23A; font-weight: bold; }
.minus { color: #F56C6C; font-weight: bold; }
.pagination { margin-top: 15px; display: flex; justify-content: flex-end; }
</style>
