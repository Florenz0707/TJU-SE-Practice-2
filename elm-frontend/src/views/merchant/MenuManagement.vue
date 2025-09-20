<template>
  <div class="menu-management-container" v-loading="loading">
    <div class="header">
      <h2>菜单管理</h2>
      <el-button type="primary" @click="handleOpenEditor()">添加新菜品</el-button>
    </div>
    <el-table :data="foods" stripe style="width: 100%">
      <el-table-column prop="foodName" label="菜品名称" />
      <el-table-column prop="foodExplain" label="描述" />
      <el-table-column prop="foodPrice" label="价格">
        <template #default="{ row }">
          <span>¥{{ row.foodPrice.toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="handleOpenEditor(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEditMode ? '编辑菜品' : '添加新菜品'" width="500px" @closed="selectedFood = null">
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
import { ref, onMounted, computed } from 'vue';
import { getAllFoods, deleteFood, addFood, updateFood, type FoodCreationDto } from '../../api/food';
import { getCurrentUserBusinesses } from '../../api/business';
import type { Food, Business, HttpResultListBusiness, HttpResultListFood } from '../../api/types';
import { ElMessage, ElMessageBox } from 'element-plus';
import FoodEditor from './FoodEditor.vue';

const loading = ref(true);
const foods = ref<Food[]>([]);
const business = ref<Business | null>(null);
const dialogVisible = ref(false);
const selectedFood = ref<Food | null>(null);
const foodEditorRef = ref<{ getFormData: () => Promise<Partial<Food> | null> } | null>(null);

const isEditMode = computed(() => !!selectedFood.value);

const fetchBusinessAndFoods = async () => {
  loading.value = true;
  try {
    const businessResponse: HttpResultListBusiness = await getCurrentUserBusinesses();
    if (businessResponse.success && businessResponse.data && businessResponse.data.length > 0) {
      const currentBusiness = businessResponse.data[0];
      if (currentBusiness) {
        business.value = currentBusiness;
        if (currentBusiness.id) {
          const foodsResponse: HttpResultListFood = await getAllFoods({ business: currentBusiness.id });
          if (foodsResponse.success) {
            foods.value = foodsResponse.data || [];
          }
        }
      }
    } else {
      ElMessage.warning('当前用户没有关联的店铺');
    }
  } catch (error) {
    ElMessage.error('加载菜单信息失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchBusinessAndFoods);

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
      // The `updateFood` API expects a full Food object.
      const payload: Food = { ...selectedFood.value, ...formData };
      await updateFood(selectedFood.value.id, payload);
      ElMessage.success('更新成功！');
    } else {
      if (!business.value?.id) {
        ElMessage.error('无法确定当前店铺，无法添加菜品');
        loading.value = false;
        return;
      }
      // The `addFood` API requires a specific DTO.
      const payload: FoodCreationDto = {
        foodName: formData.foodName || '',
        foodPrice: formData.foodPrice || 0,
        foodExplain: formData.foodExplain,
        foodImg: formData.foodImg,
        business: { id: business.value.id },
        remarks: formData.remarks,
      };
      await addFood(payload);
      ElMessage.success('添加成功！');
    }
    dialogVisible.value = false;
    await fetchBusinessAndFoods();
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
    await fetchBusinessAndFoods();

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
.menu-management-container {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
