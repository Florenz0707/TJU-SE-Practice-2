import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

// Import layouts and views
import Login from '../views/Login.vue';
import Register from '../views/Register.vue';
import CustomerLayout from '../views/customer/CustomerLayout.vue';
import MerchantLayout from '../views/merchant/MerchantLayout.vue';
import AdminLayout from '../views/admin/AdminLayout.vue';
import Forbidden from '../views/error/Forbidden.vue';
import NotFound from '../views/error/NotFound.vue';

// Lazy-loaded components
const Home = () => import('../views/customer/Home.vue');
const RestaurantDetail = () => import('../views/customer/RestaurantDetail.vue');

// Merchant views
const MerchantDashboard = () => import('../views/merchant/Dashboard.vue');
const MenuManagement = () => import('../views/merchant/MenuManagement.vue');
const BusinessProfile = () => import('../views/merchant/BusinessProfile.vue');
const OrderHistory = () => import('../views/merchant/OrderHistory.vue');

const AdminDashboard = () => import('../views/admin/Dashboard.vue');

// Define routes with types
const routes: Array<RouteRecordRaw> = [
  // Public routes
  { path: '/login', name: 'Login', component: Login, meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: Register, meta: { title: '注册' } },
  { path: '/forbidden', name: 'Forbidden', component: Forbidden, meta: { title: '无权访问' } },

  // Customer routes
  {
    path: '/',
    component: CustomerLayout,
    children: [
      // Publicly accessible routes
      { path: '', name: 'Home', component: Home, meta: { title: '首页' } },
      { path: 'restaurant/:id', name: 'RestaurantDetail', component: RestaurantDetail, meta: { title: '餐厅详情' } },

      // Routes that require authentication
      {
        path: 'profile',
        component: () => import('../views/customer/Profile/ProfileLayout.vue'),
        meta: { requiresAuth: true, roles: ['CUSTOMER'] },
        redirect: { name: 'UserProfile' },
        children: [
          {
            path: 'details',
            name: 'UserProfile',
            component: () => import('../views/customer/Profile/UserProfile.vue'),
            meta: { title: '我的信息' },
          },
          {
            path: 'addresses',
            name: 'AddressManagement',
            component: () => import('../views/customer/Profile/AddressManagement.vue'),
            meta: { title: '地址管理' },
          },
          {
            path: 'orders',
            name: 'OrderHistory',
            component: () => import('../views/customer/Profile/OrderHistory.vue'),
            meta: { title: '我的订单' },
          },
          {
            path: 'orders/:id',
            name: 'OrderDetail',
            component: () => import('../views/customer/Profile/OrderDetail.vue'),
            meta: { title: '订单详情' },
          },
        ],
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('../views/customer/Checkout.vue'),
        meta: { requiresAuth: true, roles: ['CUSTOMER'], title: '结账' },
      },
    ],
  },

  // Merchant routes
  {
    path: '/merchant',
    component: MerchantLayout,
    redirect: '/merchant/dashboard',
    meta: {
      requiresAuth: true,
      roles: ['MERCHANT', 'ADMIN']
    },
    children: [
      { path: 'dashboard', name: 'MerchantDashboard', component: MerchantDashboard, meta: { title: '商家仪表盘' } },
      { path: 'menu', name: 'MenuManagement', component: MenuManagement, meta: { title: '菜单管理' } },
      { path: 'profile', name: 'BusinessProfile', component: BusinessProfile, meta: { title: '店铺信息' } },
      { path: 'orders', name: 'MerchantOrderHistory', component: OrderHistory, meta: { title: '历史订单' } }
    ]
  },

  // Admin routes
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: {
      requiresAuth: true,
      roles: ['ADMIN']
    },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: AdminDashboard, meta: { title: '平台总览' } },
      { path: 'users', name: 'UserManagement', component: () => import('../views/admin/UserManagement.vue'), meta: { title: '用户管理' } },
      { path: 'businesses', name: 'BusinessManagement', component: () => import('../views/admin/BusinessManagement.vue'), meta: { title: '店铺管理' } }
    ]
  },

  // Catch-all 404 route
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: { title: '页面未找到' }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
