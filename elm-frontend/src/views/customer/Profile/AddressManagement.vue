<template>
  <div>
    <h2>Address Management</h2>
    <el-button
      type="primary"
      @click="openAddressDialog()"
      style="margin-bottom: 20px"
    >
      Add New Address
    </el-button>

    <el-table :data="addresses" stripe v-loading="loading">
      <el-table-column prop="contactName" label="Contact Name" />
      <el-table-column prop="contactTel" label="Phone" />
      <el-table-column prop="address" label="Address" />
      <el-table-column label="Actions">
        <template #default="{ row }">
          <el-button size="small" @click="openAddressDialog(row)"
            >Edit</el-button
          >
          <el-popconfirm
            v-if="row.id"
            title="Are you sure you want to delete this address?"
            @confirm="handleDeleteAddress(row.id!)"
          >
            <template #reference>
              <el-button size="small" type="danger">Delete</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? 'Edit Address' : 'Add New Address'"
      width="500px"
    >
      <el-form :model="addressForm" ref="formRef" label-width="120px">
        <el-form-item
          label="Contact Name"
          prop="contactName"
          :rules="{ required: true, message: 'Name is required' }"
        >
          <el-input v-model="addressForm.contactName" />
        </el-form-item>
        <el-form-item
          label="Phone Number"
          prop="contactTel"
          :rules="{ required: true, message: 'Phone is required' }"
        >
          <el-input v-model="addressForm.contactTel" />
        </el-form-item>
        <el-form-item
          label="Address"
          prop="address"
          :rules="{ required: true, message: 'Address is required' }"
        >
          <el-input v-model="addressForm.address" type="textarea" />
        </el-form-item>
        <el-form-item label="Contact Sex" prop="contactSex">
          <el-radio-group v-model="addressForm.contactSex">
            <el-radio :label="1">Male</el-radio>
            <el-radio :label="2">Female</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleSaveAddress">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getCurrentUserAddresses,
  addDeliveryAddress,
  updateDeliveryAddress,
  deleteDeliveryAddress,
} from '../../../api/address'
import type { DeliveryAddress } from '../../../api/types'
import { ElMessage, type FormInstance } from 'element-plus'

const addresses = ref<DeliveryAddress[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()
const addressForm = ref<Partial<DeliveryAddress>>({})

const fetchAddresses = async () => {
  loading.value = true
  try {
    const res = await getCurrentUserAddresses()
    if (res.success) {
      addresses.value = res.data
    } else {
      throw new Error(res.message)
    }
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to fetch addresses')
  } finally {
    loading.value = false
  }
}

const openAddressDialog = (address?: DeliveryAddress) => {
  if (address) {
    isEditing.value = true
    addressForm.value = { ...address }
  } else {
    isEditing.value = false
    addressForm.value = {
      contactName: '',
      contactTel: '',
      address: '',
      contactSex: 1,
    }
  }
  dialogVisible.value = true
}

const handleSaveAddress = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  try {
    const payload = addressForm.value as DeliveryAddress
    if (isEditing.value && payload.id) {
      await updateDeliveryAddress(payload.id, payload)
      ElMessage.success('Address updated!')
    } else {
      await addDeliveryAddress(payload)
      ElMessage.success('Address added!')
    }
    dialogVisible.value = false
    fetchAddresses()
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to save address')
  }
}

const handleDeleteAddress = async (id: number) => {
  try {
    await deleteDeliveryAddress(id)
    ElMessage.success('Address deleted!')
    fetchAddresses()
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to delete address')
  }
}

onMounted(fetchAddresses)
</script>
