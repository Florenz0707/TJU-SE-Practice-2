<template>
  <div class="mobile-points-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>积分规则</span>
          <el-button type="primary" size="small" @click="openDialog()">添加</el-button>
        </div>
      </template>

      <div class="rule-list">
        <div v-for="rule in rules" :key="rule.id" class="rule-item">
          <div class="rule-info">
             <div class="rule-header">
               <span class="channel-type">{{ rule.channelType }}</span>
               <el-tag size="small" :type="rule.isEnabled ? 'success' : 'info'">{{ rule.isEnabled ? '启用' : '禁用' }}</el-tag>
             </div>
             <div class="rule-detail">
               <span>比例: {{ rule.ratio }}</span>
               <span class="expiry">有效期: {{ rule.expireDays === -1 ? '永久' : rule.expireDays + '天' }}</span>
             </div>
          </div>
          <div class="rule-actions">
            <el-button size="small" circle icon="Edit" @click="openDialog(rule)"></el-button>
            <el-button size="small" circle icon="Delete" type="danger" @click="handleDelete(rule.id!)"></el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '添加规则'" width="90%">
      <el-form :model="form" label-position="top">
        <el-form-item label="渠道类型">
          <el-select v-model="form.channelType" :disabled="isEdit" style="width: 100%">
            <el-option label="订单" value="ORDER" />
            <el-option label="评价" value="COMMENT" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="注册" value="REGISTER" />
          </el-select>
        </el-form-item>

        <el-form-item :label="form.channelType === 'ORDER' ? '订单比例 (1元=X积分)' : '固定积分数量'">
          <el-input-number v-model="form.ratio" :step="0.1" :min="0" style="width: 100%" />
        </el-form-item>

        <el-form-item label="有效期(天, -1永久)">
          <el-input-number v-model="form.expireDays" :min="-1" style="width: 100%" />
        </el-form-item>

        <el-form-item label="是否启用">
          <el-switch v-model="form.isEnabled" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getAllPointsRules, createPointsRule, updatePointsRule, deletePointsRule } from '../../../api/points';
import type { PointsRule } from '../../../api/types';
import { ElMessage, ElMessageBox } from 'element-plus';

const rules = ref<PointsRule[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = ref<PointsRule>({
  channelType: 'ORDER',
  ratio: 1.0,
  expireDays: 30,
  description: '',
  isEnabled: true,
});

const fetchRules = async () => {
  try {
    const res = await getAllPointsRules();
    if (res.success) {
      rules.value = res.data;
    }
  } catch (e) {
    ElMessage.error('获取规则失败');
  }
};

const openDialog = (rule?: PointsRule) => {
  if (rule) {
    isEdit.value = true;
    form.value = { ...rule };
  } else {
    isEdit.value = false;
    form.value = {
      channelType: 'ORDER',
      ratio: 1.0,
      expireDays: 30,
      description: '',
      isEnabled: true,
    };
  }
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  try {
    if (isEdit.value && form.value.id) {
      await updatePointsRule(form.value.id, form.value);
      ElMessage.success('更新成功');
    } else {
      await createPointsRule(form.value);
      ElMessage.success('创建成功');
    }
    dialogVisible.value = false;
    fetchRules();
  } catch (e) {
    ElMessage.error('保存失败');
  }
};

const handleDelete = async (id: number) => {
  ElMessageBox.confirm('确定删除该规则吗?', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deletePointsRule(id);
      ElMessage.success('删除成功');
      fetchRules();
    } catch (e) {
      ElMessage.error('删除失败');
    }
  });
};

onMounted(() => {
  fetchRules();
});
</script>

<style scoped>
.mobile-points-management { padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.rule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}
.rule-info { flex: 1; }
.rule-header { display: flex; align-items: center; gap: 10px; margin-bottom: 5px; }
.channel-type { font-weight: bold; }
.rule-detail { font-size: 12px; color: #666; display: flex; gap: 10px; }
.rule-actions { display: flex; gap: 5px; }
</style>
