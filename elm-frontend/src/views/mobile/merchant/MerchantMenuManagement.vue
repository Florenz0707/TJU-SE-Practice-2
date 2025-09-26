<template>
  <div class="mobile-menu-management" v-loading="loading">
    <div class="header">
      <h4>菜单管理</h4>
      <el-button type="primary" size="small" @click="handleOpenEditor()">添加新菜品</el-button>
    </div>

    <div v-if="foods.length" class="food-list">
      <el-card v-for="food in foods" :key="food.id" class="food-card">
        <div class="food-info">
          <div class="food-details">
            <p class="food-name">{{ food.foodName }}</p>
            <p class="food-price">¥{{ food.foodPrice.toFixed(2) }}</p>
            <p class="food-explain">{{ food.foodExplain }}</p>
          </div>
          <div class="food-actions">
            <el-button size="small" @click="handleOpenEditor(food)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(food.id)">删除</el-button>
          </div>
        </div>
      </el-card>
    </div>
    <el-empty v-else description="暂无菜品"></el-empty>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditMode ? '编辑菜品' : '添加新菜品'"
      width="90%"
      @closed="selectedFood = null"
    >
      <FoodEditor v-if="dialogVisible" :food-data="selectedFood" ref="foodEditorRef" />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { getAllFoods, deleteFood, addFood, updateFood, type FoodCreationDto } from '../../../api/food';
import { useBusinessStore } from '../../../store/business';
import { storeToRefs } from 'pinia';
import type { Food, HttpResultListFood } from '../../../api/types';
import { ElMessage, ElMessageBox } from 'element-plus';
import FoodEditor from './MerchantFoodEditor.vue';

const loading = ref(true);
const foods = ref<Food[]>([]);
const dialogVisible = ref(false);
const selectedFood = ref<Food | null>(null);
const foodEditorRef = ref<{ getFormData: () => Promise<Partial<Food> | null> } | null>(null);

const businessStore = useBusinessStore();
const { selectedBusinessId } = storeToRefs(businessStore);

const isEditMode = computed(() => !!selectedFood.value);

const fetchFoods = async () => {
  if (!selectedBusinessId.value) {
    foods.value = [];
    return;
  }
  loading.value = true;
  try {
    const foodsResponse: HttpResultListFood = await getAllFoods({ business: selectedBusinessId.value });
    if (foodsResponse.success) {
      foods.value = foodsResponse.data || [];
    } else {
      foods.value = [];
      ElMessage.warning(foodsResponse.message || '加载菜单信息失败');
    }
  } catch (error) {
    ElMessage.error('加载菜单信息失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchFoods);

watch(selectedBusinessId, fetchFoods);

const handleOpenEditor = (food: Food | null = null) => {
  selectedFood.value = food;
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!foodEditorRef.value) return;

  const formData = await foodEditorRef.value.getFormData();
  if (!formData) {
    ElMessage.error('请检查表单输入');
    return;
  }

  loading.value = true;
  try {
    if (isEditMode.value && selectedFood.value?.id) {
      const payload: Food = { ...selectedFood.value, ...formData };
      await updateFood(selectedFood.value.id, payload);
      ElMessage.success('更新成功！');
    } else {
      if (!selectedBusinessId.value) {
        ElMessage.error('无法确定当前店铺，无法添加菜品');
        loading.value = false;
        return;
      }
      const payload: FoodCreationDto = {
        foodName: formData.foodName || '',
        foodPrice: formData.foodPrice || 0,
        foodExplain: formData.foodExplain,
        foodImg: formData.foodImg,
        business: { id: selectedBusinessId.value },
        remarks: formData.remarks,
      };
      await addFood(payload);
      ElMessage.success('添加成功！');
    }
    dialogVisible.value = false;
    await fetchFoods();
  } catch (error) {
    ElMessage.error('保存失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个菜品吗？此操作无法撤销。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    loading.value = true;
    await deleteFood(id);
    ElMessage.success('菜品删除成功！');
    await fetchFoods();

  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
      console.error(error);
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.mobile-menu-management {
  padding: 1rem;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.food-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.food-card {
  width: 100%;
}
.food-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.food-details {
  flex-grow: 1;
}
.food-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.food-name {
  font-weight: bold;
  font-size: 1rem;
  margin: 0;
}
.food-price {
  color: #f56c6c;
  font-size: 0.9rem;
  margin: 0.25rem 0;
}
.food-explain {
  color: #606266;
  font-size: 0.8rem;
  margin: 0;
}
</style>