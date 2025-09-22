<template>
  <div class="order-history-container" v-loading="loading">
    <div class="header">
      <h2>历史订单查询</h2>
      <el-input
        v-model="searchQuery"
        placeholder="搜索订单ID或顾客信息"
        class="search-input"
        @keyup.enter="handleSearch"
        clearable
        @clear="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <el-table :data="filteredOrders" stripe style="width: 100%">
      <el-table-column prop="id" label="订单ID" width="100" />
      <el-table-column prop="orderDate" label="下单时间" width="200">
        <template #default="{ row }">
          <span>{{
            row.orderDate ? new Date(row.orderDate).toLocaleString() : 'N/A'
          }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="customer.username" label="顾客用户名" />
      <el-table-column prop="orderTotal" label="订单总额">
        <template #default="{ row }">
          <span>¥{{ (row.orderTotal ?? 0).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="orderState" label="订单状态">
        <template #default="{ row }">
          <el-tag :type="getOrderStatusType(row.orderState)">
            {{ getOrderStatusText(row.orderState) }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { listOrdersByUserId } from '../../api/order'
import { getCurrentUserBusinesses } from '../../api/business'
import type { Order, Business, HttpResultListBusiness } from '../../api/types'
import { ElMessage } from 'element-plus'

const loading = ref(true)
const allOrders = ref<Order[]>([])
const business = ref<Business | null>(null)
const searchQuery = ref('')

const fetchBusinessAndOrders = async () => {
  loading.value = true
  try {
    const businessResponse: HttpResultListBusiness =
      await getCurrentUserBusinesses()
    if (
      businessResponse.success &&
      businessResponse.data &&
      businessResponse.data.length > 0
    ) {
      const currentBusiness = businessResponse.data[0]
      if (currentBusiness) {
        business.value = currentBusiness
        const ownerId = currentBusiness.businessOwner?.id
        if (ownerId) {
          const ordersResponse = await listOrdersByUserId(ownerId);
          if (ordersResponse.success) {
            allOrders.value = ordersResponse.data || [];
          } else {
            ElMessage.error(ordersResponse.message || '获取订单列表失败');
          }
        } else {
          ElMessage.warning('无法确定店铺所有者，无法加载订单')
        }
      } else {
        ElMessage.warning('Could not retrieve business details.')
      }
    } else {
      ElMessage.warning(businessResponse.message || '当前用户没有关联的店铺')
    }
  } catch (error) {
    ElMessage.error('加载订单历史失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchBusinessAndOrders)

const filteredOrders = computed(() => {
  if (!searchQuery.value) {
    return allOrders.value
  }
  const query = searchQuery.value.toLowerCase()
  return allOrders.value.filter(
    order =>
      (order.id?.toString() ?? '').includes(query) ||
      (order.customer?.username?.toLowerCase() ?? '').includes(query)
  )
})

const handleSearch = () => {
  // The computed property already handles filtering
}

const getOrderStatusText = (status?: number): string => {
  if (status === undefined) return '未知状态'
  const statusMap: { [key: number]: string } = {
    0: '已下单',
    1: '商家已接单',
    2: '准备中',
    3: '待取餐',
    4: '配送中',
    5: '已送达',
    6: '已取消',
  }
  return statusMap[status] || '未知状态'
}

const getOrderStatusType = (status?: number): string => {
  if (status === undefined) return 'info'
  const typeMap: { [key: number]: string } = {
    0: 'info',
    1: 'primary',
    2: 'primary',
    3: 'warning',
    4: 'warning',
    5: 'success',
    6: 'danger',
  }
  return typeMap[status] || 'info'
}
</script>

<style scoped>
.order-history-container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
}
</style>
