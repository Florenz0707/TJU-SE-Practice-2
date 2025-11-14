<template>
  <div class="wallet-view">
    <el-card class="wallet-summary">
      <template #header>
        <div class="card-header">
          <span>我的钱包</span>
        </div>
      </template>
      <div class="balance">
        <h2>余额: ¥{{ walletStore.walletBalance.toFixed(2) }}</h2>
      </div>
    </el-card>

    <el-tabs type="border-card" class="transaction-tabs">
      <el-tab-pane label="收入记录">
        <el-table :data="walletStore.incomingTransactions" style="width: 100%">
          <el-table-column prop="id" label="ID" width="180" />
          <el-table-column prop="amount" label="金额" />
          <el-table-column prop="type" label="类型" />
          <el-table-column prop="finished" label="已完成">
            <template #default="scope">
              <el-tag :type="scope.row.finished ? 'success' : 'danger'">
                {{ scope.row.finished ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="支出记录">
        <el-table :data="walletStore.outgoingTransactions" style="width: 100%">
          <el-table-column prop="id" label="ID" width="180" />
          <el-table-column prop="amount" label="金额" />
          <el-table-column prop="type" label="类型" />
          <el-table-column prop="finished" label="已完成">
            <template #default="scope">
              <el-tag :type="scope.row.finished ? 'success' : 'danger'">
                {{ scope.row.finished ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useWalletStore } from '../../../store/wallet';

const walletStore = useWalletStore();

onMounted(() => {
  walletStore.fetchMyWallet();
  walletStore.fetchTransactions();
});
</script>

<style scoped>
.wallet-view {
  padding: 20px;
}
.wallet-summary {
  margin-bottom: 20px;
}
.balance {
  font-size: 24px;
}
</style>
