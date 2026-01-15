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

    <!-- Voucher Section -->
    <div class="discount-section">
      <h3>优惠券</h3>
      <el-select v-model="selectedVoucherId" placeholder="选择优惠券" clearable style="width: 100%;">
        <el-option label="不使用优惠券" :value="null" />
        <el-option 
          v-for="voucher in availableVouchers" 
          :key="voucher.id" 
          :label="`满${voucher.threshold}减${voucher.value}`"
          :value="voucher.id"
          :disabled="cartStore.finalOrderTotal < voucher.threshold"
        >
          <div style="display: flex; justify-content: space-between; width: 100%;">
            <span>满{{ voucher.threshold }}减{{ voucher.value }}</span>
            <span style="color: var(--el-text-color-secondary); font-size: 0.9em;">
              {{ cartStore.finalOrderTotal < voucher.threshold ? '不可用' : '可用' }}
            </span>
          </div>
        </el-option>
      </el-select>
      <div v-if="selectedVoucher" class="selected-info">
        <el-tag type="success" size="small">已选择: 满{{ selectedVoucher.threshold }}减{{ selectedVoucher.value }}</el-tag>
      </div>
    </div>

    <!-- Points Section -->
    <div class="discount-section">
      <h3>积分抵扣</h3>
      <div class="points-info">
        <p>可用积分: <strong>{{ pointsAccount?.availablePoints || 0 }}</strong></p>
        <el-checkbox v-model="usePoints">使用积分抵扣</el-checkbox>
      </div>
      <div v-if="usePoints" class="points-input">
        <el-input-number 
          v-model="pointsToUse" 
          :min="0" 
          :max="maxPointsCanUse"
          :step="100"
          placeholder="输入积分"
          style="width: 100%;"
        />
        <p class="points-tip">100积分 = ¥1，最多: {{ maxPointsCanUse }} 积分</p>
      </div>
    </div>

    <div class="summary-section">
      <h3>订单概要</h3>
      <div v-for="item in cartStore.itemsForCurrentBusiness" :key="item.id" class="summary-item">
        <span>{{ item.food?.foodName }} x {{ item.quantity }}</span>
        <span>¥{{ (item.food?.foodPrice! * item.quantity!).toFixed(2) }}</span>
      </div>
      <div class="summary-item">
        <span>配送费</span>
        <span>¥{{ cartStore.deliveryPrice.toFixed(2) }}</span>
      </div>
      <div v-if="voucherDiscount > 0" class="summary-item discount">
        <span>优惠券</span>
        <span>-¥{{ voucherDiscount.toFixed(2) }}</span>
      </div>
      <div v-if="pointsDiscount > 0" class="summary-item discount">
        <span>积分抵扣</span>
        <span>-¥{{ pointsDiscount.toFixed(2) }}</span>
      </div>
      <div class="summary-total">
        <strong>应付金额</strong>
        <strong class="final-price">¥{{ finalPrice.toFixed(2) }}</strong>
      </div>
    </div>

    <div class="actions">
      <el-button type="primary" size="large" @click="submitOrder" :loading="isSubmitting">提交订单</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../../../store/cart';
import { useAuthStore } from '../../../store/auth';
import { getCurrentUserAddresses } from '../../../api/address';
import { addOrder } from '../../../api/order';
import { getMyVouchers } from '../../../api/privateVoucher';
import { getMyPointsAccount } from '../../../api/points';
import type { DeliveryAddress, PrivateVoucher, PointsAccount } from '../../../api/types';
import { ElMessage } from 'element-plus';
import AddressCard from '../../../components/mobile/AddressCard.vue';

const cartStore = useCartStore();
const authStore = useAuthStore();
const router = useRouter();
const addresses = ref<DeliveryAddress[]>([]);
const selectedAddress = ref<DeliveryAddress | null>(null);
const isSubmitting = ref(false);

// Voucher and Points state
const availableVouchers = ref<PrivateVoucher[]>([]);
const selectedVoucherId = ref<number | null>(null);
const pointsAccount = ref<PointsAccount | null>(null);
const usePoints = ref(false);
const pointsToUse = ref(0);
const tempOrderId = ref<string>('');

const selectedVoucher = computed(() => availableVouchers.value.find(v => v.id === selectedVoucherId.value));

// Calculate discounts
const voucherDiscount = computed(() => {
  if (!selectedVoucher.value) return 0;
  if (cartStore.finalOrderTotal < selectedVoucher.value.threshold) return 0;
  return selectedVoucher.value.value;
});

const pointsDiscount = computed(() => {
  if (!usePoints.value || pointsToUse.value <= 0) return 0;
  return pointsToUse.value / 100; // 100 points = 1 yuan
});

const maxPointsCanUse = computed(() => {
  const available = pointsAccount.value?.availablePoints || 0;
  const maxByOrder = Math.floor((cartStore.finalOrderTotal - voucherDiscount.value) * 100);
  return Math.min(available, maxByOrder);
});

const finalPrice = computed(() => {
  let price = cartStore.finalOrderTotal - voucherDiscount.value - pointsDiscount.value;
  return Math.max(price, 0);
});

// Watch points to ensure it doesn't exceed maximum
watch(maxPointsCanUse, (newMax) => {
  if (pointsToUse.value > newMax) {
    pointsToUse.value = newMax;
  }
});

watch(usePoints, (newValue) => {
  if (!newValue) {
    pointsToUse.value = 0;
  }
});

const fetchVouchers = async () => {
  try {
    const res = await getMyVouchers();
    if (res.success) {
      availableVouchers.value = res.data.filter(v => !v.used && new Date(v.expiryDate) > new Date());
    }
  } catch (e) {
    console.error('Failed to fetch vouchers:', e);
  }
};

const fetchPointsAccount = async () => {
  try {
    const res = await getMyPointsAccount();
    if (res.success) {
      pointsAccount.value = res.data;
    }
  } catch (e) {
    console.error('Failed to fetch points account:', e);
  }
};

onMounted(async () => {
  const res = await getCurrentUserAddresses();
  if (res.success && res.data.length > 0) {
    addresses.value = res.data;
    selectedAddress.value = res.data[0] ?? null;
  }
  fetchVouchers();
  fetchPointsAccount();
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

  if (cartStore.cartTotal < cartStore.startPrice) {
    ElMessage.error(`订单金额未达到起送价 ¥${cartStore.startPrice.toFixed(2)}`);
    return;
  }

  isSubmitting.value = true;
  
  try {
    const currentCartItems = cartStore.itemsForCurrentBusiness;
    if (currentCartItems.length === 0) {
      ElMessage.error('购物车为空，无法下单。');
      return;
    }

    const business = currentCartItems[0]?.business;
    if (!business) {
      ElMessage.error('无法确定商家信息，无法下单');
      return;
    }

    // Create order with discount information
    // The backend will handle points freezing, deduction, and notifications automatically
    const res = await addOrder({
      customer: authStore.user,
      deliveryAddress: selectedAddress.value,
      business: business,
      orderTotal: finalPrice.value,
      orderState: 1, // 1: Paid
    });

    if (!res.success || !res.data?.id) {
      throw new Error(res.message || '创建订单失败');
    }

    ElMessage.success('下单成功！');
    cartStore.fetchCart();
    router.push('/mobile/orders');
    
  } catch (error: any) {
    ElMessage.error(error.message);
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
.mobile-checkout-page { 
  padding: 1rem; 
  padding-bottom: 5rem;
}
h2 { font-size: 1.5rem; margin-bottom: 1.5rem; }
h3 { font-size: 1.2rem; margin-bottom: 1rem; border-bottom: 1px solid #eee; padding-bottom: 0.5rem; }

.address-section,
.discount-section,
.summary-section {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

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

.discount-section .selected-info {
  margin-top: 0.5rem;
}

.points-info {
  margin-bottom: 1rem;
}

.points-info p {
  margin-bottom: 0.5rem;
}

.points-input {
  margin-top: 1rem;
}

.points-tip {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: #909399;
}

.summary-item { 
  display: flex; 
  justify-content: space-between; 
  margin-bottom: 0.5rem; 
}

.summary-item.discount {
  color: #67c23a;
}

.summary-total { 
  display: flex; 
  justify-content: space-between; 
  font-size: 1.2rem; 
  margin-top: 1rem; 
  padding-top: 1rem; 
  border-top: 1px solid #ddd; 
}

.final-price {
  color: #f56c6c;
  font-weight: bold;
}

.actions { 
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 1rem;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
}
.actions .el-button { width: 100%; }
</style>
