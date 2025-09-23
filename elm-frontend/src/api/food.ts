import request from '../utils/request';
import type { Food, HttpResultFood, HttpResultListFood, HttpResultEmpty } from './types';

// The request body for creating a food is a specific DTO, not the full Food object.
// It requires the business to be an object with just an ID.
export interface FoodCreationDto {
  id?: number;
  foodName: string;
  foodPrice: number;
  foodExplain?: string;
  foodImg?: string;
  business: {
    id: number;
  };
  remarks?: string;
}

/**
 * @description Retrieves a list of food items, optionally filtered by business or order.
 * @param {object} [params] - Query parameters, e.g., { business: businessId, order: orderId }
 * @returns {Promise<HttpResultListFood>}
 * @see {@link openapi.json} - operationId: "getAllFoods"
 */
export const getAllFoods = (params?: { business?: number; order?: number }): Promise<HttpResultListFood> => {
  return request.get('/foods', { params });
};

/**
 * @description Adds a new food item to a business.
 * @param {FoodCreationDto} data - The food item data to add.
 * @returns {Promise<HttpResultFood>}
 * @see {@link openapi.json} - operationId: "addFood"
 */
export const addFood = (data: FoodCreationDto): Promise<HttpResultFood> => {
  return request.post('/foods', data);
};

/**
 * @description Retrieves a single food item by its ID.
 * @param {number} id - The ID of the food item.
 * @returns {Promise<HttpResultFood>}
 * @see {@link openapi.json} - operationId: "getFoodById"
 */
export const getFoodById = (id: number): Promise<HttpResultFood> => {
  return request.get(`/foods/${id}`);
};

/**
 * @description Updates an existing food item by its ID.
 * @param {number} id - The ID of the food item to update.
 * @param {Food} data - The full food object to update.
 * @returns {Promise<HttpResultFood>}
 * @see {@link openapi.json} - operationId: "updateFood"
 */
export const updateFood = (id: number, data: Food): Promise<HttpResultFood> => {
  return request.put(`/foods/${id}`, data);
};

/**
 * @description Deletes a food item by its ID.
 * @param {number} id - The ID of the food item to delete.
 * @returns {Promise<HttpResultEmpty>}
 * @see {@link openapi.json} - operationId: "deleteFood"
 */
export const deleteFood = (id: number): Promise<HttpResultEmpty> => {
  return request.delete(`/foods/${id}`);
};
