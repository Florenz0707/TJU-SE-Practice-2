import request from '../utils/request';
import type { HttpResult } from './types';
import type { Wallet } from './types';

export function getWalletById(id: number): Promise<HttpResult<Wallet>> {
  return request.get(`/wallet/${id}`);
}

export function getWalletOwnerById(id: number): Promise<HttpResult<any>> {
  return request.get(`/wallet/owner/${id}`);
}

export function getMyWallet(): Promise<HttpResult<Wallet>> {
  return request.get('/wallet/my');
}

export function createWallet(): Promise<HttpResult<Wallet>> {
  return request.post('/wallet');
}
