import request from '../utils/request';
import type { LoginDto, JWTToken } from './types';

/**
 * @description Authenticates the user and returns a JWT token.
 * @param {LoginDto} data - { username, password, rememberMe? }
 * @returns {Promise<JWTToken>}
 * @see {@link openapi.json} - operationId: "authorize"
 */
export function login(data: LoginDto): Promise<JWTToken> {
  // Note: The openapi.json specifies the response is directly the JWTToken, not wrapped in HttpResult.
  return request.post('/auth', data);
}
