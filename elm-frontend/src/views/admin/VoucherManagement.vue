<template>
  <div class="voucher-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公共优惠券管理</span>
          <el-button type="primary" @click="openVoucherDialog()">添加优惠券</el-button>
        </div>
      </template>

      <el-table :data="vouchers" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="threshold" label="门槛" />
        <el-table-column prop="value" label="价值" />
        <el-table-column prop="totalQuantity" label="总数量" />
        <el-table-column prop="perUserLimit" label="每人限领" />
        <el-table-column prop="claimable" label="可领取">
          <template #default="scope">
            <el-tag :type="scope.row.claimable ? 'success' : 'danger'">
              {{ scope.row.claimable ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="validDays" label="有效期（天）" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="openVoucherDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteVoucher(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑优惠券' : '添加优惠券'">
      <el-form :model="voucherForm" label-width="120px">
        <el-form-item label="门槛">
          <el-input-number v-model="voucherForm.threshold" :min="0" />
        </el-form-item>
        <el-form-item label="价值">
          <el-input-number v-model="voucherForm.value" :min="0" />
        </el-form-item>
         <el-form-item label="总数量 (0为不限)">
          <el-input-number v-model="voucherForm.totalQuantity" :min="0" />
        </el-form-item>
         <el-form-item label="每人限领 (0为不限)">
          <el-input-number v-model="voucherForm.perUserLimit" :min="0" />
        </el-form-item>
        <el-form-item label="可领取">
          <el-switch v-model="voucherForm.claimable" />
        </el-form-item>
        <el-form-item label="有效期（天）">
          <el-input-number v-model="voucherForm.validDays" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveVoucher">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getAllPublicVouchers, addPublicVoucher, updatePublicVoucher, deletePublicVoucher } from '../../api/publicVoucher';
import type { PublicVoucher } from '../../api/types';

const vouchers = ref<PublicVoucher[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const voucherForm = ref<PublicVoucher>({
  id: 0,
  threshold: 0,
  value: 0,
  claimable: true,
  validDays: 30,
  totalQuantity: 0,
  perUserLimit: 0
});

async function fetchVouchers() {
  try {
    const response = await getAllPublicVouchers();
    vouchers.value = response.data;
  } catch (error) {
    ElMessage.error('获取优惠券失败。');
  }
}

function openVoucherDialog(voucher?: PublicVoucher) {
  if (voucher) {
    isEdit.value = true;
    voucherForm.value = { ...voucher };
  } else {
    isEdit.value = false;
    voucherForm.value = {
      id: 0,
      threshold: 0,
      value: 0,
      claimable: true,
      validDays: 30,
      totalQuantity: 0,
      perUserLimit: 0
    };
  }
  dialogVisible.value = true;
}

async function saveVoucher() {
  try {
    if (isEdit.value) {
      await updatePublicVoucher(voucherForm.value);
      ElMessage.success('优惠券更新成功。');
    } else {
      await addPublicVoucher(voucherForm.value);
      ElMessage.success('优惠券添加成功。');
    }
    dialogVisible.value = false;
    fetchVouchers();
  } catch (error) {
    ElMessage.error('保存优惠券失败。');
  }
}

async function deleteVoucher(id: number) {
  try {
    await ElMessageBox.confirm('您确定要删除此优惠券吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await deletePublicVoucher(id.toString());
    ElMessage.success('优惠券删除成功。');
    fetchVouchers();
  } catch (error) {
    ElMessage.info('删除已取消。');
  }
}

onMounted(() => {
  fetchVouchers();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
