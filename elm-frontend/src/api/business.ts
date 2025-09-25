import request from '../utils/request';
import type { Business, HttpResultBusiness, HttpResultListBusiness } from './types';

/**
 * @description Retrieves a list of all businesses.
 * @param {any} [params] - Optional query parameters.
 * @returns {Promise<HttpResultListBusiness>}
 * @see {@link openapi.json} - operationId: "getBusinesses"
 */
export const getBusinesses = (params?: any): Promise<HttpResultListBusiness> => {
  return request.get('/businesses', { params });
};

/**
 * @description Creates a new business.
 * @param {Business} data - The business object to create.
 * @returns {Promise<HttpResultBusiness>}
 * @see {@link openapi.json} - operationId: "addBusiness"
 */
export const addBusiness = (data: Partial<Business>): Promise<HttpResultBusiness> => {
  return request.post('/businesses', data);
};

/**
 * @description Retrieves all businesses owned by the current user.
 * @returns {Promise<HttpResultListBusiness>}
 * @see {@link openapi.json} - operationId: "getCurrentUserBusinesses"
 */
export const getCurrentUserBusinesses = (): Promise<HttpResultListBusiness> => {
  return request.get('/businesses/my');
};

/**
 * @description Retrieves a single business by its ID.
 * @param {number} id - The ID of the business.
 * @returns {Promise<HttpResultBusiness>}
 * @see {@link openapi.json} - operationId: "getBusiness"
 */
export const getBusinessById = (id: number): Promise<HttpResultBusiness> => {
  return request.get(`/businesses/${id}`);
};

/**
 * @description Updates a business by its ID.
 * @param {number} id - The ID of the business to update.
 * @param {Business} data - The full business object to update.
 * @returns {Promise<HttpResultBusiness>}
 * @see {@link openapi.json} - operationId: "updateBusiness"
 */
export const updateBusiness = (id: number, data: Business): Promise<HttpResultBusiness> => {
  return request.put(`/businesses/${id}`, data);
};

/**
 * @description Deletes a business by its ID.
 * @param {number} id - The ID of the business to delete.
 * @returns {Promise<HttpResultBusiness>}
 * @see {@link openapi.json} - operationId: "deleteBusiness"
 */
export const deleteBusiness = (id: number): Promise<HttpResultBusiness> => {
  return request.delete(`/businesses/${id}`);
};

/**
 * @description Partially updates a business by its ID.
 * @param {number} id - The ID of the business to patch.
 * @param {Partial<Business>} data - The partial business data to update.
 * @returns {Promise<HttpResultBusiness>}
 * @see {@link openapi.json} - operationId: "patchBusiness"
 */
export const patchBusiness = (id: number, data: Partial<Business>): Promise<HttpResultBusiness> => {
  return request.patch(`/businesses/${id}`, data);
};
