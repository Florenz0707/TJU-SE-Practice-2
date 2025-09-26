import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

// Import layouts and views
import Login from '../views/Login.vue';
import Register from '../views/Register.vue';
import CustomerLayout from '../views/customer/CustomerLayout.vue';
import MerchantLayout from '../views/merchant/MerchantLayout.vue';
import AdminLayout from '../views/admin/AdminLayout.vue';
import MobileLayout from '../layouts/MobileLayout.vue';
import MobileMerchantLayout from '../layouts/MobileMerchantLayout.vue';
import MobileAdminLayout from '../layouts/MobileAdminLayout.vue';
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
            path: 'user-profile',
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
          {
            path: 'review/:orderId',
            name: 'SubmitReview',
            component: () => import('../views/customer/SubmitReview.vue'),
            meta: { title: '评价订单' },
          },
          {
            path: 'apply-merchant',
            name: 'ApplyAsMerchant',
            component: () => import('../views/customer/Profile/ApplyAsMerchant.vue'),
            meta: { title: '成为商家' },
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
      roles: ['MERCHANT']
    },
    children: [
      { path: 'dashboard', name: 'MerchantDashboard', component: MerchantDashboard, meta: { title: '商家仪表盘' } },
      { path: 'menu', name: 'MenuManagement', component: MenuManagement, meta: { title: '菜单管理' } },
      { path: 'profile', name: 'BusinessProfile', component: BusinessProfile, meta: { title: '店铺信息' } },
      { path: 'orders', name: 'MerchantOrderHistory', component: OrderHistory, meta: { title: '历史订单' } },
      { path: 'applications', name: 'MyApplications', component: () => import('../views/merchant/MyApplications.vue'), meta: { title: '我的申请' } },
      { path: 'user-profile', name: 'MerchantUserProfile', component: () => import('../views/merchant/UserProfile.vue'), meta: { title: '我的资料' } }
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
      { path: 'businesses', name: 'MerchantManagement', component: () => import('../views/admin/MerchantManagement.vue'), meta: { title: '商家管理' } },
      { path: 'applications', name: 'ShopManagement', component: () => import('../views/admin/ShopManagement.vue'), meta: { title: '店铺管理' } },
      { path: 'user-profile', name: 'AdminUserProfile', component: () => import('../views/admin/UserProfile.vue'), meta: { title: '我的资料' } }
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

// Mobile customer routes
const mobileRoutes: RouteRecordRaw = {
  path: '/mobile',
  component: MobileLayout,
  children: [
    {
      path: 'home',
      name: 'MobileHome',
      component: () => import('../views/mobile/customer/MobileHome.vue'),
      meta: { title: '首页' }
    },
    {
      path: 'restaurant/:id',
      name: 'MobileRestaurantDetail',
      component: () => import('../views/mobile/customer/MobileRestaurantDetail.vue'),
      meta: { title: '餐厅详情' }
    },
    {
      path: 'orders',
      name: 'MobileOrders',
      component: () => import('../views/mobile/customer/Orders.vue'),
      meta: { title: '我的订单', requiresAuth: true, roles: ['CUSTOMER'] }
    },
    {
      path: 'profile',
      name: 'MobileUserProfile',
      component: () => import('../views/mobile/customer/Profile/UserProfile.vue'),
      meta: { title: '我的', requiresAuth: true, roles: ['CUSTOMER'] },
    },
    {
      path: 'profile/addresses',
      name: 'MobileAddressManagement',
      component: () => import('../views/mobile/customer/Profile/AddressManagement.vue'),
      meta: { title: '地址管理', requiresAuth: true, roles: ['CUSTOMER'] },
    },
    {
      path: 'profile/apply-merchant',
      name: 'MobileApplyAsMerchant',
      component: () => import('../views/mobile/customer/Profile/ApplyAsMerchant.vue'),
      meta: { title: '成为商家', requiresAuth: true, roles: ['CUSTOMER'] },
    },
    {
      path: 'cart',
      name: 'MobileCart',
      component: () => import('../views/mobile/customer/Cart.vue'),
      meta: { title: '购物车', requiresAuth: true, roles: ['CUSTOMER'] }
    },
    {
      path: 'checkout',
      name: 'MobileCheckout',
      component: () => import('../views/mobile/customer/Checkout.vue'),
      meta: { title: '确认订单', requiresAuth: true, roles: ['CUSTOMER'] }
    },
    {
      path: 'orders/:id',
      name: 'MobileOrderDetail',
      component: () => import('../views/mobile/customer/OrderDetail.vue'),
      meta: { title: '订单详情', requiresAuth: true, roles: ['CUSTOMER'] }
    },
    {
      path: 'review/:orderId',
      name: 'MobileSubmitReview',
      component: () => import('../views/mobile/customer/SubmitReview.vue'),
      meta: { title: '评价订单', requiresAuth: true, roles: ['CUSTOMER'] }
    }
  ]
};

// Mobile merchant routes
const mobileMerchantRoutes: RouteRecordRaw = {
    path: '/mobile/merchant',
    component: MobileMerchantLayout,
    redirect: '/mobile/merchant/dashboard',
    meta: {
      requiresAuth: true,
      roles: ['MERCHANT']
    },
    children: [
      { path: 'dashboard', name: 'MobileMerchantDashboard', component: () => import('../views/mobile/merchant/MerchantDashboard.vue'), meta: { title: '商家仪表盘' } },
      { path: 'menu', name: 'MobileMenuManagement', component: () => import('../views/mobile/merchant/MerchantMenuManagement.vue'), meta: { title: '菜单管理' } },
      { path: 'profile', name: 'MobileBusinessProfile', component: () => import('../views/mobile/merchant/BusinessInfoManagement.vue'), meta: { title: '商家信息管理' } },
      { path: 'orders', name: 'MobileMerchantOrderHistory', component: () => import('../views/mobile/merchant/MerchantOrderHistory.vue'), meta: { title: '历史订单' } },
      { path: 'applications', name: 'MobileMyApplications', component: () => import('../views/mobile/merchant/MyApplications.vue'), meta: { title: '我的申请' } },
      { path: 'user-profile', name: 'MobileMerchantUserProfile', component: () => import('../views/mobile/merchant/UserProfile.vue'), meta: { title: '我的资料' } }
    ]
};

// Mobile admin routes
const mobileAdminRoutes: RouteRecordRaw = {
    path: '/mobile/admin',
    component: MobileAdminLayout,
    redirect: '/mobile/admin/dashboard',
    meta: {
      requiresAuth: true,
      roles: ['ADMIN']
    },
    children: [
      { path: 'dashboard', name: 'MobileAdminDashboard', component: () => import('../views/mobile/admin/Dashboard.vue'), meta: { title: '管理仪表盘' } },
      { path: 'users', name: 'MobileUserManagement', component: () => import('../views/mobile/admin/UserManagement.vue'), meta: { title: '用户管理' } },
      { path: 'businesses', name: 'MobileMerchantManagement', component: () => import('../views/mobile/admin/MerchantManagement.vue'), meta: { title: '商家管理' } },
      { path: 'shops', name: 'MobileShopManagement', component: () => import('../views/mobile/admin/ShopManagement.vue'), meta: { title: '店铺管理' } },
      { path: 'user-profile', name: 'MobileAdminProfile', component: () => import('../views/mobile/admin/UserProfile.vue'), meta: { title: '我的资料' } }
    ]
};


routes.push(mobileRoutes, mobileMerchantRoutes, mobileAdminRoutes);

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    } else {
      return { top: 0 };
    }
  },
});

import { useAuthStore } from '../store/auth';
import { isMobile } from '../utils/device';

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore();
  const isAuthenticated = authStore.user !== null;
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

  // Redirect root to mobile home on mobile devices
  if (to.path === '/' && isMobile()) {
    return next('/mobile/home');
  }

  // Redirect merchant dashboard to mobile version on mobile devices
  if (to.path === '/merchant/dashboard' && isMobile()) {
    return next('/mobile/merchant/dashboard');
  }

  // Redirect admin dashboard to mobile version on mobile devices
  if (to.path === '/admin/dashboard' && isMobile()) {
    return next('/mobile/admin/dashboard');
  }

  if (requiresAuth && !isAuthenticated) {
    // Redirect to login page, preserving the intended destination
    return next({
      path: '/login',
      query: { redirect: to.fullPath },
    });
  }

  // Check for role-based authorization
  const requiredRoles = to.matched.flatMap(record => (record.meta.roles as string[]) || []);
  if (requiredRoles.length > 0) {
    const userRoles = authStore.userRoles;
    const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role));
    if (!isAuthenticated || !hasRequiredRole) {
      return next('/forbidden');
    }
  }

  next();
});

export default router;
