<template>
  <div class="mobile-wallet-view">
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
        <div v-for="transaction in walletStore.incomingTransactions" :key="transaction.id" class="transaction-item">
          <p><strong>ID:</strong> {{ transaction.id }}</p>
          <p><strong>金额:</strong> ¥{{ transaction.amount }}</p>
          <p><strong>类型:</strong> {{ transaction.type }}</p>
          <p><strong>已完成:</strong> {{ transaction.finished ? '是' : '否' }}</p>
        </div>
      </el-tab-pane>
      <el-tab-pane label="支出记录">
        <div v-for="transaction in walletStore.outgoingTransactions" :key="transaction.id" class="transaction-item">
          <p><strong>ID:</strong> {{ transaction.id }}</p>
          <p><strong>金额:</strong> ¥{{ transaction.amount }}</p>
          <p><strong>类型:</strong> {{ transaction.type }}</p>
          <p><strong>已完成:</strong> {{ transaction.finished ? '是' : '否' }}</p>
        </div>
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
.mobile-wallet-view {
  padding: 10px;
}
.wallet-summary {
  margin-bottom: 10px;
}
.balance {
  font-size: 20px;
}
.transaction-item {
  border-bottom: 1px solid #eee;
  padding: 10px 0;
}
.transaction-item p {
  margin: 5px 0;
}
</style>
