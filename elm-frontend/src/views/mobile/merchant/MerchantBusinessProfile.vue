<template>
  <div v-loading="loading" class="mobile-business-profile">
    <div class="header">
      <h4>店铺信息管理</h4>
    </div>
    <el-card v-if="business">
      <el-form :model="business" label-position="top" ref="formRef">
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
        <el-form-item label="店铺名称" prop="businessName">
          <el-input v-model="business.businessName" />
        </el-form-item>
        <el-form-item label="店铺地址" prop="businessAddress">
          <el-input v-model="business.businessAddress" />
        </el-form-item>
        <el-form-item label="店铺介绍" prop="businessExplain">
          <el-input type="textarea" v-model="business.businessExplain" />
        </el-form-item>
        <el-form-item label="起送价" prop="startPrice">
          <el-input-number v-model="business.startPrice" :precision="2" :step="1" controls-position="right" class="full-width-input" />
        </el-form-item>
        <el-form-item label="配送费" prop="deliveryPrice">
          <el-input-number v-model="business.deliveryPrice" :precision="2" :step="1" controls-position="right" class="full-width-input" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" class="full-width-btn">保存更改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-empty v-else-if="!loading" description="未能加载店铺信息"></el-empty>

    <el-card class="menu-card" style="margin-top: 1rem;">
      <div class="menu-item" @click="showRoles = !showRoles">
        <span>切换身份</span>
        <ChevronRight :size="20" color="#999" />
      </div>
      <el-collapse-transition>
        <div v-show="showRoles">
          <router-link to="/mobile/home" class="menu-item sub-item">
            <span>顾客</span>
          </router-link>
          <router-link v-if="isMerchant" to="/mobile/merchant/dashboard" class="menu-item sub-item">
            <span>商家</span>
          </router-link>
          <router-link v-if="isAdmin" to="/mobile/admin/dashboard" class="menu-item sub-item">
            <span>管理</span>
          </router-link>
        </div>
      </el-collapse-transition>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAuthStore } from '../../../store/auth';
import { useBusinessStore } from '../../../store/business';
import { storeToRefs } from 'pinia';
import { updateBusiness } from '../../../api/business';
import { ElMessage, type UploadProps } from 'element-plus';
import { Plus, ChevronRight } from 'lucide-vue-next';

const loading = ref(false);
const businessStore = useBusinessStore();
const { selectedBusinessId, businesses } = storeToRefs(businessStore);
const authStore = useAuthStore();
const showRoles = ref(false);

const business = computed({
  get: () => businesses.value.find(b => b.id === selectedBusinessId.value) || null,
  set: (val) => {
    if (val) {
      const index = businesses.value.findIndex(b => b.id === val.id);
      if (index !== -1) {
        businesses.value[index] = val;
      }
    }
  }
});

const isMerchant = computed(() => authStore.userRoles.includes('MERCHANT'));
const isAdmin = computed(() => authStore.userRoles.includes('ADMIN'));

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


const handleSave = async () => {
  if (!business.value || !business.value.id) {
    ElMessage.error('没有可保存的店铺信息');
    return;
  }
  loading.value = true;
  try {
    await updateBusiness(business.value.id, business.value);
    ElMessage.success('店铺信息更新成功！');
  } catch (error) {
    ElMessage.error('更新失败，请稍后重试');
    console.error(error);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.mobile-business-profile {
  padding: 1rem;
}
.header {
  margin-bottom: 1rem;
}
.avatar-uploader .avatar {
  width: 100%;
  height: auto;
  max-width: 200px;
  display: block;
  margin: 0 auto;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100%;
  height: 150px;
  line-height: 150px;
  text-align: center;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
}
.full-width-input {
  width: 100%;
}
.full-width-btn {
  width: 100%;
}
</style>