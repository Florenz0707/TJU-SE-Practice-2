/**
 * This file contains TypeScript interfaces that are generated based on the openapi.json specification.
 * It is crucial for maintaining type safety between the frontend and the backend.
 * All interfaces are derived from the 'components.schemas' section of the OpenAPI document.
 */

// Generic API Response Wrapper
export interface HttpResult<T = any> {
  success: boolean;
  code: string;
  data: T;
  message?: string;
}

// Schemas from openapi.json

export interface Authority {
  name: string;
}

export interface User {
  id?: number;
  createTime?: string;
  updateTime?: string;
  creator?: number;
  updater?: number;
  deleted?: boolean;
  username: string;
  authorities?: Authority[];
}

export interface Person extends User {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  gender?: string;
  photo?: string;
}

export interface LoginDto {
  username: string;
  password?: string;
  rememberMe?: boolean;
}

export interface JWTToken {
  id_token: string;
}

export interface Business {
  id: number;
  createTime?: string;
  updateTime?: string;
  creator?: number;
  updater?: number;
  deleted?: boolean;
  businessName: string;
  businessOwner?: User;
  businessAddress?: string;
  businessExplain?: string;
  businessImg?: string;
  orderTypeId?: number;
  startPrice?: number;
  deliveryPrice?: number;
  remarks?: string;
}

export interface Food {
  id: number;
  createTime?: string;
  updateTime?: string;
  creator?: number;
  updater?: number;
  deleted?: boolean;
  foodName: string;
  foodExplain?: string;
  foodImg?: string;
  foodPrice: number;
  business: Business;
  remarks?: string;
}

export interface DeliveryAddress {
  id?: number;
  createTime?: string;
  updateTime?: string;
  creator?: number;
  updater?: number;
  deleted?: boolean;
  contactName: string;
  contactSex: number;
  contactTel: string;
  address: string;
  customer?: User;
}

export interface Order {
  id?: number;
  createTime?: string;
  updateTime?: string;
  creator?: number;
  updater?: number;
  deleted?: boolean;
  customer?: User;
  business?: Business;
  orderDate?: string;
  orderTotal?: number;
  deliveryAddress?: DeliveryAddress;
  orderState?: number;
}

export interface Cart {
  id?: number;
  createTime?: string;
  updateTime?: string;
  creator?: number;
  updater?: number;
  deleted?: boolean;
  food?: Food;
  customer?: User;
  business?: Business;
  quantity?: number;
}

// Specific HttpResult wrappers for different data types

export type HttpResultEmpty = HttpResult<object>;
export type HttpResultString = HttpResult<string>;

export type HttpResultBusiness = HttpResult<Business>;
export type HttpResultListBusiness = HttpResult<Business[]>;

export type HttpResultUser = HttpResult<User>;
export type HttpResultListUser = HttpResult<User[]>;

export type HttpResultPerson = HttpResult<Person>;

export type HttpResultOrder = HttpResult<Order>;
export type HttpResultListOrder = HttpResult<Order[]>;

export type HttpResultFood = HttpResult<Food>;
export type HttpResultListFood = HttpResult<Food[]>;

export type HttpResultCart = HttpResult<Cart>;
export type HttpResultListCart = HttpResult<Cart[]>;

export type HttpResultDeliveryAddress = HttpResult<DeliveryAddress>;
export type HttpResultListDeliveryAddress = HttpResult<DeliveryAddress[]>;
