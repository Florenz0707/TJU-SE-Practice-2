<template>
  <div class="address-card" :class="{ 'is-selected': selected }" @click="$emit('select')">
    <div class="info">
      <p class="name-tel">
        <strong>{{ address.contactName }}</strong>
        <span>{{ address.contactTel }}</span>
      </p>
      <p class="address-text">{{ address.address }}</p>
    </div>
    <div v-if="selected" class="checkmark">
      <CheckCircle2 :size="24" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckCircle2 } from 'lucide-vue-next';
import type { DeliveryAddress } from '../../api/types';

defineProps<{
  address: DeliveryAddress;
  selected: boolean;
}>();

defineEmits(['select']);
</script>

<style scoped>
.address-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.address-card.is-selected {
  border-color: var(--el-color-primary);
}

.info {
  flex-grow: 1;
}

.name-tel {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 0.5rem;
}

.name-tel span {
  font-weight: normal;
  color: #6b7280;
  margin-left: 0.5rem;
}

.address-text {
  font-size: 0.875rem;
  color: #374151;
  margin: 0;
  line-height: 1.5;
}

.checkmark {
  color: var(--el-color-primary);
  flex-shrink: 0;
  margin-left: 1rem;
}
</style>