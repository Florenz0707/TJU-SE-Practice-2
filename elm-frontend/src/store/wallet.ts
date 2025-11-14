import { defineStore } from 'pinia';
import { getMyWallet, createWallet } from '../api/wallet';
import { getTransactionsByWalletId, createTransaction, finishTransaction } from '../api/transaction';
import type { Wallet, Transaction } from '../api/types';

interface WalletState {
  wallet: Wallet | null;
  transactions: {
    in: Transaction[];
    out: Transaction[];
  };
}

export const useWalletStore = defineStore('wallet', {
  state: (): WalletState => ({
    wallet: null,
    transactions: {
      in: [],
      out: [],
    },
  }),

  getters: {
    walletBalance: (state): number => state.wallet?.balance ?? 0,
    incomingTransactions: (state): Transaction[] => state.transactions.in,
    outgoingTransactions: (state): Transaction[] => state.transactions.out,
  },

  actions: {
    async fetchMyWallet() {
      try {
        const response = await getMyWallet();
        this.wallet = response.data;
      } catch (error) {
        console.error('Failed to fetch wallet:', error);
      }
    },

    async fetchTransactions() {
      if (!this.wallet) {
        await this.fetchMyWallet();
      }
      if (this.wallet) {
        try {
          const response = await getTransactionsByWalletId(this.wallet.id);
          this.transactions.in = response.data.inTransactions;
          this.transactions.out = response.data.outTransactions;
        } catch (error) {
          console.error('Failed to fetch transactions:', error);
        }
      }
    },

    async createWallet() {
      try {
        const response = await createWallet();
        this.wallet = response.data;
      } catch (error) {
        console.error('Failed to create wallet:', error);
      }
    },

    async createTransaction(transaction: Transaction) {
      try {
        await createTransaction(transaction);
        await this.fetchTransactions(); // Refresh transactions after creating a new one
      } catch (error) {
        console.error('Failed to create transaction:', error);
      }
    },

    async finishTransaction(id: number) {
      try {
        await finishTransaction(id, true);
        await this.fetchTransactions(); // Refresh transactions after finishing one
      } catch (error) {
        console.error('Failed to finish transaction:', error);
      }
    },
  },
});
