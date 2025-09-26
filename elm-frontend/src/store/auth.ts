import { defineStore } from 'pinia';
import { login as apiLogin } from '../api/auth';
import { getActualUser } from '../api/user';
import { setRequestToken } from '../utils/request';
import type { User, LoginDto, JWTToken } from '../api/types';

// Define the state shape with types
interface AuthState {
  token: string | null;
  refreshToken: string | null;
  user: User | null;
}

const AUTH_TOKEN_KEY = 'authToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const USER_KEY = 'user';

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => {
    // Retrieve the user from localStorage and parse it
    const storedUser = localStorage.getItem(USER_KEY);
    return {
      token: localStorage.getItem(AUTH_TOKEN_KEY),
      refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY),
      user: storedUser ? JSON.parse(storedUser) : null,
    };
  },

  getters: {
    isLoggedIn: (state): boolean => !!state.token,
    userRoles: (state): string[] => {
      if (!state.user?.authorities) {
        return ['GUEST'];
      }
      const roleMap: { [key: string]: string } = {
        USER: 'CUSTOMER',
        BUSINESS: 'MERCHANT',
        ADMIN: 'ADMIN',
      };
      const roles = state.user.authorities
        .map(auth => roleMap[auth.name] || auth.name)
        .filter(Boolean);
      return roles.length > 0 ? roles : ['GUEST'];
    },
  },

  actions: {
    /**
     * User login
     * @param {LoginDto} credentials
     */
    async login(credentials: LoginDto): Promise<string[]> {
      try {
        const response = await apiLogin(credentials);
        this.setTokens(response);
        await this.fetchUserInfo();
        return this.userRoles;
      } catch (error) {
        console.error('Login failed:', error);
        this.logout();
        throw error; // Re-throw the error to be handled by the component
      }
    },

    /**
     * Fetch current user info
     */
    async fetchUserInfo(): Promise<void> {
      if (!this.token) return;
      try {
        const userInfo = await getActualUser();
        this.setUser(userInfo.data); // Use the setUser action to ensure data is saved
      } catch (error) {
        console.error('Failed to fetch user info:', error);
        this.logout();
        throw new Error('Failed to fetch user info');
      }
    },

    /**
     * User logout. Clears session locally.
     * NOTE: openapi.json does not specify a backend logout endpoint.
     */
    logout(): void {
      this.token = null;
      this.refreshToken = null;
      this.user = null;
      localStorage.removeItem(AUTH_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(USER_KEY); // Clear the user from storage
      setRequestToken(null);
    },

    /**
     * Set new tokens
     * @param {JWTToken} tokens
     */
    setTokens(tokens: JWTToken) {
      this.token = tokens.id_token;
      this.refreshToken = tokens.refresh_token;
      localStorage.setItem(AUTH_TOKEN_KEY, this.token);
      localStorage.setItem(REFRESH_TOKEN_KEY, this.refreshToken);
      setRequestToken(this.token); // Also update the token for axios
    },

    /**
     * Manually set user data
     * @param {User} newUser
     */
    setUser(newUser: User) {
      this.user = newUser;
      // Also save the user to localStorage
      localStorage.setItem(USER_KEY, JSON.stringify(newUser));
    },
  },
});
