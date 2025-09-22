<template>
  <div class="checkout-container">
    <el-steps :active="activeStep" finish-status="success" simple style="margin-bottom: 20px;">
      <el-step title="Review Order" />
      <el-step title="Select Address" />
      <el-step title="Confirm & Pay" />
    </el-steps>

    <!-- Step 1: Review Order -->
    <div v-if="activeStep === 0" class="step-content">
      <h3>Review Your Order</h3>
      <el-table :data="cartStore.items" style="width: 100%">
        <el-table-column prop="food.foodName" label="Item" />
        <el-table-column prop="quantity" label="Quantity" width="100" />
        <el-table-column label="Price" width="120">
          <template #default="{ row }">${{ row.food.foodPrice.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="Total" width="120">
          <template #default="{ row }">${{ (row.food.foodPrice * row.quantity).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
      <div class="summary-total">
        <strong>Total: ${{ cartStore.cartTotal.toFixed(2) }}</strong>
      </div>
    </div>

    <!-- Step 2: Select Address -->
    <div v-if="activeStep === 1" class="step-content">
      <h3>Select Delivery Address</h3>
      <el-radio-group v-model="selectedAddressId" class="address-list">
        <el-radio-button v-for="address in addresses" :key="address.id" :label="address.id">
          <div><strong>{{ address.contactName }}</strong> ({{ address.contactTel }})</div>
          <div>{{ address.address }}</div>
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- Step 3: Confirm & Pay -->
    <div v-if="activeStep === 2" class="step-content">
      <h3>Confirm Your Order</h3>
      <p><strong>Total Price:</strong> ${{ cartStore.cartTotal.toFixed(2) }}</p>
      <p><strong>Deliver to:</strong> {{ selectedAddress?.address }}</p>
      <el-button type="primary" @click="placeOrder" :loading="isPlacingOrder">Place Order</el-button>
    </div>

    <div class="step-actions">
      <el-button @click="prevStep" :disabled="activeStep === 0">Previous</el-button>
      <el-button @click="nextStep" :disabled="isNextDisabled">{{ activeStep === 2 ? 'Finish' : 'Next' }}</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../../store/cart';
import { getCurrentUserAddresses } from '../../api/address';
import { addOrder } from '../../api/order';
import type { DeliveryAddress, Order } from '../../api/types';
import { ElMessage } from 'element-plus';

const router = useRouter();
const cartStore = useCartStore();
const activeStep = ref(0);
const addresses = ref<DeliveryAddress[]>([]);
const selectedAddressId = ref<number | null>(null);
const isPlacingOrder = ref(false);

const selectedAddress = computed(() => addresses.value.find(a => a.id === selectedAddressId.value));

const isNextDisabled = computed(() => {
  if (activeStep.value === 1 && !selectedAddressId.value) {
    return true; // Disable "Next" on address step if no address is selected
  }
  if (activeStep.value === 2) {
      return true; // Disable "Next" on the final step
  }
  return false;
});


const fetchAddresses = async () => {
  try {
    const res = await getCurrentUserAddresses();
    if (res.success) {
      addresses.value = res.data;
    } else {
      ElMessage.error(res.message || 'Failed to load addresses.');
    }
  } catch (e) {
    ElMessage.error('Failed to load addresses.');
  }
};

const nextStep = () => {
  if (activeStep.value++ > 1) activeStep.value = 0;
};

const prevStep = () => {
  if (activeStep.value-- < 1) activeStep.value = 0;
};

const placeOrder = async () => {
  if (!selectedAddressId.value || !selectedAddress.value) {
    ElMessage.error('Please select a delivery address.');
    return;
  }
  if (cartStore.items.length === 0) {
    ElMessage.error('Your cart is empty.');
    return;
  }

  isPlacingOrder.value = true;
  try {
    const firstItem = cartStore.items[0];
    if (!firstItem || !firstItem.customer || !firstItem.business) {
        ElMessage.error('Cannot place order due to incomplete cart information.');
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
    };
    const res = await addOrder(orderPayload);
    if (res.success) {
      ElMessage.success('Order placed successfully!');
      await cartStore.fetchCart(); // Refetch cart, assuming backend clears it post-order.
      router.push({ name: 'OrderHistory' }); // Redirect to order history
    } else {
      throw new Error(res.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to place order.');
  } finally {
    isPlacingOrder.value = false;
  }
};

onMounted(() => {
  if (cartStore.items.length === 0) {
    ElMessage.warning('Your cart is empty. Redirecting to home.');
    router.push({ name: 'Home' });
  }
  fetchAddresses();
});
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
.address-list .el-radio-button {
  display: block;
  margin-bottom: 10px;
}
.address-list .el-radio-button :deep(.el-radio-button__inner) {
  white-space: normal;
  height: auto;
  padding: 10px;
  text-align: left;
  line-height: 1.5;
}
</style>
