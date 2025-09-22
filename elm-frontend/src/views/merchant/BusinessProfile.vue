<template>
  <div v-loading="loading" class="business-profile-container">
    <h2>店铺信息管理</h2>
    <el-card v-if="business">
      <el-form :model="business" label-width="120px" ref="formRef">
        <el-form-item label="店铺名称" prop="businessName">
          <el-input v-model="business.businessName" />
        </el-form-item>
        <el-form-item label="店铺地址" prop="businessAddress">
          <el-input v-model="business.businessAddress" />
        </el-form-item>
        <el-form-item label="店铺介绍" prop="businessExplain">
          <el-input type="textarea" v-model="business.businessExplain" />
        </el-form-item>
        <el-form-item label="店铺图片" prop="businessImg">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :before-upload="handleBeforeUpload"
          >
            <img v-if="business.businessImg" :src="`data:image/jpeg;base64,${business.businessImg}`" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="起送价" prop="startPrice">
          <el-input-number
            v-model="business.startPrice"
            :precision="2"
            :step="1"
          />
        </el-form-item>
        <el-form-item label="配送费" prop="deliveryPrice">
          <el-input-number
            v-model="business.deliveryPrice"
            :precision="2"
            :step="1"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">保存更改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-empty v-else-if="!loading" description="未能加载店铺信息"></el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getCurrentUserBusinesses, updateBusiness } from '../../api/business';
import type { Business, HttpResultListBusiness } from '../../api/types';
import { ElMessage, type UploadProps } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';

const loading = ref(true)
const business = ref<Business | null>(null)

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
    if (business.value) {
      const base64 = e.target?.result as string;
      business.value.businessImg = base64.split(',')[1];
    }
  };
  reader.readAsDataURL(rawFile);

  return false;
};

onMounted(async () => {
  try {
    const response: HttpResultListBusiness = await getCurrentUserBusinesses()
    if (response.success && response.data && response.data.length > 0) {
      const currentBusiness = response.data[0]
      if (currentBusiness) {
        business.value = currentBusiness
      } else {
        ElMessage.warning('Could not retrieve business details.')
      }
    } else {
      ElMessage.warning('当前用户没有关联的店铺')
    }
  } catch (error) {
    ElMessage.error('加载店铺信息失败')
    console.error(error)
  } finally {
    loading.value = false
  }
})

const handleSave = async () => {
  if (!business.value || !business.value.id) {
    ElMessage.error('没有可保存的店铺信息')
    return
  }
  loading.value = true
  try {
    await updateBusiness(business.value.id, business.value)
    ElMessage.success('店铺信息更新成功！')
  } catch (error) {
    ElMessage.error('更新失败，请稍后重试')
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.business-profile-container {
  padding: 20px;
}

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
