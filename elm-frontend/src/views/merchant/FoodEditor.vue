<template>
  <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
    <el-form-item label="菜品名称" prop="foodName">
      <el-input v-model="form.foodName" />
    </el-form-item>
    <el-form-item label="描述" prop="foodExplain">
      <el-input type="textarea" v-model="form.foodExplain" />
    </el-form-item>
    <el-form-item label="价格" prop="foodPrice">
      <el-input-number
        v-model="form.foodPrice"
        :precision="2"
        :step="0.1"
        :min="0"
      />
    </el-form-item>
    <el-form-item label="图片URL" prop="foodImg">
      <el-input v-model="form.foodImg" />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, watch, defineProps, defineExpose } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Food } from '../../api/types'

interface Props {
  foodData: Food | null
}

const props = defineProps<Props>()

const formRef = ref<FormInstance>()
const form = ref<Partial<Food>>({})

// Watch for changes in the prop and update the form
watch(
  () => props.foodData,
  newFood => {
    if (newFood) {
      form.value = { ...newFood }
    } else {
      // Reset for 'add new' mode
      form.value = {
        foodName: '',
        foodExplain: '',
        foodPrice: 0,
        foodImg: '',
      }
    }
  },
  { immediate: true }
)

const rules = ref<FormRules>({
  foodName: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  foodPrice: [{ required: true, message: '请输入价格', trigger: 'blur' }],
})

const getFormData = async () => {
  if (!formRef.value) return null
  try {
    await formRef.value.validate()
    return form.value
  } catch (error) {
    return null
  }
}

// Expose the getFormData method to the parent component
defineExpose({
  getFormData,
})
</script>
