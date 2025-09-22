import request from '../utils/request'
import type {
  Cart,
  HttpResultCart,
  HttpResultListCart,
  HttpResultEmpty,
} from './types'

/**
 * @description Fetches the current user's shopping cart.
 * @returns {Promise<HttpResultListCart>}
 * @see {@link openapi.json} - operationId: "getCurrentUserCart"
 */
export const getCurrentUserCart = (): Promise<HttpResultListCart> => {
  return request.get('/carts')
}

/**
 * @description Adds an item to the shopping cart.
 * @param {Cart} data - The cart item to add.
 * @returns {Promise<HttpResultCart>}
 * @see {@link openapi.json} - operationId: "addCartItem"
 */
export const addCartItem = (data: Cart): Promise<HttpResultCart> => {
  return request.post('/carts', data)
}

/**
 * @description Updates an item's quantity in the shopping cart.
 * @param {number} id - The ID of the cart item to update.
 * @param {number} quantity - The new quantity.
 * @returns {Promise<HttpResultCart>}
 * @see {@link openapi.json} - operationId: "updateCartItem"
 */
export const updateCartItem = (id: number, quantity: number): Promise<HttpResultCart> => {
  return request.patch(`/carts/${id}`, { quantity });
};

/**
 * @description Deletes an item from the shopping cart.
 * @param {number} id - The ID of the cart item to delete.
 * @returns {Promise<HttpResultEmpty>}
 * @see {@link openapi.json} - operationId: "deleteCartItem"
 */
export const deleteCartItem = (id: number): Promise<HttpResultEmpty> => {
  return request.delete(`/carts/${id}`)
}
