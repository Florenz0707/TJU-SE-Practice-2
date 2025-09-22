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
        <el-form-item label="店铺图片URL" prop="businessImg">
          <el-input v-model="business.businessImg" />
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
import { ref, onMounted } from 'vue'
import { getCurrentUserBusinesses, updateBusiness } from '../../api/business'
import type { Business, HttpResultListBusiness } from '../../api/types'
import { ElMessage } from 'element-plus'

const loading = ref(true)
const business = ref<Business | null>(null)

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
</style>
