import request from '../utils/request';
import type { Person, HttpResultPerson } from './types';

/**
 * @description Retrieves a person by their ID.
 * @param {number} id - The ID of the person to retrieve.
 * @returns {Promise<HttpResultPerson>}
 */
export const getPersonById = (id: number): Promise<HttpResultPerson> => {
  return request.get(`/persons/${id}`);
};

/**
 * @description Updates a person's information.
 * @param {Partial<Person>} data - The person data to update.
 * @returns {Promise<HttpResultPerson>}
 */
export const updatePerson = (data: Partial<Person>): Promise<HttpResultPerson> => {
  return request.put(`/persons/${data.id}`, data);
};