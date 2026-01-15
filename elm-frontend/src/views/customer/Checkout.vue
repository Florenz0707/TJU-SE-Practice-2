<template>
  <div class="checkout-container">
    <el-steps :active="activeStep" finish-status="success" simple style="margin-bottom: 20px;">
      <el-step title="确认订单" />
      <el-step title="选择地址" />
      <el-step title="选择优惠" />
      <el-step title="确认并支付" />
    </el-steps>

    <!-- Step 1: Review Order -->
    <div v-if="activeStep === 0" class="step-content">
      <h3>确认您的订单</h3>
      <el-table :data="cartStore.itemsForCurrentBusiness" style="width: 100%">
        <el-table-column prop="food.foodName" label="商品" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column label="单价" width="120">
          <template #default="{ row }">¥{{ row.food.foodPrice.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="总价" width="120">
          <template #default="{ row }">¥{{ (row.food.foodPrice * row.quantity).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
      <div class="summary-total">
        <p>商品总价: ¥{{ cartStore.cartTotal.toFixed(2) }}</p>
        <p>配送费: ¥{{ cartStore.deliveryPrice.toFixed(2) }}</p>
        <strong>总计: ¥{{ cartStore.finalOrderTotal.toFixed(2) }}</strong>
      </div>
    </div>

    <!-- Step 2: Select Address -->
    <div v-if="activeStep === 1" class="step-content">
      <div class="address-header">
        <h3>选择配送地址</h3>
        <el-button type="primary" @click="openAddressDialog()">添加新地址</el-button>
      </div>
      <el-radio-group v-model="selectedAddressId" class="address-list">
        <el-radio-button v-for="address in addresses" :key="address.id" :label="address.id">
          <div class="address-info">
            <div><strong>{{ address.contactName }}</strong> ({{ address.contactTel }})</div>
            <div>{{ address.address }}</div>
          </div>
          <div class="address-actions">
            <el-button type="text" @click.stop.prevent="openAddressDialog(address)">编辑</el-button>
            <el-button type="text" @click.stop.prevent="deleteAddress(address.id!)">删除</el-button>
          </div>
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- Step 3: Select Discounts (Vouchers & Points) -->
    <div v-if="activeStep === 2" class="step-content">
      <h3>选择优惠方式</h3>
      
      <!-- Voucher Selection -->
      <div class="discount-section">
        <h4>优惠券</h4>
        <el-select v-model="selectedVoucherId" placeholder="选择优惠券" clearable style="width: 100%;">
          <el-option label="不使用优惠券" :value="null" />
          <el-option 
            v-for="voucher in availableVouchers" 
            :key="voucher.id" 
            :label="`满${voucher.threshold}减${voucher.value} (有效期至: ${new Date(voucher.expiryDate).toLocaleDateString()})`"
            :value="voucher.id"
            :disabled="cartStore.finalOrderTotal < voucher.threshold"
          >
            <span>满{{ voucher.threshold }}减{{ voucher.value }}</span>
            <span style="float: right; color: var(--el-text-color-secondary)">
              {{ cartStore.finalOrderTotal < voucher.threshold ? '不满足使用条件' : '可用' }}
            </span>
          </el-option>
        </el-select>
        <div v-if="selectedVoucher" class="discount-info">
          <el-tag type="success">已选择: 满{{ selectedVoucher.threshold }}减{{ selectedVoucher.value }}</el-tag>
        </div>
      </div>

      <!-- Points Selection -->
      <div class="discount-section">
        <h4>积分抵扣</h4>
        <div class="points-info">
          <p>当前可用积分: <strong>{{ pointsAccount?.availablePoints || 0 }}</strong></p>
          <el-checkbox v-model="usePoints">使用积分抵扣</el-checkbox>
        </div>
        <div v-if="usePoints" class="points-input">
          <el-input-number 
            v-model="pointsToUse" 
            :min="0" 
            :max="maxPointsCanUse"
            :step="100"
            placeholder="输入要使用的积分"
            style="width: 100%;"
          />
          <p class="points-tip">100积分 = ¥1，最多可抵扣: {{ maxPointsCanUse }} 积分 (¥{{ (maxPointsCanUse / 100).toFixed(2) }})</p>
          <div v-if="pointsToUse > 0" class="discount-info">
            <el-tag type="success">将抵扣 ¥{{ (pointsToUse / 100).toFixed(2) }}</el-tag>
          </div>
        </div>
      </div>

      <!-- Price Summary -->
      <div class="price-summary">
        <el-divider />
        <div class="price-row">
          <span>商品总价:</span>
          <span>¥{{ cartStore.cartTotal.toFixed(2) }}</span>
        </div>
        <div class="price-row">
          <span>配送费:</span>
          <span>¥{{ cartStore.deliveryPrice.toFixed(2) }}</span>
        </div>
        <div v-if="voucherDiscount > 0" class="price-row discount">
          <span>优惠券:</span>
          <span>-¥{{ voucherDiscount.toFixed(2) }}</span>
        </div>
        <div v-if="pointsDiscount > 0" class="price-row discount">
          <span>积分抵扣:</span>
          <span>-¥{{ pointsDiscount.toFixed(2) }}</span>
        </div>
        <el-divider />
        <div class="price-row total">
          <span>应付金额:</span>
          <span>¥{{ finalPrice.toFixed(2) }}</span>
        </div>
      </div>
    </div>

    <!-- Step 4: Confirm & Pay -->
    <div v-if="activeStep === 3" class="step-content">
      <h3>确认并支付</h3>
      <div class="final-summary">
        <p><strong>配送至:</strong> {{ selectedAddress?.address }}</p>
        <p><strong>联系人:</strong> {{ selectedAddress?.contactName }} ({{ selectedAddress?.contactTel }})</p>
        <el-divider />
        <div class="price-row">
          <span>商品总价:</span>
          <span>¥{{ cartStore.cartTotal.toFixed(2) }}</span>
        </div>
        <div class="price-row">
          <span>配送费:</span>
          <span>¥{{ cartStore.deliveryPrice.toFixed(2) }}</span>
        </div>
        <div v-if="voucherDiscount > 0" class="price-row discount">
          <span>优惠券:</span>
          <span>-¥{{ voucherDiscount.toFixed(2) }}</span>
        </div>
        <div v-if="pointsDiscount > 0" class="price-row discount">
          <span>积分抵扣:</span>
          <span>-¥{{ pointsDiscount.toFixed(2) }}</span>
        </div>
        <el-divider />
        <div class="price-row total">
          <span><strong>应付金额:</strong></span>
          <span class="final-price">¥{{ finalPrice.toFixed(2) }}</span>
        </div>
      </div>
      <el-button type="primary" size="large" @click="placeOrder" :loading="isPlacingOrder" style="width: 100%; margin-top: 20px;">
        提交订单
      </el-button>
    </div>

    <div class="step-actions">
      <el-button @click="prevStep" :disabled="activeStep === 0">上一步</el-button>
      <el-button @click="nextStep" :disabled="isNextDisabled">{{ activeStep === 2 ? '完成' : '下一步' }}</el-button>
    </div>

    <!-- Address Dialog -->
    <el-dialog v-model="isAddressDialogVisible" :title="addressForm.id ? '编辑地址' : '添加新地址'">
      <el-form :model="addressForm" label-width="80px">
        <el-form-item label="联系人">
          <el-input v-model="addressForm.contactName"></el-input>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="addressForm.contactTel"></el-input>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="addressForm.address"></el-input>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="addressForm.contactSex">
            <el-radio :label="1">先生</el-radio>
            <el-radio :label="2">女士</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="isAddressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../../store/cart';
import { useAuthStore } from '../../store/auth';
import { getCurrentUserAddresses, addDeliveryAddress, updateDeliveryAddress, deleteDeliveryAddress } from '../../api/address';
import { addOrder } from '../../api/order';
import { getMyVouchers } from '../../api/privateVoucher';
import { getMyPointsAccount } from '../../api/points';
import type { DeliveryAddress, Order, PrivateVoucher, PointsAccount } from '../../api/types';
import { ElMessage, ElMessageBox } from 'element-plus';

const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();
const activeStep = ref(0);
const addresses = ref<DeliveryAddress[]>([]);
const selectedAddressId = ref<number | null>(null);
const isPlacingOrder = ref(false);

const isAddressDialogVisible = ref(false);
const addressForm = ref<Partial<DeliveryAddress>>({});

// Voucher and Points state
const availableVouchers = ref<PrivateVoucher[]>([]);
const selectedVoucherId = ref<number | null>(null);
const pointsAccount = ref<PointsAccount | null>(null);
const usePoints = ref(false);
const pointsToUse = ref(0);
const tempOrderId = ref<string>('');

const selectedAddress = computed(() => addresses.value.find(a => a.id === selectedAddressId.value));
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
  // Maximum points that can be used: order total - voucher discount, converted to points
  const maxByOrder = Math.floor((cartStore.finalOrderTotal - voucherDiscount.value) * 100);
  return Math.min(available, maxByOrder);
});

const finalPrice = computed(() => {
  let price = cartStore.finalOrderTotal - voucherDiscount.value - pointsDiscount.value;
  return Math.max(price, 0); // Ensure price is not negative
});

const isNextDisabled = computed(() => {
  if (activeStep.value === 1 && !selectedAddressId.value) {
    return true; // Disable "Next" on address step if no address is selected
  }
  if (activeStep.value === 3) {
      return true; // Disable "Next" on the final step
  }
  return false;
});

// Watch points to use to ensure it doesn't exceed maximum
watch(maxPointsCanUse, (newMax) => {
  if (pointsToUse.value > newMax) {
    pointsToUse.value = newMax;
  }
});

// Watch usePoints checkbox
watch(usePoints, (newValue) => {
  if (!newValue) {
    pointsToUse.value = 0;
  }
});


const fetchAddresses = async () => {
  try {
    const res = await getCurrentUserAddresses();
    if (res.success) {
      addresses.value = res.data;
    } else {
      ElMessage.error(res.message || '获取地址失败。');
    }
  } catch (e) {
    ElMessage.error('获取地址失败。');
  }
};

const fetchVouchers = async () => {
  try {
    const res = await getMyVouchers();
    if (res.success) {
      // Filter out used vouchers and expired vouchers
      availableVouchers.value = res.data.filter(v => !v.used && new Date(v.expiryDate) > new Date());
    } else {
      ElMessage.error(res.message || '获取优惠券失败。');
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
    } else {
      ElMessage.error(res.message || '获取积分账户失败。');
    }
  } catch (e) {
    console.error('Failed to fetch points account:', e);
  }
};

const nextStep = () => {
  if (activeStep.value++ > 2) activeStep.value = 0;
};

const prevStep = () => {
  if (activeStep.value-- < 1) activeStep.value = 0;
};

const placeOrder = async () => {
  if (!selectedAddressId.value || !selectedAddress.value) {
    ElMessage.error('请选择配送地址。');
    return;
  }
  if (cartStore.itemsForCurrentBusiness.length === 0) {
    ElMessage.error('您的购物车是空的。');
    return;
  }

  isPlacingOrder.value = true;
  
  try {
    const firstItem = cartStore.itemsForCurrentBusiness[0];
    if (!firstItem || !firstItem.customer || !firstItem.business) {
        ElMessage.error('由于购物车信息不完整，无法下单。');
        isPlacingOrder.value = false;
        return;
    }

    // Create the order with discount information
    // The backend will handle points freezing, deduction, and notifications automatically
    const orderPayload: Order = {
      customer: firstItem.customer,
      business: firstItem.business,
      orderTotal: finalPrice.value, // Use the final price after discounts
      deliveryAddress: selectedAddress.value,
      orderState: 1, // 1: Paid (assuming payment is successful)
    };
    
    const res = await addOrder(orderPayload);
    if (!res.success || !res.data?.id) {
      throw new Error(res.message || '订单创建失败');
    }

    ElMessage.success('下单成功！');
    await cartStore.fetchCart(); // Refetch cart
    router.push({ name: 'OrderHistory' }); // Redirect to order history
    
  } catch (error: any) {
    ElMessage.error(error.message || '下单失败。');
  } finally {
    isPlacingOrder.value = false;
  }
};

onMounted(() => {
  if (cartStore.itemsForCurrentBusiness.length === 0) {
    ElMessage.warning('您的购物车是空的，正在跳转到主页。');
    router.push({ name: 'Home' });
  }
  fetchAddresses();
  fetchVouchers();
  fetchPointsAccount();
});

const openAddressDialog = (address: DeliveryAddress | null = null) => {
  addressForm.value = address ? { ...address } : { contactName: '', contactTel: '', address: '', contactSex: 1 };
  isAddressDialogVisible.value = true;
};

const saveAddress = async () => {
  if (!authStore.user) {
    ElMessage.error('用户未登录，无法保存地址。');
    return;
  }
  try {
    const payload: DeliveryAddress = {
      ...addressForm.value as DeliveryAddress,
      customer: authStore.user, // Make sure customer info is in the payload
    };
    if (addressForm.value.id) {
      await updateDeliveryAddress(addressForm.value.id, payload);
      ElMessage.success('地址更新成功！');
    } else {
      await addDeliveryAddress(payload);
      ElMessage.success('地址添加成功！');
    }
    isAddressDialogVisible.value = false;
    fetchAddresses();
  } catch (e) {
    ElMessage.error('保存地址失败。');
  }
};

const deleteAddress = async (id: number) => {
  ElMessageBox.confirm('确定要删除这个地址吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await deleteDeliveryAddress(id);
      ElMessage.success('地址删除成功！');
      fetchAddresses();
    } catch (e) {
      ElMessage.error('删除地址失败。');
    }
  });
};
</script>

<style scoped>
.checkout-container {
  padding: 20px;
  max-width: 800px;
  margin: auto;
}
.step-content {
  margin-top: 20px;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 4px;
}
.step-actions {
  margin-top: 20px;
  text-align: right;
}
.summary-total {
  text-align: right;
  margin-top: 20px;
  font-size: 1.2rem;
}
.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.address-list .el-radio-button {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  width: 100%;
}
.address-list .el-radio-button :deep(.el-radio-button__inner) {
  width: 100%;
  white-space: normal;
  height: auto;
  padding: 10px;
  text-align: left;
  line-height: 1.5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.address-info {
  flex-grow: 1;
}
.address-actions {
  flex-shrink: 0;
  margin-left: 20px;
}

.discount-section {
  margin-bottom: 30px;
}

.discount-section h4 {
  margin-bottom: 10px;
  color: #303133;
}

.discount-info {
  margin-top: 10px;
}

.points-info {
  margin-bottom: 15px;
}

.points-info p {
  margin-bottom: 10px;
}

.points-input {
  margin-top: 15px;
}

.points-tip {
  margin-top: 8px;
  font-size: 0.9em;
  color: #909399;
}

.price-summary {
  margin-top: 30px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  margin: 10px 0;
  font-size: 1rem;
}

.price-row.discount {
  color: #67c23a;
}

.price-row.total {
  font-size: 1.3rem;
  font-weight: bold;
  color: #303133;
}

.final-summary {
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.final-summary p {
  margin: 10px 0;
}

.final-price {
  font-size: 1.5rem;
  color: #f56c6c;
  font-weight: bold;
}
</style>
