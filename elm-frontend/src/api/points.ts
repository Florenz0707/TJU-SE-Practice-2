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

// NOTE: The following functions were removed because they were calling internal APIs
// that should only be used by backend services, not by the frontend.
// Points freezing, deduction, and rollback should be handled automatically by the backend
// during order processing. Points notifications for orders and reviews should also be
// handled by the backend.

// The following interfaces and functions have been removed:
// - freezePoints, deductPoints, rollbackPoints (Points Trade)
// - notifyOrderSuccess, notifyReviewSuccess (Points Notification)
