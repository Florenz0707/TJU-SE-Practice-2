<template>
  <div class="my-application-container" v-loading="loading">
    <!-- 头部 -->
    <div class="header">
      <h2>我的开店申请</h2>
      <el-button type="primary" @click="openApplicationDialog">申请开店</el-button>
    </div>

    <!-- 无数据提示 -->
    <div v-if="applications.length === 0" class="no-data">
      <p class="text-gray">您还没有提交任何开店申请。</p>
    </div>

    <!-- 时间线 -->
    <el-timeline v-else>
      <el-timeline-item
        v-for="app in sortedApplications"
        :key="app.id"
        :timestamp="`提交于 ${new Date(app.createTime || '').toLocaleDateString()}`"
        :type="statusType(app.applicationState)"
      >
        <div class="timeline-item">
          <h4 class="item-title">{{ app.business.businessName }}</h4>
          <p class="item-address">{{ app.business.businessAddress }}</p>
          <p class="item-desc"><strong>申请说明:</strong> {{ app.applicationExplain }}</p>
          <p class="item-status">
            <strong>状态:</strong>
            <el-tag :type="statusType(app.applicationState)">
              {{ statusText(app.applicationState) }}
            </el-tag>
          </p>
        </div>
      </el-timeline-item>
    </el-timeline>

    <!-- 申请对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="申请开店"
      width="600px"
      :before-close="handleClose"
    >
      <el-form :model="applicationData" ref="applicationFormRef" label-width="120px">
        <el-form-item
          label="店铺名称"
          prop="business.businessName"
          :rules="[{ required: true, message: '请输入店铺名称' }]"
        >
          <el-input v-model="applicationData.business.businessName" />
        </el-form-item>

        <el-form-item
          label="店铺地址"
          prop="business.businessAddress"
          :rules="[{ required: true, message: '请输入店铺地址' }]"
        >
          <el-input v-model="applicationData.business.businessAddress" />
        </el-form-item>

        <el-form-item label="店铺简介">
          <el-input v-model="applicationData.business.businessExplain" type="textarea" />
        </el-form-item>

        <el-form-item label="起送价">
          <el-input-number v-model="applicationData.business.startPrice" :min="0" />
        </el-form-item>

        <el-form-item label="配送费">
          <el-input-number v-model="applicationData.business.deliveryPrice" :min="0" />
        </el-form-item>

        <el-form-item
          label="申请说明"
          prop="applicationExplain"
          :rules="[{ required: true, message: '请输入申请说明' }]"
        >
          <el-input v-model="applicationData.applicationExplain" type="textarea" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitApplication">提交申请</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, nextTick } from 'vue'
import {
  ElMessage,
  type FormInstance
} from 'element-plus'
import {
  getMyBusinessApplications,
  submitBusinessApplication,
  ApplicationState
} from '../../api/applicationService'
import type { BusinessApplication, Business } from '../../api/types'

const loading = ref(false)

const applications = ref<BusinessApplication[]>([])
const dialogVisible = ref(false)
const applicationFormRef = ref<FormInstance>()

const applicationData = reactive({
  business: {
    businessName: '',
    businessAddress: '',
    businessExplain: '',
    startPrice: 0,
    deliveryPrice: 0
  } as Business,
  applicationExplain: ''
})

const sortedApplications = computed(() =>
  [...applications.value].sort(
    (a, b) =>
      new Date(b.createTime || 0).getTime() -
      new Date(a.createTime || 0).getTime()
  )
)

const fetchApplications = async () => {
  loading.value = true
  try {
    applications.value = await getMyBusinessApplications()
  } catch (error) {
    console.error('获取申请列表失败:', error)
    ElMessage.error('获取申请列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchApplications)

const openApplicationDialog = () => {
  // Reset form
  Object.assign(applicationData, {
    business: {
      businessName: '',
      businessAddress: '',
      businessExplain: '',
      startPrice: 0,
      deliveryPrice: 0
    },
    applicationExplain: ''
  })
  nextTick(() => {
    applicationFormRef.value?.clearValidate()
  })
  dialogVisible.value = true
}

const handleClose = (done: () => void) => {
  dialogVisible.value = false
  done()
}

const submitApplication = async () => {
  if (!applicationFormRef.value) return
  await applicationFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await submitBusinessApplication(applicationData)
        ElMessage.success('申请已提交')
        dialogVisible.value = false
        await fetchApplications()
      } catch (error) {
        ElMessage.error('申请提交失败')
        console.error('Failed to submit application:', error)
      } finally {
        loading.value = false
      }
    }
  })
}

const statusText = (state?: number) => {
  switch (state) {
    case ApplicationState.UNDISPOSED:
      return '待处理'
    case ApplicationState.APPROVED:
      return '已批准'
    case ApplicationState.REJECTED:
      return '已拒绝'
    default:
      return '未知'
  }
}

const statusType = (state?: number) => {
  switch (state) {
    case ApplicationState.UNDISPOSED:
      return 'warning'
    case ApplicationState.APPROVED:
      return 'success'
    case ApplicationState.REJECTED:
      return 'danger'
    default:
      return 'info'
  }
}
</script>

<style scoped>
.my-application-container {
  padding: 2rem;
  background-color: transparent;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.no-data {
  text-align: center;
  padding: 40px;
}

.text-gray {
  color: #6b7280;
}

.timeline-item {
  padding: 16px 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background-color: #f9fafb;
}

.item-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.item-address {
  color: #666;
  margin-bottom: 8px;
}

.item-desc,
.item-status {
  margin-top: 8px;
}
</style>
