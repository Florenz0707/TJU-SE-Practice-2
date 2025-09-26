<template>
  <div>
    <h2>订单历史</h2>
    <el-table :data="orders" stripe v-loading="loading">
      <el-table-column prop="id" label="订单ID" width="100" />
      <el-table-column prop="business.businessName" label="餐厅" />
      <el-table-column prop="orderDate" label="日期">
        <template #default="{ row }">
          {{ new Date(row.orderDate).toLocaleDateString() }}
        </template>
      </el-table-column>
      <el-table-column prop="orderTotal" label="总价">
        <template #default="{ row }">
          ¥{{ row.orderTotal.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="orderState" label="状态">
        <template #default="{ row }">
          <el-tag :type="getOrderStatusInfo(row.orderState as OrderStatus).type">{{ getOrderStatusInfo(row.orderState as OrderStatus).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" @click="viewOrderDetails(row.id)">查看详情</el-button>
          <el-button v-if="row.orderState === OrderStatusEnum.COMPLETE" size="small" type="primary" @click="goToReview(row.id)">评价</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getMyOrdersCustomer } from '../../../api/order';
import type { Order, OrderStatus } from '../../../api/types';
import { getOrderStatusInfo, OrderStatus as OrderStatusEnum } from '../../../api/types';
import { ElMessage } from 'element-plus';

const router = useRouter();
const orders = ref<Order[]>([]);
const loading = ref(false);

const fetchOrders = async () => {
  loading.value = true;
  try {
    const res = await getMyOrdersCustomer();
    if (res.success) {
      orders.value = res.data.sort((a, b) => {
        if (a.createTime && b.createTime) {
          return new Date(b.createTime).getTime() - new Date(a.createTime).getTime();
        }
        return 0;
      });
    } else {
      throw new Error(res.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取订单历史失败');
  } finally {
    loading.value = false;
  }
};

const viewOrderDetails = (id: number) => {
  router.push({ name: 'OrderDetail', params: { id } });
};

const goToReview = (id: number) => {
  router.push({ name: 'SubmitReview', params: { orderId: id } });
};

onMounted(fetchOrders);
</script>
