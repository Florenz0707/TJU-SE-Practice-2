<template>
  <div>
    <h2>Order History</h2>
    <el-table :data="orders" stripe v-loading="loading">
      <el-table-column prop="id" label="Order ID" width="100" />
      <el-table-column prop="business.businessName" label="Restaurant" />
      <el-table-column prop="orderDate" label="Date">
        <template #default="{ row }">
          {{ new Date(row.orderDate).toLocaleDateString() }}
        </template>
      </el-table-column>
      <el-table-column prop="orderTotal" label="Total">
        <template #default="{ row }">
          ${{ row.orderTotal.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="orderState" label="Status">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.orderState)">{{
            getStatusText(row.orderState)
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions">
        <template #default="{ row }">
          <el-button size="small" @click="viewOrderDetails(row.id)"
            >View Details</el-button
          >
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUserOrders } from '../../../api/order'
import type { Order } from '../../../api/types'
import { ElMessage } from 'element-plus'

const router = useRouter()
const orders = ref<Order[]>([])
const loading = ref(false)

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getCurrentUserOrders()
    if (res.success) {
      orders.value = res.data
    } else {
      throw new Error(res.message)
    }
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to fetch order history')
  } finally {
    loading.value = false
  }
}

const getStatusType = (state: number) => {
  // Assuming 1: Placed, 2: Preparing, 3: Delivering, 4: Delivered, 5: Cancelled
  if (state === 4) return 'success'
  if (state === 5) return 'danger'
  return 'primary'
}

const getStatusText = (state: number) => {
  const statuses: { [key: number]: string } = {
    1: 'Placed',
    2: 'Preparing',
    3: 'Out for Delivery',
    4: 'Delivered',
    5: 'Cancelled',
  }
  return statuses[state] || 'Unknown'
}

const viewOrderDetails = (id: number) => {
  router.push({ name: 'OrderDetail', params: { id } })
}

onMounted(fetchOrders)
</script>
