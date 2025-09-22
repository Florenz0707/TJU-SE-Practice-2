import request from '../utils/request';
import type { Order, HttpResultOrder, HttpResultListOrder, HttpResult } from './types';

/**
 * @description Retrieves a list of orders for a specific user.
 * @param {number} userId - The ID of the user whose orders to retrieve.
 * @returns {Promise<HttpResultListOrder>}
 * @see {@link openapi.json} - operationId: "listOrdersByUserId"
 */
export const listOrdersByUserId = (userId: number): Promise<HttpResultListOrder> => {
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
 * @description Fetches the current user's order history.
 * @returns {Promise<HttpResultListOrder>}
 * @see {@link openapi.json} - operationId: "getCurrentUserOrders"
 */
export const getCurrentUserOrders = (): Promise<HttpResultListOrder> => {
  return request.get('/orders/my');
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
 * @param {number} id - The ID of the order to update.
 * @param {number} orderState - The new state of the order.
 * @returns {Promise<HttpResultOrder>}
 * @see {@link openapi.json} - operationId: "updateOrderStatus"
 */
export const updateOrderStatus = (id: number, orderState: number): Promise<HttpResultOrder> => {
  return request.patch(`/orders/${id}`, { orderState });
};
