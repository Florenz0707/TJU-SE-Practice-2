<template>
  <div class="vouchers-view">
      <el-tabs type="border-card">
        <el-tab-pane label="我的优惠券">
            <div class="voucher-list">
                <el-empty v-if="myVouchers.length === 0" description="暂无优惠券" />
                 <el-card v-for="v in myVouchers" :key="v.id" class="voucher-card" :class="{ used: v.used }">
                    <div class="voucher-content">
                        <div class="voucher-left">
                            <div class="amount">¥<span>{{ v.value }}</span></div>
                            <div class="condition">满 {{ v.threshold }} 可用</div>
                        </div>
                        <div class="voucher-right">
                             <div class="title">优惠券</div>
                             <div class="date">有效期至: {{ new Date(v.expiryDate).toLocaleDateString() }}</div>
                             <div class="status">
                                 <el-tag v-if="v.used" type="info">已使用</el-tag>
                                 <el-tag v-else type="success">未使用</el-tag>
                             </div>
                        </div>
                    </div>
                 </el-card>
            </div>
        </el-tab-pane>
        <el-tab-pane label="领券中心">
             <div class="voucher-list">
                <el-empty v-if="publicVouchers.length === 0" description="暂无优惠券可领" />
                 <el-card v-for="v in publicVouchers" :key="v.id" class="voucher-card claimable">
                    <div class="voucher-content">
                        <div class="voucher-left">
                            <div class="amount">¥<span>{{ v.value }}</span></div>
                            <div class="condition">满 {{ v.threshold }} 可用</div>
                        </div>
                        <div class="voucher-right">
                             <div class="title">优惠券</div>
                             <div class="date">有效期: {{ v.validDays }} 天</div>
                             <div class="action">
                                 <el-button type="primary" size="small" @click="handleClaim(v.id)">立即领取</el-button>
                             </div>
                        </div>
                    </div>
                 </el-card>
            </div>
        </el-tab-pane>
      </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getMyVouchers, claimVoucher } from '../../../api/privateVoucher';
import { getAvailablePublicVouchers } from '../../../api/publicVoucher';
import type { PrivateVoucher, PublicVoucher } from '../../../api/types';
import { ElMessage } from 'element-plus';

const myVouchers = ref<PrivateVoucher[]>([]);
const publicVouchers = ref<PublicVoucher[]>([]);

async function fetchMyVouchers() {
    try {
        const res = await getMyVouchers();
        myVouchers.value = res.data;
    } catch(e) {
        console.error(e);
    }
}

async function fetchPublicVouchers() {
    try {
        const res = await getAvailablePublicVouchers();
        // Only show vouchers that haven't been claimed by current user
        publicVouchers.value = res.data;
    } catch(e) {
        console.error(e);
    }
}

async function handleClaim(id: number) {
    try {
        await claimVoucher(id);
        ElMessage.success('领取成功');
        await fetchMyVouchers(); // Refresh my list
        await fetchPublicVouchers(); // Refresh available list
    } catch(e) {
        ElMessage.error('领取失败');
    }
}

onMounted(() => {
    fetchMyVouchers();
    fetchPublicVouchers();
});
</script>

<style scoped>
.vouchers-view {
    padding: 20px;
}
.voucher-list {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
}
.voucher-card {
    width: 300px;
}
.voucher-content {
    display: flex;
    justify-content: space-between;
}
.voucher-left {
    background-color: #fef0f0;
    color: #f56c6c;
    padding: 10px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    border-radius: 4px;
    min-width: 80px;
}
.voucher-left .amount span {
    font-size: 24px;
    font-weight: bold;
}
.voucher-left .condition {
    font-size: 12px;
}
.voucher-right {
    flex: 1;
    padding-left: 15px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}
.voucher-right .title {
    font-weight: bold;
}
.voucher-right .date {
    font-size: 12px;
    color: #999;
    margin: 5px 0;
}
</style>
