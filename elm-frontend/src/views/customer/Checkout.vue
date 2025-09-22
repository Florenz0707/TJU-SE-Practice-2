<template>
  <div class="checkout-container">
    <el-steps :active="activeStep" finish-status="success" simple style="margin-bottom: 20px;">
      <el-step title="确认订单" />
      <el-step title="选择地址" />
      <el-step title="确认并支付" />
    </el-steps>

    <!-- Step 1: Review Order -->
    <div v-if="activeStep === 0" class="step-content">
      <h3>确认您的订单</h3>
      <el-table :data="cartStore.items" style="width: 100%">
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
        <strong>总计: ¥{{ cartStore.cartTotal.toFixed(2) }}</strong>
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

    <!-- Step 3: Confirm & Pay -->
    <div v-if="activeStep === 2" class="step-content">
      <h3>确认您的订单</h3>
      <p><strong>总金额:</strong> ¥{{ cartStore.cartTotal.toFixed(2) }}</p>
      <p><strong>配送至:</strong> {{ selectedAddress?.address }}</p>
      <el-button type="primary" @click="placeOrder" :loading="isPlacingOrder">提交订单</el-button>
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
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../../store/cart';
import { useAuthStore } from '../../store/auth';
import { getCurrentUserAddresses, addDeliveryAddress, updateDeliveryAddress, deleteDeliveryAddress } from '../../api/address';
import { addOrder } from '../../api/order';
import type { DeliveryAddress, Order } from '../../api/types';
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

const selectedAddress = computed(() => addresses.value.find(a => a.id === selectedAddressId.value));

const isNextDisabled = computed(() => {
  if (activeStep.value === 1 && !selectedAddressId.value) {
    return true // Disable "Next" on address step if no address is selected
  }
  if (activeStep.value === 2) {
    return true // Disable "Next" on the final step
  }
  return false
})

const fetchAddresses = async () => {
  try {
    const res = await getCurrentUserAddresses()
    if (res.success) {
      addresses.value = res.data
    } else {
      ElMessage.error(res.message || '获取地址失败。');
    }
  } catch (e) {
    ElMessage.error('获取地址失败。');
  }
}

const nextStep = () => {
  if (activeStep.value++ > 1) activeStep.value = 0
}

const prevStep = () => {
  if (activeStep.value-- < 1) activeStep.value = 0
}

const placeOrder = async () => {
  if (!selectedAddressId.value || !selectedAddress.value) {
    ElMessage.error('请选择配送地址。');
    return;
  }
  if (cartStore.items.length === 0) {
    ElMessage.error('您的购物车是空的。');
    return;
  }

  isPlacingOrder.value = true
  try {
    const firstItem = cartStore.items[0]
    if (!firstItem || !firstItem.customer || !firstItem.business) {
        ElMessage.error('由于购物车信息不完整，无法下单。');
        isPlacingOrder.value = false;
        return;
    }
    // Construct the Order object based on the backend's expectation
    const orderPayload: Order = {
      customer: firstItem.customer,
      business: firstItem.business,
      orderTotal: cartStore.cartTotal,
      deliveryAddress: selectedAddress.value,
      orderState: 0, // 0: Placed
    }
    const res = await addOrder(orderPayload)
    if (res.success) {
      ElMessage.success('下单成功！');
      await cartStore.fetchCart(); // Refetch cart, assuming backend clears it post-order.
      router.push({ name: 'OrderHistory' }); // Redirect to order history
    } else {
      throw new Error(res.message)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '下单失败。');
  } finally {
    isPlacingOrder.value = false
  }
}

onMounted(() => {
  if (cartStore.items.length === 0) {
    ElMessage.warning('您的购物车是空的，正在跳转到主页。');
    router.push({ name: 'Home' });
  }
  fetchAddresses();
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
</style>
