import { defineStore } from 'pinia';
import { ref } from 'vue';
import { getCurrentUserBusinesses } from '../api/business';
import type { Business } from '../api/types';
import { ElMessage } from 'element-plus';

export const useBusinessStore = defineStore('business', () => {
  const businesses = ref<Business[]>([]);
  const selectedBusinessId = ref<number | null>(null);

  const fetchBusinesses = async () => {
    try {
      const res = await getCurrentUserBusinesses();
      if (res.success && res.data) {
        businesses.value = res.data;
        if (businesses.value.length > 0 && !selectedBusinessId.value && businesses.value[0]?.id) {
          selectedBusinessId.value = businesses.value[0].id;
        }
      } else {
        ElMessage.warning(res.message || '当前用户没有关联的店铺');
      }
    } catch (error) {
      ElMessage.error('加载店铺列表失败');
      console.error(error);
    }
  };

  const selectBusiness = (businessId: number) => {
    selectedBusinessId.value = businessId;
  };

  return {
    businesses,
    selectedBusinessId,
    fetchBusinesses,
    selectBusiness,
  };
});