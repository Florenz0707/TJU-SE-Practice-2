import request from '../utils/request';
import type {
  BusinessApplication,
  MerchantApplication,
  HttpResult,
} from './types';

// Business Application APIs

/**
 * @description Submits a new business application.
 * @param {BusinessApplication} data - The application data.
 * @returns {Promise<HttpResult<BusinessApplication>>}
 * @see {@link openapi.json} - operationId: "submitBusinessApplication"
 */
export const submitBusinessApplication = (data: BusinessApplication): Promise<HttpResult<BusinessApplication>> => {
  return request.post('/applications/business', data);
};

/**
 * @description Retrieves all business applications.
 * @returns {Promise<HttpResult<BusinessApplication[]>>}
 * @see {@link openapi.json} - operationId: "getBusinessApplications"
 */
export const getBusinessApplications = (): Promise<HttpResult<BusinessApplication[]>> => {
  return request.get('/applications/business');
};

/**
 * @description Retrieves a single business application by its ID.
 * @param {number} id - The ID of the application.
 * @returns {Promise<HttpResult<BusinessApplication>>}
 * @see {@link openapi.json} - operationId: "getBusinessApplicationById"
 */
export const getBusinessApplicationById = (id: number): Promise<HttpResult<BusinessApplication>> => {
  return request.get(`/applications/business/${id}`);
};

/**
 * @description Approves or rejects a business application.
 * @param {number} id - The ID of the application.
 * @param {Partial<BusinessApplication>} data - The update data, including the new state.
 * @returns {Promise<HttpResult<BusinessApplication>>}
 * @see {@link openapi.json} - operationId: "approveBusinessApplication"
 */
export const approveBusinessApplication = (id: number, data: Partial<BusinessApplication>): Promise<HttpResult<BusinessApplication>> => {
  return request.patch(`/applications/business/${id}`, data);
};

/**
 * @description Retrieves all business applications for the current user.
 * @returns {Promise<HttpResult<BusinessApplication[]>>}
 * @see {@link openapi.json} - operationId: "getMyBusinessApplications"
 */
export const getMyBusinessApplications = (): Promise<HttpResult<BusinessApplication[]>> => {
  return request.get('/applications/business/my');
};

// Merchant Application APIs

/**
 * @description Submits a merchant application.
 * @param {Partial<MerchantApplication>} applicationData - The application data.
 * @returns {Promise<HttpResult<MerchantApplication>>}
 * @see {@link openapi.json} - operationId: "submitMerchantApplication"
 */
export const applyToBeMerchant = (applicationData: Partial<MerchantApplication>): Promise<HttpResult<MerchantApplication>> => {
  return request.post('/applications/merchant', applicationData);
};

/**
 * @description Retrieves all merchant applications.
 * @returns {Promise<HttpResult<MerchantApplication[]>>}
 * @see {@link openapi.json} - operationId: "getMerchantApplications"
 */
export const getMerchantApplications = (): Promise<HttpResult<MerchantApplication[]>> => {
  return request.get('/applications/merchant');
};

/**
 * @description Retrieves a single merchant application by its ID.
 * @param {number} id - The ID of the application.
 * @returns {Promise<HttpResult<MerchantApplication>>}
 * @see {@link openapi.json} - operationId: "getMerchantApplicationById"
 */
export const getMerchantApplicationById = (id: number): Promise<HttpResult<MerchantApplication>> => {
  return request.get(`/applications/merchant/${id}`);
};

/**
 * @description Approves or rejects a merchant application.
 * @param {number} id - The ID of the application.
 * @param {Partial<MerchantApplication>} data - The approval data.
 * @returns {Promise<HttpResult<MerchantApplication>>}
 * @see {@link openapi.json} - operationId: "approveMerchantApplication"
 */
export const approveMerchantApplication = (id: number, data: Partial<MerchantApplication>): Promise<HttpResult<MerchantApplication>> => {
  return request.patch(`/applications/merchant/${id}`, data);
};

/**
 * @description Retrieves all merchant applications for the current user.
 * @returns {Promise<HttpResult<MerchantApplication[]>>}
 * @see {@link openapi.json} - operationId: "getMyMerchantApplications"
 */
export const getMyMerchantApplications = (): Promise<HttpResult<MerchantApplication[]>> => {
  return request.get('/applications/merchant/my');
};