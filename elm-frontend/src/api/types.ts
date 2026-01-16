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
  password?: string;
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
  refresh_token: string;
}

export interface Business {
  id?: number;
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
  orderDetails?: OrderDetail[];
  usedVoucher?: PrivateVoucher | { id: number };
  voucherDiscount?: number;
  pointsUsed?: number;
  pointsDiscount?: number;
  walletPaid?: number;
  paymentMethod?: 'external' | 'wallet' | 'mixed';
}

export interface OrderDetail {
  id?: number;
  order?: Order;
  food?: Food;
  quantity?: number;
}

export interface Review {
  id: number;
  createTime?: string;
  updateTime?: string;
  creator?: number;
  updater?: number;
  deleted?: boolean;
  customer?: User;
  business?: Business;
  order?: Order;
  anonymous?: boolean;
  stars: number;
  content: string;
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

export const OrderStatus = {
  CANCELED: 0,
  PAID: 1,
  ACCEPTED: 2,
  DELIVERY: 3,
  COMPLETE: 4,
  COMMENTED: 5,
} as const;

export type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

export function getOrderStatusInfo(status: OrderStatus) {
  switch (status) {
    case OrderStatus.CANCELED:
      return { text: '已取消', type: 'info' as const };
    case OrderStatus.PAID:
      return { text: '已支付', type: 'primary' as const };
    case OrderStatus.ACCEPTED:
      return { text: '已接单', type: 'warning' as const };
    case OrderStatus.DELIVERY:
      return { text: '配送中', type: 'warning' as const };
    case OrderStatus.COMPLETE:
      return { text: '已完成', type: 'success' as const };
    case OrderStatus.COMMENTED:
      return { text: '已评价', type: 'success' as const };
    default:
      return { text: '未知状态', type: 'info' as const };
  }
}

export interface BusinessApplication {
  id: number;
  createTime?: string;
  updateTime?: string;
  creator?: number;
  updater?: number;
  deleted?: boolean;
  business: Business;
  handler?: User;
  applicationExplain?: string;
  applicationState?: number;
}

export interface MerchantApplication {
  id: number;
  createTime?: string;
  updateTime?: string;
  creator?: number;
  updater?: number;
  deleted?: boolean;
  applicant: User;
  applicationExplain?: string;
  applicationState?: number;
  handler?: User;
}

export interface Wallet {
  id: number;
  owner: number;
  balance: number;
  voucher: number;
  creditLimit?: number;
  lastWithdrawalAt?: string;
}

export interface Transaction {
  id?: number;
  amount: number;
  type: number;
  inWalletId?: number;
  outWalletId?: number;
  finished: boolean;
  createTime?: string;
}

export interface PublicVoucher {
  id: number;
  threshold: number;
  value: number;
  claimable: boolean;
  validDays: number;
  totalQuantity?: number;
  perUserLimit?: number;
}

export interface PrivateVoucher {
    id: number;
    walletId: number;
    publicVoucherId: number;
    value: number;
    threshold: number;
    expiryDate: string;
    used: boolean;
    publicVoucher: PublicVoucher;
}

// Points System Types

export interface PointsAccount {
    id: number;
    userId: number;
    totalPoints: number;
    frozenPoints: number;
    availablePoints: number;
}

export const PointsRecordType = {
    EARN: 'EARN',
    CONSUME: 'CONSUME',
    EXPIRE: 'EXPIRE',
    FREEZE: 'FREEZE',
    UNFREEZE: 'UNFREEZE'
} as const;
export type PointsRecordType = typeof PointsRecordType[keyof typeof PointsRecordType];

export interface PointsRecord {
    id: number;
    userId: number;
    type: PointsRecordType;
    points: number;
    bizId?: string;
    channelType?: string;
    description: string;
    recordTime: string;
}

export const PointsRuleType = {
    ORDER: 'ORDER',
    COMMENT: 'COMMENT',
    LOGIN: 'LOGIN',
    REGISTER: 'REGISTER'
} as const;
export type PointsRuleType = typeof PointsRuleType[keyof typeof PointsRuleType];

export interface PointsRule {
    id?: number;
    channelType: PointsRuleType;
    ratio: number;
    expireDays: number;
    description: string;
    isEnabled: boolean;
}
