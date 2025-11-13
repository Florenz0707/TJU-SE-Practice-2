import request from '../utils/request';
import type { Order, Review, HttpResultOrder, HttpResultListOrder, HttpResult } from './types';

/**
 * @description Retrieves a list of orders for the current customer.
 * @returns {Promise<HttpResultListOrder>} A promise that resolves to the list of orders.
 */
export const getMyOrdersCustomer = (): Promise<HttpResultListOrder> => {
  return request.get('/orders/user/my');
};

/**
 * @description Retrieves a list of orders for the current merchant.
 * @returns {Promise<HttpResultListOrder>} A promise that resolves to the list of orders.
 */
export const getMyOrdersMerchant = (): Promise<HttpResultListOrder> => {
  return request.get('/orders/merchant/my');
};

/**
 * @description Retrieves a list of orders for a specific business.
 * @param {number} businessId - The ID of the business.
 * @returns {Promise<HttpResultListOrder>} A promise that resolves to the list of orders.
 */
export const getOrdersByBusinessId = (businessId: number): Promise<HttpResultListOrder> => {
  return request.get(`/orders/business/${businessId}`);
};

/**
 * @description Retrieves a list of orders, optionally filtered by user ID.
 * @param {number} [userId] - The ID of the user whose orders to retrieve.
 * @returns {Promise<HttpResultListOrder>}
 * @deprecated Use getMyOrdersCustomer, getMyOrdersMerchant, or getOrdersByBusinessId instead.
 * @see {@link openapi.json} - operationId: "listOrders"
 */
export const listOrders = (userId?: number): Promise<HttpResultListOrder> => {
  return request.get('/orders', { params: { userId } });
};

/**
 * @description Creates a new order.
 * @param {Order} data - The order data to be submitted.
 * @returns {Promise<HttpResultOrder>}
 * @see {@link openapi.json} - operationId: "addOrders"
 */
export const addOrder = (data: Order): Promise<HttpResultOrder> => {
  return request.post('/orders', data);
};

/**
 * @description Fetches a single order by its ID.
 * @param {number} id - The ID of the order to fetch.
 * @returns {Promise<HttpResult<Order>>}
 * @see {@link openapi.json} - operationId: "getOrderById"
 */
export const getOrderById = (id: number): Promise<HttpResult<Order>> => {
  return request.get(`/orders/${id}`);
};

/**
 * @description Updates the status of an order.
 * @param {Partial<Order>} data - The order data with the updated status.
 * @returns {Promise<HttpResultOrder>}
 * @see {@link openapi.json} - operationId: "updateOrderStatus"
 */
export const updateOrderStatus = (data: Partial<Order>): Promise<HttpResultOrder> => {
  return request.patch('/orders', data);
};

/**
 * @description Adds a review for an order.
 * @param {Partial<Review>} data - The review data.
 * @returns {Promise<HttpResult<Review>>}
 */
export const addReview = (data: Partial<Review>): Promise<HttpResult<Review>> => {
  return request.post('/reviews', data);
};
