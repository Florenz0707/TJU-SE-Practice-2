<template>
  <div class="points-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>积分规则管理</span>
          <el-button type="primary" @click="openRuleDialog()">添加规则</el-button>
        </div>
      </template>

      <el-table :data="rules" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="channelType" label="渠道类型" />
        <el-table-column prop="ratio" label="兑换比例/值" />
        <el-table-column prop="expireDays" label="有效期(天)">
            <template #default="scope">
                {{ scope.row.expireDays === -1 ? '永久' : scope.row.expireDays }}
            </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="isEnabled" label="启用">
          <template #default="scope">
            <el-tag :type="scope.row.isEnabled ? 'success' : 'info'">
              {{ scope.row.isEnabled ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="openRuleDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑积分规则' : '添加积分规则'">
      <el-form :model="ruleForm" label-width="120px">
        <el-form-item label="渠道类型">
          <el-select v-model="ruleForm.channelType" placeholder="请选择渠道">
             <el-option
              v-for="item in channelTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="兑换比例/值">
            <el-input-number v-model="ruleForm.ratio" :min="0" :precision="2" />
            <div class="form-tip">订单类型为1元=多少积分，其他类型为固定积分值</div>
        </el-form-item>
        <el-form-item label="有效期(天)">
          <el-input-number v-model="ruleForm.expireDays" :min="-1" />
           <div class="form-tip">-1表示永久有效</div>
        </el-form-item>
         <el-form-item label="描述">
          <el-input v-model="ruleForm.description" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.isEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getAllPointsRules, createPointsRule, updatePointsRule, deletePointsRule } from '../../api/points';
import { type PointsRule, PointsRuleType } from '../../api/types';

const rules = ref<PointsRule[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const ruleForm = ref<PointsRule>({
  channelType: 'ORDER',
  ratio: 1.0,
  expireDays: 365,
  description: '',
  isEnabled: true,
});

const channelTypes = [
    { label: '订单', value: PointsRuleType.ORDER },
    { label: '评价', value: PointsRuleType.COMMENT },
    { label: '登录', value: PointsRuleType.LOGIN },
    { label: '注册', value: PointsRuleType.REGISTER }
];

async function fetchRules() {
  try {
    const response = await getAllPointsRules();
    rules.value = response.data;
  } catch (error) {
    ElMessage.error('获取积分规则失败。');
  }
}

function openRuleDialog(rule?: PointsRule) {
  if (rule) {
    isEdit.value = true;
    ruleForm.value = { ...rule };
  } else {
    isEdit.value = false;
    ruleForm.value = {
        channelType: 'ORDER',
        ratio: 1.0,
        expireDays: 365,
        description: '',
        isEnabled: true,
    };
  }
  dialogVisible.value = true;
}

async function saveRule() {
  try {
    if (isEdit.value && ruleForm.value.id) {
      await updatePointsRule(ruleForm.value.id, ruleForm.value);
      ElMessage.success('规则更新成功。');
    } else {
      await createPointsRule(ruleForm.value);
      ElMessage.success('规则添加成功。');
    }
    dialogVisible.value = false;
    fetchRules();
  } catch (error) {
    ElMessage.error('保存规则失败。');
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('您确定要删除此规则吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await deletePointsRule(id);
    ElMessage.success('规则删除成功。');
    fetchRules();
  } catch (error) {
      // cancel
  }
}

onMounted(() => {
  fetchRules();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-tip {
    font-size: 12px;
    color: #999;
    margin-left: 10px;
}
</style>
