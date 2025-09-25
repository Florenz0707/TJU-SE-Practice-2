<template>
  <div class="mobile-address-management-page">
    <el-page-header @back="goBack" content="地址管理"></el-page-header>

    <div class="address-list">
      <el-card v-for="address in addresses" :key="address.id" class="address-card">
        <div class="address-info">
          <p><strong>{{ address.contactName }}</strong> ({{ address.contactTel }})</p>
          <p>{{ address.address }}</p>
        </div>
        <div class="address-actions">
          <el-button size="small" @click="openAddressDialog(address)">编辑</el-button>
          <el-popconfirm title="确定删除吗？" @confirm="handleDeleteAddress(address.id!)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </el-card>
    </div>

    <el-button type="primary" @click="openAddressDialog()" class="add-address-btn">
      新增地址
    </el-button>

    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑地址' : '新增地址'" width="90%">
      <el-form :model="addressForm" ref="formRef" label-position="top">
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
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
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
const route = useRoute();
const router = useRouter();

const fromCheckout = computed(() => route.query.from === 'checkout');

const goBack = () => {
  if (fromCheckout.value) {
    router.push('/mobile/checkout');
  } else {
    router.back();
  }
};

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

<style scoped>
.mobile-address-management-page { padding: 1rem; }
.el-page-header { margin-bottom: 1.5rem; }
.address-list { margin-bottom: 1.5rem; }
.address-card { margin-bottom: 1rem; position: relative; }
.address-info { margin-right: 80px; }
.address-actions { position: absolute; top: 1rem; right: 1rem; }
.add-address-btn { width: 100%; }
</style>