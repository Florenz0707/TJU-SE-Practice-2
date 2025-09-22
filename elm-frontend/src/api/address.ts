import request from '../utils/request';
import type {
  DeliveryAddress,
  HttpResultDeliveryAddress,
  HttpResultListDeliveryAddress,
  HttpResultEmpty,
} from './types';

/**
 * @description Retrieves the current user's delivery addresses.
 * @returns {Promise<HttpResultListDeliveryAddress>}
 * @see {@link openapi.json} - operationId: "getCurrentUserAddresses"
 */
export const getCurrentUserAddresses = (): Promise<HttpResultListDeliveryAddress> => {
  return request.get('/addresses');
};

/**
 * @description Adds a new delivery address for the current user.
 * @param {DeliveryAddress} data - The address data to add.
 * @returns {Promise<HttpResultDeliveryAddress>}
 * @see {@link openapi.json} - operationId: "addDeliveryAddress"
 */
export const addDeliveryAddress = (data: DeliveryAddress): Promise<HttpResultDeliveryAddress> => {
  return request.post('/addresses', data);
};

/**
 * @description Updates an existing delivery address.
 * @param {number} id - The ID of the address to update.
 * @param {DeliveryAddress} data - The updated address data.
 * @returns {Promise<HttpResultDeliveryAddress>}
 * @see {@link openapi.json} - operationId: "updateDeliveryAddress"
 */
export const updateDeliveryAddress = (id: number, data: DeliveryAddress): Promise<HttpResultDeliveryAddress> => {
  return request.put(`/addresses/${id}`, data);
};

/**
 * @description Deletes a delivery address.
 * @param {number} id - The ID of the address to delete.
 * @returns {Promise<HttpResultEmpty>}
 * @see {@link openapi.json} - operationId: "deleteDeliveryAddress"
 */
export const deleteDeliveryAddress = (id: number): Promise<HttpResultEmpty> => {
  return request.delete(`/addresses/${id}`);
};
