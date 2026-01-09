<template>
  <div class="mobile-voucher-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公共优惠券管理</span>
          <el-button type="primary" size="small" @click="openVoucherDialog()">添加</el-button>
        </div>
      </template>

      <div v-for="voucher in vouchers" :key="voucher.id" class="voucher-item">
        <div class="voucher-info">
          <p><strong>ID:</strong> {{ voucher.id }}</p>
          <p><strong>门槛:</strong> ¥{{ voucher.threshold }}</p>
          <p><strong>价值:</strong> ¥{{ voucher.value }}</p>
          <p><strong>可领取:</strong> {{ voucher.claimable ? '是' : '否' }}</p>
          <p><strong>有效期:</strong> {{ voucher.validDays }} 天</p>
        </div>
        <div class="voucher-actions">
          <el-button size="small" @click="openVoucherDialog(voucher)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteVoucher(voucher.id)">删除</el-button>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑优惠券' : '添加优惠券'" width="90%">
      <el-form :model="voucherForm" label-position="top">
        <el-form-item label="门槛">
          <el-input-number v-model="voucherForm.threshold" :min="0" />
        </el-form-item>
        <el-form-item label="价值">
          <el-input-number v-model="voucherForm.value" :min="0" />
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
import { getAllPublicVouchers, addPublicVoucher, updatePublicVoucher, deletePublicVoucher } from '../../../api/publicVoucher';
import type { PublicVoucher } from '../../../api/types';

const vouchers = ref<PublicVoucher[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const voucherForm = ref<PublicVoucher>({
  id: 0,
  threshold: 0,
  value: 0,
  claimable: true,
  validDays: 30,
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
.voucher-item {
  border-bottom: 1px solid #eee;
  padding: 10px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.voucher-info p {
  margin: 5px 0;
}
</style>
