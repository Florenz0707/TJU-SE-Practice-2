import request from '../utils/request';
import type { HttpResult } from './types';
import type { Transaction } from './types';

export function createTransaction(transaction: Transaction): Promise<HttpResult<Transaction>> {
  return request.post('/transaction', transaction);
}

export function finishTransaction(id: number, isFinished: boolean): Promise<HttpResult<Transaction>> {
  return request.patch(`/transaction/finished?id=${id}`, { isFinished });
}

export function getTransactionById(id: number): Promise<HttpResult<Transaction>> {
  return request.get(`/transaction/${id}`);
}

export function getTransactionsByWalletId(walletId: number): Promise<HttpResult<{ inTransactions: Transaction[], outTransactions: Transaction[] }>> {
  return request.get(`/transaction/list/${walletId}`);
}
