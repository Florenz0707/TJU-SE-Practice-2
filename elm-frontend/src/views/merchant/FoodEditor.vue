<template>
  <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
    <el-form-item label="菜品名称" prop="foodName">
      <el-input v-model="form.foodName" />
    </el-form-item>
    <el-form-item label="描述" prop="foodExplain">
      <el-input type="textarea" v-model="form.foodExplain" />
    </el-form-item>
    <el-form-item label="价格" prop="foodPrice">
      <el-input-number v-model="form.foodPrice" :precision="2" :step="0.1" :min="0" />
    </el-form-item>
    <el-form-item label="图片" prop="foodImg">
      <el-upload
        class="avatar-uploader"
        action="#"
        :show-file-list="false"
        :before-upload="handleBeforeUpload"
      >
        <img v-if="form.foodImg" :src="formatBase64Image(form.foodImg)" class="avatar" />
        <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
      </el-upload>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus';
import type { Food } from '../../api/types';
import { Plus } from '@element-plus/icons-vue';
import { formatBase64Image } from '../../utils/image';

interface Props {
  foodData: Food | null;
}

const props = defineProps<Props>();

const formRef = ref<FormInstance>();
const form = ref<Partial<Food>>({});

const handleBeforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('Avatar picture must be JPG or PNG format!');
    return false;
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('Avatar picture size can not exceed 2MB!');
    return false;
  }

  const reader = new FileReader();
  reader.onload = (e) => {
    form.value.foodImg = e.target?.result as string;
  };
  reader.readAsDataURL(rawFile);

  return false;
};

// Watch for changes in the prop and update the form
watch(() => props.foodData, (newFood) => {
  if (newFood) {
    form.value = { ...newFood };
  } else {
    // Reset for 'add new' mode
    form.value = {
      foodName: '',
      foodExplain: '',
      foodPrice: 0,
      foodImg: '',
    };
  }
}, { immediate: true });

const rules = ref<FormRules>({
  foodName: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  foodPrice: [{ required: true, message: '请输入价格', trigger: 'blur' }],
});

const getFormData = async () => {
  if (!formRef.value) return null;
  try {
    await formRef.value.validate();
    return form.value;
  } catch (error) {
    return null;
  }
};

// Expose the getFormData method to the parent component
defineExpose({
  getFormData,
});

</script>

<style scoped>
.avatar-uploader .avatar {
  width: 178px;
  height: 178px;
  display: block;
}
</style>

<style>
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
}
</style>
