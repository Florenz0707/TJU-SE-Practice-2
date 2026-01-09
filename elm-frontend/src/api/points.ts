import request from '../utils/request';
import type { HttpResult, PointsAccount, PointsRecord, PointsRule } from './types';

// Account
export function getMyPointsAccount(): Promise<HttpResult<PointsAccount>> {
  return request.get('/points/account/my');
}

// Records
export function getMyPointsRecords(page: number, size: number, type?: number): Promise<HttpResult<{records: PointsRecord[], total: number}>> {
  const params: any = { page, size };
  if (type !== undefined) {
    params.type = type;
  }
  return request.get('/points/record/my', { params });
}

// Rules (Admin)
export function createPointsRule(rule: PointsRule): Promise<HttpResult<PointsRule>> {
  return request.post('/points/admin/rules', rule);
}

export function updatePointsRule(id: number, rule: PointsRule): Promise<HttpResult<PointsRule>> {
  return request.put(`/points/admin/rules/${id}`, rule);
}

export function deletePointsRule(id: number): Promise<HttpResult<any>> {
  return request.delete(`/points/admin/rules/${id}`);
}

export function getAllPointsRules(): Promise<HttpResult<PointsRule[]>> {
  return request.get('/points/admin/rules');
}

// Points Trade (Internal API)
export interface FreezePointsRequest {
  userId: number;
  points: number;
  tempOrderId: string;
}

export interface FreezePointsResponse {
  success: boolean;
  pointsUsed: number;
  moneySaved: number;
  balanceSnap: number;
  message: string;
}

export function freezePoints(data: FreezePointsRequest): Promise<HttpResult<FreezePointsResponse>> {
  return request.post('/inner/points/trade/freeze', data);
}

export interface DeductPointsRequest {
  userId: number;
  tempOrderId: string;
  finalOrderId: string;
}

export function deductPoints(data: DeductPointsRequest): Promise<HttpResult<boolean>> {
  return request.post('/inner/points/trade/deduct', data);
}

export interface RollbackPointsRequest {
  userId: number;
  tempOrderId: string;
  reason: string;
}

export function rollbackPoints(data: RollbackPointsRequest): Promise<HttpResult<boolean>> {
  return request.post('/inner/points/trade/rollback', data);
}

// Points Notification (Internal API)
export interface OrderSuccessNotifyRequest {
  userId: number;
  bizId: string;
  amount: number;
  eventTime: string;
  extraInfo: string;
}

export interface ReviewSuccessNotifyRequest {
  userId: number;
  bizId: string;
  amount: number;
  eventTime: string;
  extraInfo: string;
}

export function notifyOrderSuccess(data: OrderSuccessNotifyRequest): Promise<HttpResult<number>> {
  return request.post('/inner/points/notify/order-success', data);
}

export function notifyReviewSuccess(data: ReviewSuccessNotifyRequest): Promise<HttpResult<number>> {
  return request.post('/inner/points/notify/review-success', data);
}
