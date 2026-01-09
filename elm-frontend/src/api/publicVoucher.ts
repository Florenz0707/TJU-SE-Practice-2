import request from '../utils/request';
import type { HttpResult } from './types';
import type { PublicVoucher } from './types';

export function getAllPublicVouchers(): Promise<HttpResult<PublicVoucher[]>> {
  return request.get('/publicVoucher/list');
}

export function addPublicVoucher(voucher: PublicVoucher): Promise<HttpResult<any>> {
  return request.post('/publicVoucher', voucher);
}

// Updated based on prompt: PUT /api/publicVoucher removed path parameter id
export function updatePublicVoucher(voucher: PublicVoucher): Promise<HttpResult<any>> {
  return request.put(`/publicVoucher`, voucher);
}

export function getPublicVoucherById(id: string): Promise<HttpResult<PublicVoucher>> {
  return request.get(`/publicVoucher/${id}`);
}

export function deletePublicVoucher(id: string): Promise<HttpResult<any>> {
  return request.delete(`/publicVoucher/${id}`);
}
