import request from '../utils/request';
import type { Review, HttpResult, HttpResultEmpty } from './types';

/**
 * @description Adds a review for a specific order.
 * @param {number} orderId - The ID of the order being reviewed.
 * @param {Partial<Review>} data - The review content.
 * @returns {Promise<HttpResult<Review>>}
 * @see {@link openapi.json} - operationId: "addReview"
 */
export const addReview = (orderId: number, data: Partial<Review>): Promise<HttpResult<Review>> => {
  return request.post(`/reviews/order/${orderId}`, data);
};

/**
 * @description Retrieves the review for a specific order.
 * @param {number} orderId - The ID of the order.
 * @returns {Promise<HttpResult<Review>>}
 * @see {@link openapi.json} - operationId: "getOrderReview"
 */
export const getOrderReview = (orderId: number): Promise<HttpResult<Review>> => {
  return request.get(`/reviews/order/${orderId}`);
};

/**
 * @description Updates an existing review.
 * @param {number} reviewId - The ID of the review to update.
 * @param {Partial<Review>} data - The updated review data.
 * @returns {Promise<HttpResult<Review>>}
 * @see {@link openapi.json} - operationId: "updateReview"
 */
export const updateReview = (reviewId: number, data: Partial<Review>): Promise<HttpResult<Review>> => {
  return request.patch(`/reviews/${reviewId}`, data);
};

/**
 * @description Deletes a review.
 * @param {number} reviewId - The ID of the review to delete.
 * @returns {Promise<HttpResultEmpty>}
 * @see {@link openapi.json} - operationId: "deleteReview"
 */
export const deleteReview = (reviewId: number): Promise<HttpResultEmpty> => {
  return request.delete(`/reviews/${reviewId}`);
};

/**
 * @description Retrieves all reviews written by the current user.
 * @returns {Promise<HttpResult<Review[]>>}
 * @see {@link openapi.json} - operationId: "getMyReviews"
 */
export const getMyReviews = (): Promise<HttpResult<Review[]>> => {
  return request.get('/reviews/my');
};

/**
 * @description Retrieves all reviews for a specific business.
 * @param {number} businessId - The ID of the business.
 * @returns {Promise<HttpResult<Review[]>>}
 * @see {@link openapi.json} - operationId: "getBusinessReviews"
 */
export const getBusinessReviews = (businessId: number): Promise<HttpResult<Review[]>> => {
  return request.get(`/reviews/business/${businessId}`);
};