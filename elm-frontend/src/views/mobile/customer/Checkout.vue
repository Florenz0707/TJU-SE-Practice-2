<template>
  <div class="mobile-checkout-page">
    <h2>确认订单</h2>

    <div class="address-section">
  <div class="address-header">
    <h3>配送地址</h3>
    <el-button type="text" @click="goToAddressManagement">管理地址</el-button>
  </div>
      <div class="address-list">
        <AddressCard
          v-for="address in addresses"
          :key="address.id"
          :address="address"
          :selected="selectedAddress?.id === address.id"
          @select="selectedAddress = address"
        />
      </div>
  <el-button v-if="addresses.length === 0" @click="goToAddressManagement" type="primary" plain>
    去添加地址
  </el-button>
    </div>

    <div class="summary-section">
      <h3>订单概要</h3>
      <div v-for="item in cartStore.items" :key="item.id" class="summary-item">
        <span>{{ item.food?.foodName }} x {{ item.quantity }}</span>
        <span>¥{{ (item.food?.foodPrice! * item.quantity!).toFixed(2) }}</span>
      </div>
      <div class="summary-total">
        <strong>总计</strong>
        <strong>¥{{ cartStore.cartTotal.toFixed(2) }}</strong>
      </div>
    </div>

    <div class="actions">
      <el-button type="primary" size="large" @click="submitOrder" :loading="isSubmitting">提交订单</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../../../store/cart';
import { useAuthStore } from '../../../store/auth';
import { getCurrentUserAddresses } from '../../../api/address';
import { addOrder } from '../../../api/order';
import type { DeliveryAddress } from '../../../api/types';
import { ElMessage } from 'element-plus';
import AddressCard from '../../../components/mobile/AddressCard.vue';

const cartStore = useCartStore();
const authStore = useAuthStore();
const router = useRouter();
const addresses = ref<DeliveryAddress[]>([]);
const selectedAddress = ref<DeliveryAddress | null>(null);
const isSubmitting = ref(false);

onMounted(async () => {
  const res = await getCurrentUserAddresses();
  if (res.success && res.data.length > 0) {
    addresses.value = res.data;
    selectedAddress.value = res.data[0] ?? null; // Default to the first address
  }
});

const goToAddressManagement = () => {
  router.push({ name: 'MobileAddressManagement', query: { from: 'checkout' } });
};

const submitOrder = async () => {
  if (!authStore.user) {
    ElMessage.error('用户未登录，无法下单。');
    return;
  }
  if (!selectedAddress.value) {
    ElMessage.error('请选择一个配送地址');
    return;
  }
  isSubmitting.value = true;
  try {
    const businessId = cartStore.items[0]?.business?.id;
    if (!businessId) {
      ElMessage.error('无法确定商家信息，无法下单');
      return;
    }

    const allFromSameBusiness = cartStore.items.every(item => item.business?.id === businessId);
    if (!allFromSameBusiness) {
      ElMessage.error('购物车中包含多家餐厅的商品，请分别下单。');
      return;
    }

    const business = cartStore.items[0]?.business;
    if (!business) {
      ElMessage.error('无法确定商家信息，无法下单');
      return;
    }

    const res = await addOrder({
      customer: authStore.user,
      deliveryAddress: selectedAddress.value,
      business: business,
      orderTotal: cartStore.cartTotal,
      orderState: 0, // Assuming 0 is a default state for new orders
    });

    if (res.success) {
      ElMessage.success('下单成功！');
      cartStore.fetchCart(); // Refresh cart (should be empty now)
      router.push('/mobile/orders');
    } else {
      throw new Error(res.message || '创建订单失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message);
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
.mobile-checkout-page { padding: 1rem; }
h2 { font-size: 1.5rem; margin-bottom: 1.5rem; }
h3 { font-size: 1.2rem; margin-bottom: 1rem; border-bottom: 1px solid #eee; padding-bottom: 0.5rem; }
.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.address-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.summary-item { display: flex; justify-content: space-between; margin-bottom: 0.5rem; }
.summary-total { display: flex; justify-content: space-between; font-size: 1.2rem; margin-top: 1rem; padding-top: 1rem; border-top: 1px solid #ddd; }
.actions { margin-top: 2rem; }
.actions .el-button { width: 100%; }
</style>