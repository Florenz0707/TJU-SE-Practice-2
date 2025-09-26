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

export const useAuthStore = defineStore('auth', {
  // The state is now initialized as empty and is not persisted.
  state: (): AuthState => {
    return {
      token: null,
      refreshToken: null,
      user: null,
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
        this.setUser(userInfo.data);
      } catch (error) {
        console.error('Failed to fetch user info:', error);
        this.logout();
        throw new Error('Failed to fetch user info');
      }
    },

    /**
     * User logout. Clears session from memory.
     */
    logout(): void {
      this.token = null;
      this.refreshToken = null;
      this.user = null;
      setRequestToken(null);
    },

    /**
     * Set new tokens in memory
     * @param {JWTToken} tokens
     */
    setTokens(tokens: JWTToken) {
      this.token = tokens.id_token;
      this.refreshToken = tokens.refresh_token;
      setRequestToken(this.token); // Also update the token for axios
    },

    /**
     * Manually set user data in memory
     * @param {User} newUser
     */
    setUser(newUser: User) {
      this.user = newUser;
    },
  },
});
