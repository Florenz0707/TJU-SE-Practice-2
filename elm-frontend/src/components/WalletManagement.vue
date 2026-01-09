<template>
  <div class="wallet-view">
    <el-card class="wallet-summary">
      <template #header>
        <div class="card-header">
          <span>{{ title }}</span>
        </div>
      </template>
      <div v-if="loading" class="loading-container">
         <el-skeleton :rows="3" animated />
      </div>
      <div v-else class="balance-container">
        <h2>余额: ¥{{ walletStore.walletBalance.toFixed(2) }}</h2>
        <div class="wallet-info">
             <el-tag type="info">信用额度: ¥{{ wallet?.creditLimit || 0 }}</el-tag>
             <span class="withdrawal-info" v-if="wallet?.lastWithdrawalAt">上次提现: {{ new Date(wallet.lastWithdrawalAt).toLocaleString() }}</span>
        </div>
        <div class="wallet-actions">
           <el-button type="primary" @click="handleRecharge">充值</el-button>
           <el-button type="success" @click="handleWithdraw">提现</el-button>
        </div>
      </div>
    </el-card>

    <el-tabs type="border-card" class="transaction-tabs">
      <el-tab-pane label="收入记录">
        <el-table :data="walletStore.incomingTransactions" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="amount" label="金额" />
          <el-table-column prop="type" label="类型">
             <template #default="scope">
                {{ formatTransactionType(scope.row.type) }}
             </template>
          </el-table-column>
           <el-table-column prop="createTime" label="时间">
            <template #default="scope">
              {{ scope.row.createTime ? new Date(scope.row.createTime).toLocaleString() : '' }}
            </template>
          </el-table-column>
          <el-table-column prop="finished" label="状态">
            <template #default="scope">
              <el-tag :type="scope.row.finished ? 'success' : 'warning'">
                {{ scope.row.finished ? '已完成' : '处理中' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="支出记录">
        <el-table :data="walletStore.outgoingTransactions" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="amount" label="金额" />
          <el-table-column prop="type" label="类型">
             <template #default="scope">
                {{ formatTransactionType(scope.row.type) }}
             </template>
          </el-table-column>
           <el-table-column prop="createTime" label="时间">
            <template #default="scope">
              {{ scope.row.createTime ? new Date(scope.row.createTime).toLocaleString() : '' }}
            </template>
          </el-table-column>
          <el-table-column prop="finished" label="状态">
            <template #default="scope">
              <el-tag :type="scope.row.finished ? 'success' : 'warning'">
                {{ scope.row.finished ? '已完成' : '处理中' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

     <!-- Recharge Dialog -->
    <el-dialog v-model="rechargeDialogVisible" title="钱包充值">
      <el-form :model="rechargeForm">
        <el-form-item label="充值金额" label-width="100px">
          <el-input-number v-model="rechargeForm.amount" :min="1" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="rechargeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmRecharge">确认充值</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Withdraw Dialog -->
    <el-dialog v-model="withdrawDialogVisible" title="钱包提现">
      <el-form :model="withdrawForm">
        <el-form-item label="提现金额" label-width="100px">
          <el-input-number v-model="withdrawForm.amount" :min="1" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="withdrawDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmWithdraw">确认提现</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useWalletStore } from '../store/wallet';
import { ElMessage } from 'element-plus';
import { storeToRefs } from 'pinia';

const props = defineProps({
  title: {
    type: String,
    default: '我的钱包'
  },
  createSuccessMessage: {
    type: String,
    default: '为您自动创建了钱包'
  }
});

const walletStore = useWalletStore();
const { wallet } = storeToRefs(walletStore);
const loading = ref(true);

const rechargeDialogVisible = ref(false);
const withdrawDialogVisible = ref(false);
const rechargeForm = ref({ amount: 100 });
const withdrawForm = ref({ amount: 100 });

// Transaction Types Mappings
const TransactionType = {
    TRANSFER: 0,
    TOP_UP: 1, // Recharge
    WITHDRAW: 2,
    PAYMENT: 3,
    REFUND: 4
};

function formatTransactionType(type: number) {
    switch(type) {
        case TransactionType.TRANSFER: return '转账';
        case TransactionType.TOP_UP: return '充值';
        case TransactionType.WITHDRAW: return '提现';
        case TransactionType.PAYMENT: return '支付';
        case TransactionType.REFUND: return '退款';
        default: return '未知';
    }
}


onMounted(async () => {
    loading.value = true;
    await walletStore.fetchMyWallet();
    if (!walletStore.wallet) {
        // Lazy creation
        try {
            await walletStore.createWallet();
             ElMessage.success(props.createSuccessMessage);
        } catch(e) {
            ElMessage.error('无法创建钱包，请联系管理员');
        }
    }
    await walletStore.fetchTransactions();
    loading.value = false;
});

const handleRecharge = () => {
    rechargeForm.value.amount = 100;
    rechargeDialogVisible.value = true;
};

const handleWithdraw = () => {
    withdrawForm.value.amount = 100;
    withdrawDialogVisible.value = true;
};

const confirmRecharge = async () => {
    if (!walletStore.wallet) return;
    try {
        await walletStore.createTransaction({
            amount: rechargeForm.value.amount,
            type: TransactionType.TOP_UP,
            inWalletId: walletStore.wallet.id, // Money goes INTO wallet
            finished: true // Usually assumes instant success for mock/test
        });
        ElMessage.success('充值成功');
        rechargeDialogVisible.value = false;
    } catch (e) {
        ElMessage.error('充值失败');
    }
};

const confirmWithdraw = async () => {
    if (!walletStore.wallet) return;
    try {
        await walletStore.createTransaction({
            amount: withdrawForm.value.amount,
            type: TransactionType.WITHDRAW,
            outWalletId: walletStore.wallet.id, // Money comes OUT of wallet
            finished: true
        });
        ElMessage.success('提现申请已提交');
        withdrawDialogVisible.value = false;
    } catch (e) {
        ElMessage.error('提现失败');
    }
};

</script>

<style scoped>
.wallet-view {
  padding: 20px;
}
.wallet-summary {
  margin-bottom: 20px;
}
.balance-container {
    display: flex;
    flex-direction: column;
    gap: 15px;
}
.balance {
  font-size: 24px;
}
.wallet-info {
    display: flex;
    gap: 15px;
    align-items: center;
    color: #666;
    font-size: 14px;
}
.wallet-actions {
    margin-top: 10px;
}
</style>
