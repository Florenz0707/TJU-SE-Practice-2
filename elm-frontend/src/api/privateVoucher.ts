import request from '../utils/request';
import type { HttpResult, PrivateVoucher } from './types';

export function claimVoucher(publicVoucherId: number): Promise<HttpResult<any>> {
  return request.post(`/privateVoucher/claim/${publicVoucherId}`);
}

export function getMyVouchers(): Promise<HttpResult<PrivateVoucher[]>> {
  return request.get('/privateVoucher/my');
}

export function redeemVoucher(id: number): Promise<HttpResult<boolean>> {
  return request.post(`/privateVoucher/redeem/${id}`);
}
