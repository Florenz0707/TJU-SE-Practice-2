<template>
  <div>
    <h2>地址管理</h2>
    <el-button type="primary" @click="openAddressDialog()" style="margin-bottom: 20px;">
      新增地址
    </el-button>

    <el-table :data="addresses" stripe v-loading="loading">
      <el-table-column prop="contactName" label="联系人" />
      <el-table-column prop="contactTel" label="电话" />
      <el-table-column prop="address" label="地址" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" @click="openAddressDialog(row)">编辑</el-button>
          <el-popconfirm
            v-if="row.id"
            title="确定要删除此地址吗？"
            @confirm="handleDeleteAddress(row.id!)"
          >
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑地址' : '新增地址'" width="500px">
      <el-form :model="addressForm" ref="formRef" label-width="120px">
        <el-form-item label="联系人" prop="contactName" :rules="{ required: true, message: '联系人不能为空' }">
          <el-input v-model="addressForm.contactName" />
        </el-form-item>
        <el-form-item label="电话号码" prop="contactTel" :rules="{ required: true, message: '电话号码不能为空' }">
          <el-input v-model="addressForm.contactTel" />
        </el-form-item>
        <el-form-item label="地址" prop="address" :rules="{ required: true, message: '地址不能为空' }">
          <el-input v-model="addressForm.address" type="textarea" />
        </el-form-item>
         <el-form-item label="性别" prop="contactSex">
           <el-radio-group v-model="addressForm.contactSex">
             <el-radio :label="1">先生</el-radio>
             <el-radio :label="2">女士</el-radio>
           </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useAuthStore } from '../../../store/auth';
import {
  getCurrentUserAddresses,
  addDeliveryAddress,
  updateDeliveryAddress,
  deleteDeliveryAddress,
} from '../../../api/address';
import type { DeliveryAddress } from '../../../api/types';
import { ElMessage, type FormInstance } from 'element-plus';

const authStore = useAuthStore();
const addresses = ref<DeliveryAddress[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const isEditing = ref(false);
const formRef = ref<FormInstance>();
const addressForm = ref<Partial<DeliveryAddress>>({});

const fetchAddresses = async () => {
  loading.value = true;
  try {
    const res = await getCurrentUserAddresses();
    if (res.success) {
      addresses.value = res.data;
    } else {
      throw new Error(res.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取地址失败');
  } finally {
    loading.value = false;
  }
};

const openAddressDialog = (address?: DeliveryAddress) => {
  if (address) {
    isEditing.value = true;
    addressForm.value = { ...address };
  } else {
    isEditing.value = false;
    addressForm.value = { contactName: '', contactTel: '', address: '', contactSex: 1 };
  }
  dialogVisible.value = true;
};

const handleSaveAddress = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  if (!authStore.user) {
    ElMessage.error('用户未登录，无法保存地址。');
    return;
  }
  try {
    const payload: DeliveryAddress = {
      ...addressForm.value as DeliveryAddress,
      customer: authStore.user,
    };
    if (isEditing.value && payload.id) {
      await updateDeliveryAddress(payload.id, payload);
      ElMessage.success('地址更新成功！');
    } else {
      await addDeliveryAddress(payload);
      ElMessage.success('地址添加成功！');
    }
    dialogVisible.value = false;
    fetchAddresses();
  } catch (error: any) {
    ElMessage.error(error.message || '保存地址失败');
  }
};

const handleDeleteAddress = async (id: number) => {
  try {
    await deleteDeliveryAddress(id);
    ElMessage.success('地址删除成功！');
    fetchAddresses();
  } catch (error: any) {
    ElMessage.error(error.message || '删除地址失败');
  }
};

onMounted(fetchAddresses);
</script>
