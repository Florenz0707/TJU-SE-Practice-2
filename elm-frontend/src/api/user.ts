import request from '../utils/request';
import type {
  Person,
  LoginDto,
  HttpResultUser,
  HttpResultListUser,
  HttpResultPerson,
  HttpResultEmpty,
  HttpResultString,
} from './types';

/**
 * @description Creates a new user account with a password.
 * @param {LoginDto} data - User credentials { username, password }.
 * @returns {Promise<HttpResultUser>}
 * @see {@link openapi.json} - operationId: "createUser"
 */
export function createUser(data: LoginDto): Promise<HttpResultUser> {
  return request.post('/persons', data);
}

/**
 * @description Creates a new user with full person details.
 * @param {Person} data - Person object with detailed information.
 * @returns {Promise<HttpResultPerson>}
 * @see {@link openapi.json} - operationId: "addPerson"
 */
export function addPerson(data: Person): Promise<HttpResultPerson> {
  return request.post('/persons', data);
}

/**
 * @description Changes the password for a user.
 * @param {LoginDto} data - { username, password }
 * @returns {Promise<HttpResultString>}
 * @see {@link openapi.json} - operationId: "updateUserPassword"
 */
export function updateUserPassword(data: LoginDto): Promise<HttpResultString> {
  return request.post('/password', data);
}

/**
 * @description Retrieves a list of all users. For admin purposes.
 * @returns {Promise<HttpResultListUser>}
 * @see {@link openapi.json} - operationId: "getAllUsers"
 */
export const getAllUsers = (): Promise<HttpResultListUser> => {
  return request.get('/users');
};

/**
 * @description Gets the currently logged-in user's information.
 * @returns {Promise<HttpResultUser>}
 * @see {@link openapi.json} - operationId: "getActualUser"
 */
export const getActualUser = (): Promise<HttpResultUser> => {
  return request.get('/user');
};

/**
 * @description Retrieves a single user by their ID.
 * @param {number} id - The ID of the user.
 * @returns {Promise<HttpResultUser>}
 * @see {@link openapi.json} - operationId: "getUserById"
 */
export const getUserById = (id: number): Promise<HttpResultUser> => {
  return request.get(`/users/${id}`);
};

/**
 * @description Updates a user's information.
 * @param {number} id - The ID of the user to update.
 * @param {Person} data - The user data to update.
 * @returns {Promise<HttpResultUser>}
 * @see {@link openapi.json} - operationId: "updateUser"
 */
export const updateUser = (id: number, data: Person): Promise<HttpResultUser> => {
  return request.put(`/users/${id}`, data);
};

/**
 * @description Deletes a user by their ID.
 * @param {number} id - The ID of the user to delete.
 * @returns {Promise<HttpResultEmpty>}
 * @see {@link openapi.json} - operationId: "deleteUser"
 */
export const deleteUser = (id: number): Promise<HttpResultEmpty> => {
  return request.delete(`/users/${id}`);
};
